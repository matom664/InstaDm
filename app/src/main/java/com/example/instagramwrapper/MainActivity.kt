package com.example.instagramwrapper

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.instagramwrapper.ui.theme.InstagramWrapperTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            InstagramWrapperTheme {
                InstagramBrowserScreen()
            }
        }
    }
}

@Composable
private fun InstagramBrowserScreen() {
    val context = LocalContext.current
    val repository = remember { LastViewedRepository(context.applicationContext) }
    val connectivityMonitor = remember { ConnectivityMonitor(context.applicationContext) }
    val isOnline by connectivityMonitor.observeConnectivity().collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()

    val initialUrl by produceState(initialValue = InstagramUrlFilter.defaultHomeUrl, repository) {
        val lastViewed = runCatching { repository.lastViewedStateFlow.first() }.getOrNull()
        value = InstagramUrlFilter.normalizeAllowedInstagramUrl(lastViewed?.url) ?: InstagramUrlFilter.defaultHomeUrl
    }

    var webViewState by remember {
        mutableStateOf(
            WebViewState(
                currentUrl = null,
                isLoading = true,
                pageLoaded = false,
                loadError = false,
                isOffline = !isOnline,
                canGoBack = false,
            )
        )
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearSessionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        webView?.settings?.cacheMode = if (isOnline) {
            android.webkit.WebSettings.LOAD_DEFAULT
        } else {
            android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        webViewState = webViewState.copy(isOffline = !isOnline)
    }

    BackHandler(enabled = webViewState.canGoBack) {
        val currentWebView = webView
        if (currentWebView?.canGoBack() == true) {
            currentWebView.goBack()
        } else {
            (context as? ComponentActivity)?.finish()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            InstagramWebView(
                initialUrl = initialUrl,
                isOnline = isOnline,
                onStateChanged = { webViewState = it },
                onWebViewCreated = { createdWebView -> webView = createdWebView },
                onBlockedNavigation = {
                    showSettingsMenu = false
                },
                onPersistAllowedUrl = { url ->
                    scope.launch {
                        repository.saveLastViewedUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (!isOnline && webViewState.pageLoaded) {
                SmallStatusCard(
                    message = stringResource(R.string.status_offline),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                )
            }

            if (webViewState.loadError && !webViewState.pageLoaded) {
                OfflineScreen(
                    message = if (isOnline) {
                        stringResource(R.string.error_unable_to_load)
                    } else {
                        stringResource(R.string.error_offline)
                    },
                    onRetry = {
                        webView?.reload()
                    },
                )
            }

            if (webViewState.loadError && webViewState.pageLoaded) {
                SmallErrorRetryCard(
                    message = if (isOnline) {
                        stringResource(R.string.error_unable_to_load)
                    } else {
                        stringResource(R.string.error_offline)
                    },
                    onRetry = { webView?.reload() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                )
            }

            TextButton(
                onClick = { showSettingsMenu = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text(text = stringResource(R.string.content_description_settings))
            }

            DropdownMenu(
                expanded = showSettingsMenu,
                onDismissRequest = { showSettingsMenu = false },
                properties = PopupProperties(focusable = true),
            ) {
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.settings_clear_cache)) },
                    onClick = {
                        showSettingsMenu = false
                        clearCacheDialog = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.settings_clear_session)) },
                    onClick = {
                        showSettingsMenu = false
                        clearSessionDialog = true
                    },
                )
            }

            if (clearCacheDialog) {
                AlertDialog(
                    onDismissRequest = { clearCacheDialog = false },
                    title = { Text(text = stringResource(R.string.dialog_clear_cache_title)) },
                    text = { Text(text = stringResource(R.string.dialog_clear_cache_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            clearCacheDialog = false
                            webView?.apply {
                                clearCache(true)
                                clearHistory()
                                clearFormData()
                            }
                            WebStorage.getInstance().deleteAllData()
                            webView?.reload()
                        }) {
                            Text(text = stringResource(R.string.action_clear))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { clearCacheDialog = false }) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    },
                )
            }

            if (clearSessionDialog) {
                AlertDialog(
                    onDismissRequest = { clearSessionDialog = false },
                    title = { Text(text = stringResource(R.string.dialog_clear_session_title)) },
                    text = { Text(text = stringResource(R.string.dialog_clear_session_body)) },
                    confirmButton = {
                        TextButton(onClick = {
                            clearSessionDialog = false
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.removeAllCookies { _ ->
                                cookieManager.flush()
                                WebStorage.getInstance().deleteAllData()
                                webView?.clearHistory()
                                webView?.loadUrl(InstagramUrlFilter.defaultHomeUrl)
                            }
                        }) {
                            Text(text = stringResource(R.string.action_clear_session))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { clearSessionDialog = false }) {
                            Text(text = stringResource(R.string.action_cancel))
                        }
                    },
                    properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
                )
            }
        }
    }
}

@Composable
private fun LoadingShell() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.loading_app),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            )
        }
    }
}

@Composable
private fun SmallStatusCard(message: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        modifier = modifier,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun SmallErrorRetryCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Text(text = stringResource(R.string.action_retry))
            }
        }
    }
}
