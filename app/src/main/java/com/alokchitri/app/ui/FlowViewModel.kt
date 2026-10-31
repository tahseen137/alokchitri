package com.alokchitri.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alokchitri.app.camera.DeviceProbe
import com.alokchitri.app.data.Prefs
import com.alokchitri.app.data.SampleData
import com.alokchitri.app.ui.screens.PhoneBrand
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.LightMeter
import com.alokchitri.engine.LightReading
import com.alokchitri.engine.RecommendationEngine
import com.alokchitri.engine.RecommendationResult
import com.alokchitri.engine.ShotGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class FlowState(
    /** Null until the probe has run; the Setup screen shows a placeholder meanwhile. */
    val phoneName: String? = null,
    /** Brand for the How-to tabs, guessed from the phone name; null for brands without a guide. */
    val brand: PhoneBrand? = null,
    /** Sample limits until the Camera2 probe answers, and if the phone reports no back camera at all. */
    val device: DeviceLimits = SampleData.device,
    val goal: ShotGoal = ShotGoal.INDOOR_LOW_LIGHT,
    /** Live brightness. 5.0 = dim room, so the screens have something sensible before the camera is wired. */
    val ev100: Double = 5.0,
    val colorTempK: Int? = null,
    val checked: Set<Int> = emptySet(),
) {
    val result: RecommendationResult
        get() = RecommendationEngine.recommend(goal, LightReading(ev100, colorTempK), device)
}

/**
 * Holds the "new shot" flow: goal -> light reading -> recommendation -> floating card ticks.
 * [onLightReading] is fed by camera/Camera2PreviewHost while the Meter screen is open.
 */
class FlowViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val _state = MutableStateFlow(FlowState())
    val state: StateFlow<FlowState> = _state.asStateFlow()

    val setupDone: Boolean get() = prefs.setupDone

    init {
        viewModelScope.launch {
            val probed = withContext(Dispatchers.Default) { runCatching { DeviceProbe(app).probe() }.getOrNull() }
            val name = prefs.phoneNameOverride ?: probed?.phoneName ?: DeviceProbe.buildName()
            _state.update { it.copy(phoneName = name, brand = PhoneBrand.guess(name), device = probed?.limits ?: it.device) }
        }
    }

    fun confirmSetup() {
        prefs.setupDone = true
    }

    /** The user corrected the phone name. Camera2 limits stay as probed: they describe the real hardware. */
    fun setPhoneName(name: String) {
        prefs.phoneNameOverride = name
        _state.update { it.copy(phoneName = name, brand = PhoneBrand.guess(name)) }
    }

    private val smoother = LightMeter.Smoother()

    fun selectGoal(goal: ShotGoal) = _state.update { it.copy(goal = goal, checked = emptySet()) }

    fun onLightReading(rawEv100: Double, colorTempK: Int? = null) {
        val smoothed = smoother.add(rawEv100)
        // Frames arrive ~30 times a second; a tenth of a stop is below anything the engine or gauge resolves.
        _state.update { if (abs(it.ev100 - smoothed) < 0.1 && it.colorTempK == colorTempK) it else it.copy(ev100 = smoothed, colorTempK = colorTempK) }
    }

    fun toggleChecked(index: Int) = _state.update {
        it.copy(checked = if (index in it.checked) it.checked - index else it.checked + index)
    }
}
