package com.alokchitri.app.ui

import androidx.lifecycle.ViewModel
import com.alokchitri.app.data.SampleData
import com.alokchitri.engine.DeviceLimits
import com.alokchitri.engine.LightMeter
import com.alokchitri.engine.LightReading
import com.alokchitri.engine.RecommendationEngine
import com.alokchitri.engine.RecommendationResult
import com.alokchitri.engine.ShotGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FlowState(
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
 * TODO: replace [onLightReading] callers with the Camera2 AE callback (see camera/AeMeterCallback.kt)
 * and [FlowState.device] with the result of camera/DeviceProbe.
 */
class FlowViewModel : ViewModel() {
    private val _state = MutableStateFlow(FlowState())
    val state: StateFlow<FlowState> = _state.asStateFlow()

    private val smoother = LightMeter.Smoother()

    fun selectGoal(goal: ShotGoal) = _state.update { it.copy(goal = goal, checked = emptySet()) }

    fun onLightReading(rawEv100: Double, colorTempK: Int? = null) {
        val smoothed = smoother.add(rawEv100)
        _state.update { it.copy(ev100 = smoothed, colorTempK = colorTempK) }
    }

    fun toggleChecked(index: Int) = _state.update {
        it.copy(checked = if (index in it.checked) it.checked - index else it.checked + index)
    }
}
