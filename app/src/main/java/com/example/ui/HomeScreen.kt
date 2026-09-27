package com.example.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AaidInspectorCard
import com.example.ui.components.BubbleSettingsCard
import com.example.ui.components.DirectActionCards
import com.example.ui.components.FloatingControlCard
import com.example.ui.components.GuideAndHistoryCard
import com.example.ui.components.HeaderBar
import com.example.util.AdResetHelper
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Observe lifecycle so when user returns from Settings, permission is refreshed automatically!
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.refreshAdId()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isFloatingActive by viewModel.isFloatingActive.collectAsStateWithLifecycle()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
    val adIdResult by viewModel.adIdResult.collectAsStateWithLifecycle()
    val isLoadingAdId by viewModel.isLoadingAdId.collectAsStateWithLifecycle()
    val bubbleSizeDp by viewModel.bubbleSizeDp.collectAsStateWithLifecycle()
    val bubbleAlpha by viewModel.bubbleAlpha.collectAsStateWithLifecycle()
    val clickAction by viewModel.clickAction.collectAsStateWithLifecycle()
    val snapEdges by viewModel.snapEdges.collectAsStateWithLifecycle()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsStateWithLifecycle()
    val bubbleColor by viewModel.bubbleColor.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val resetCount by viewModel.resetCount.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HeaderBar(
                language = language,
                onToggleLanguage = { viewModel.setLanguage(it) },
                onRefreshAdId = { viewModel.refreshAdId() },
                isLoading = isLoadingAdId
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Bubble Hero Toggle Card
                FloatingControlCard(
                    isFloatingActive = isFloatingActive,
                    hasOverlayPermission = hasOverlayPermission,
                    language = language,
                    onToggleFloating = {
                        viewModel.toggleFloatingBubble(context)
                        if (!hasOverlayPermission) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (language == "bn")
                                        "স্ক্রিনের উপর ফ্লোটিং বাটন দেখাতে 'Display over other apps' পারমিশন অন করুন।"
                                    else
                                        "Please allow 'Display over other apps' to show the floating bubble."
                                )
                            }
                        }
                    },
                    onRequestOverlayPermission = {
                        AdResetHelper.requestOverlayPermission(context)
                    }
                )

                // Direct 1-Tap Action Card
                DirectActionCards(
                    language = language,
                    resetCount = resetCount,
                    onOpenAdsDirectly = {
                        viewModel.openGoogleAdsDirectly(context)
                    },
                    onOpenPrivacySandbox = {
                        viewModel.openPrivacySandbox(context)
                    },
                    onOpenPlayServices = {
                        viewModel.openPlayServices(context)
                    }
                )

                // Advertising ID Inspector Card
                AaidInspectorCard(
                    adIdResult = adIdResult,
                    isLoading = isLoadingAdId,
                    language = language,
                    onRefresh = {
                        viewModel.refreshAdId()
                    }
                )

                // Bubble Appearance & Behavior Settings Card
                BubbleSettingsCard(
                    bubbleSizeDp = bubbleSizeDp,
                    bubbleAlpha = bubbleAlpha,
                    clickAction = clickAction,
                    snapEdges = snapEdges,
                    vibrateEnabled = vibrateEnabled,
                    bubbleColor = bubbleColor,
                    language = language,
                    onSizeChange = { viewModel.setBubbleSize(it, context) },
                    onAlphaChange = { viewModel.setBubbleAlpha(it, context) },
                    onClickActionChange = { viewModel.setClickAction(it) },
                    onSnapEdgesChange = { viewModel.setSnapEdges(it) },
                    onVibrateChange = { viewModel.setVibrateEnabled(it) },
                    onColorChange = { viewModel.setBubbleColor(it, context) }
                )

                // Guide & History Log Card
                GuideAndHistoryCard(
                    language = language,
                    resetCount = resetCount,
                    history = history,
                    onClearHistory = {
                        viewModel.clearHistory()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
