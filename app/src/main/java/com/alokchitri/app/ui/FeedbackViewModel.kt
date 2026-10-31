package com.alokchitri.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alokchitri.app.data.BillingRepository
import com.alokchitri.app.data.FeedbackApi
import com.alokchitri.app.data.FeedbackQuota
import com.alokchitri.app.data.PhotoPrep
import com.alokchitri.app.data.Prefs
import com.alokchitri.app.data.PreparedPhoto
import com.alokchitri.app.ui.screens.FeedbackUiState
import com.alokchitri.app.ui.screens.PhotoMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen 9: picked photo -> EXIF + downscale -> server -> feedback. A feedback is spent only when the server
 * answered, so failed calls cost nothing (spec). Language and context are passed per request.
 * Also owns Google Play Billing, because both purchases change the feedback quota this class shows.
 */
class FeedbackViewModel(app: Application) : AndroidViewModel(app) {
    val quota = FeedbackQuota(app)
    val billing = BillingRepository(app, Prefs(app), quota, viewModelScope, onEntitlementsChanged = { refreshQuota() })

    private val _state = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Empty)
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    private val _left = MutableStateFlow(quota.available)
    /** Feedbacks the user can still ask for (today's allowance + extras + top-up credits). */
    val left: StateFlow<Int> = _left.asStateFlow()

    private val _leftToday = MutableStateFlow(quota.leftToday)
    /** Today's allowance only, for Home's "N / 3 left" card. */
    val leftToday: StateFlow<Int> = _leftToday.asStateFlow()

    private var photo: PreparedPhoto? = null
    private var job: Job? = null

    init {
        billing.connect()
    }

    fun onPhotoPicked(uri: Uri, language: String, phone: String?) {
        job?.cancel()
        job = viewModelScope.launch {
            val prepared = withContext(Dispatchers.Default) { runCatching { PhotoPrep.prepare(getApplication(), uri) }.getOrNull() }
            if (prepared == null) {
                photo = null
                _state.value = FeedbackUiState.Error(PhotoMeta(emptyList(), uri.toString()), unreadable = true)
                return@launch
            }
            photo = prepared
            ask(language, phone)
        }
    }

    /** Retry after an error, or ask again once quota came back (ad watched, top-up bought). */
    fun retry(language: String, phone: String?) {
        if (photo == null) return
        job?.cancel()
        job = viewModelScope.launch { ask(language, phone) }
    }

    /**
     * A rewarded ad was watched: one more feedback for today. If the user was stuck on "out of quota" with a
     * photo, ask for its feedback right away.
     */
    fun onAdRewarded(language: String, phone: String?) {
        quota.addBonus()
        refreshQuota()
        if (_state.value is FeedbackUiState.OutOfQuota) retry(language, phone)
    }

    /** Called after a rewarded ad or a purchase changed the quota. */
    fun refreshQuota() {
        _left.value = quota.available
        _leftToday.value = quota.leftToday
    }

    private suspend fun ask(language: String, phone: String?) {
        val prepared = photo ?: return
        val meta = PhotoMeta(prepared.exif.chips, prepared.uri.toString())
        if (quota.available <= 0) {
            _state.value = FeedbackUiState.OutOfQuota(meta)
            return
        }
        _state.value = FeedbackUiState.Loading(meta)
        val result = runCatching { FeedbackApi.request(prepared, language, phone) }
        _state.value = result.fold(
            onSuccess = {
                quota.consume()
                refreshQuota()
                FeedbackUiState.Ready(meta, it)
            },
            onFailure = { FeedbackUiState.Error(meta) },
        )
    }
}
