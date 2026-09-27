package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.AaidInspectorCard
import com.example.ui.components.BubbleCustomizationCard
import com.example.ui.components.DirectActionCards
import com.example.ui.components.FloatingControlCard
import com.example.ui.components.GuideAndHistoryCard
import com.example.ui.components.HeaderBar
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AdResetHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Refresh overlay permission & ad id status whenever user resumes back from Settings
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
    val language by viewModel.language.collectAsStateWithLifecycle()

    val bubbleSizeDp by viewModel.bubbleSizeDp.collectAsStateWithLifecycle()
    val bubbleAlpha by viewModel.bubbleAlpha.collectAsStateWithLifecycle()
    val clickAction by viewModel.clickAction.collectAsStateWithLifecycle()
    val snapEdges by viewModel.snapEdges.collectAsStateWithLifecycle()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsStateWithLifecycle()
    val bubbleColor by viewModel.bubbleColor.collectAsStateWithLifecycle()
    val resetCount by viewModel.resetCount.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("main_scrollable_container"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Floating Bubble Toggle & Overlay Permission Card
                item(key = "floating_control") {
                    FloatingControlCard(
                        isFloatingActive = isFloatingActive,
                        hasOverlayPermission = hasOverlayPermission,
                        language = language,
                        onToggleFloating = { viewModel.toggleFloatingBubble(context) },
                        onRequestOverlayPermission = { AdResetHelper.requestOverlayPermission(context) }
                    )
                }

                // 2. Direct Reset Action & Google Play Services Shortcuts
                item(key = "direct_actions") {
                    DirectActionCards(
                        language = language,
                        resetCount = resetCount,
                        onOpenAdsDirectly = { viewModel.openGoogleAdsDirectly(context) },
                        onOpenPrivacySandbox = { viewModel.openPrivacySandbox(context) },
                        onOpenPlayServices = { viewModel.openPlayServices(context) }
                    )
                }

                // 3. Current Advertising ID Inspector & Copy
                item(key = "aaid_inspector") {
                    AaidInspectorCard(
                        adIdResult = adIdResult,
                        isLoading = isLoadingAdId,
                        language = language,
                        onRefresh = { viewModel.refreshAdId() }
                    )
                }

                // 4. Floating Bubble Customization Settings
                item(key = "bubble_customization") {
                    BubbleCustomizationCard(
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
                        onBubbleColorChange = { viewModel.setBubbleColor(it, context) }
                    )
                }

                // 5. Reset Steps Guide & History Log
                item(key = "guide_and_history") {
                    GuideAndHistoryCard(
                        language = language,
                        resetCount = resetCount,
                        history = history,
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }
            }
        }
    }
}
