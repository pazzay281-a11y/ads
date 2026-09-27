package com.example.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.data.BubbleClickAction
import com.example.data.BubbleColor
import com.example.data.ResetLogEntry
import com.example.service.FloatingAdResetService
import com.example.util.AdIdResult
import com.example.util.AdResetHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences.getInstance(application)

    val isFloatingActive: StateFlow<Boolean> = FloatingAdResetService.isServiceRunning

    private val _hasOverlayPermission = MutableStateFlow(false)
    val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(true)
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _adIdResult = MutableStateFlow<AdIdResult?>(null)
    val adIdResult: StateFlow<AdIdResult?> = _adIdResult.asStateFlow()

    private val _isLoadingAdId = MutableStateFlow(false)
    val isLoadingAdId: StateFlow<Boolean> = _isLoadingAdId.asStateFlow()

    val bubbleSizeDp: StateFlow<Int> = prefs.bubbleSizeDp
    val bubbleAlpha: StateFlow<Float> = prefs.bubbleAlpha
    val clickAction: StateFlow<BubbleClickAction> = prefs.clickAction
    val snapEdges: StateFlow<Boolean> = prefs.snapEdges
    val vibrateEnabled: StateFlow<Boolean> = prefs.vibrateEnabled
    val bubbleColor: StateFlow<BubbleColor> = prefs.bubbleColor
    val language: StateFlow<String> = prefs.language
    val resetCount: StateFlow<Int> = prefs.resetCount
    val history: StateFlow<List<ResetLogEntry>> = prefs.history

    init {
        refreshPermissions()
        refreshAdId()
    }

    fun refreshPermissions() {
        val app = getApplication<Application>()
        _hasOverlayPermission.value = AdResetHelper.hasOverlayPermission(app)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            _hasNotificationPermission.value = ContextCompat.checkSelfPermission(
                app,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            _hasNotificationPermission.value = true
        }
    }

    fun refreshAdId() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _isLoadingAdId.value = true
            val result = AdResetHelper.getAdvertisingId(app)
            _adIdResult.value = result
            _isLoadingAdId.value = false
            if (result.success && result.id.isNotEmpty()) {
                prefs.recordIdRefresh(result.id)
            }
        }
    }

    fun toggleFloatingBubble(context: Context) {
        refreshPermissions()
        if (!_hasOverlayPermission.value) {
            AdResetHelper.requestOverlayPermission(context)
            return
        }

        if (isFloatingActive.value) {
            FloatingAdResetService.stopService(context)
        } else {
            FloatingAdResetService.startService(context)
        }
    }

    fun openGoogleAdsDirectly(context: Context) {
        val currentId = _adIdResult.value?.id ?: ""
        prefs.recordResetAction(note = "Direct in-app reset launched", adId = currentId)
        AdResetHelper.openGoogleAdsSettings(context)
    }

    fun openPrivacySandbox(context: Context) {
        AdResetHelper.openPrivacySandboxSettings(context)
    }

    fun openPlayServices(context: Context) {
        AdResetHelper.openPlayServicesDetails(context)
    }

    fun setBubbleSize(size: Int, context: Context) {
        prefs.setBubbleSize(size)
        if (isFloatingActive.value) {
            FloatingAdResetService.updateConfig(context)
        }
    }

    fun setBubbleAlpha(alpha: Float, context: Context) {
        prefs.setBubbleAlpha(alpha)
        if (isFloatingActive.value) {
            FloatingAdResetService.updateConfig(context)
        }
    }

    fun setClickAction(action: BubbleClickAction) {
        prefs.setClickAction(action)
    }

    fun setSnapEdges(snap: Boolean) {
        prefs.setSnapEdges(snap)
    }

    fun setVibrateEnabled(enabled: Boolean) {
        prefs.setVibrateEnabled(enabled)
    }

    fun setBubbleColor(color: BubbleColor, context: Context) {
        prefs.setBubbleColor(color)
        if (isFloatingActive.value) {
            FloatingAdResetService.updateConfig(context)
        }
    }

    fun setLanguage(lang: String) {
        prefs.setLanguage(lang)
    }

    fun clearHistory() {
        prefs.clearHistory()
    }
}
