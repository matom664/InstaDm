package com.example.instagramwrapper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun InstagramWebView(
    initialUrl: String,
    isOnline: Boolean,
    blockMode: BlockMode,
    onStateChanged: (WebViewState) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onBlockedNavigation: (String) -> Unit,
    onPersistAllowedUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScopeSafe()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    var webViewState by remember {
        mutableStateOf(
            WebViewState(
                currentUrl = initialUrl,
                isLoading = true,
                pageLoaded = false,
                loadError = false,
                isOffline = !isOnline,
                canGoBack = false,
            )
        )
    }

    val isOnlineState by rememberUpdatedState(isOnline)
    val blockModeState by rememberUpdatedState(blockMode)
    val onBlockedNavigationState by rememberUpdatedState(onBlockedNavigation)
    val onPersistAllowedUrlState by rememberUpdatedState(onPersistAllowedUrl)

    fun publishState(nextState: WebViewState) {
        webViewState = nextState
        onStateChanged(nextState)
    }

    fun updateNavigationState(view: WebView?, currentUrl: String? = view?.url) {
        publishState(
            webViewState.copy(
                currentUrl = currentUrl,
                canGoBack = view?.canGoBack() == true,
            )
        )
    }

    fun handleBlockedNavigation(view: WebView?, url: String) {
        onBlockedNavigationState(url)
        if (view?.canGoBack() == true) {
            view.goBack()
        }
    }

    fun shouldAllowNavigation(url: String, isMainFrame: Boolean): Boolean {
        if (!isMainFrame) return false
        if (InstagramUrlFilter.isBlockedInstagramUrl(url)) return false
        return InstagramUrlFilter.isInstagramUrl(url)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = webViewState.isLoading && !webViewState.pageLoaded,
        onRefresh = {
            webView?.reload()
        },
    )

    LaunchedEffect(isOnlineState, webView) {
        webView?.settings?.cacheMode = if (isOnlineState) {
            WebSettings.LOAD_DEFAULT
        } else {
            WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
        publishState(
            webViewState.copy(
                isOffline = !isOnlineState,
            )
        )
    }

    DisposableEffect(webView) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { factoryContext ->
                WebView(factoryContext).apply {
                    webView = this
                    onWebViewCreated(this)

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.databaseEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.cacheMode = if (isOnlineState) {
                        WebSettings.LOAD_DEFAULT
                    } else {
                        WebSettings.LOAD_CACHE_ELSE_NETWORK
                    }
                    settings.userAgentString = WebSettings.getDefaultUserAgent(factoryContext)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.setSupportMultipleWindows(false)

                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            publishState(
                                webViewState.copy(
                                    isLoading = newProgress in 0..99,
                                    canGoBack = view?.canGoBack() == true,
                                    currentUrl = view?.url ?: webViewState.currentUrl,
                                )
                            )
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            val isMainFrame = request.isForMainFrame
                            return handleNavigation(context, view, url, isMainFrame, blockModeState, onBlockedNavigationState)
                        }

                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            val safeUrl = url ?: return false
                            return handleNavigation(context, view, safeUrl, true, blockModeState, onBlockedNavigationState)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            publishState(
                                webViewState.copy(
                                    currentUrl = url ?: view?.url,
                                    isLoading = true,
                                    loadError = false,
                                    isOffline = !isOnlineState,
                                    canGoBack = view?.canGoBack() == true,
                                )
                            )
                            if (view != null) {
                                ReelsBlocker.inject(view, blockModeState.blocksReels)
                            }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            if (view != null) {
                                ReelsBlocker.inject(view, blockModeState.blocksReels)
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            val currentUrl = url ?: view?.url
                            val isBlockedByMode = currentUrl != null &&
                                blockModeState.blocksReels &&
                                InstagramUrlFilter.isBlockedInstagramUrl(currentUrl)
                            if (currentUrl != null && !isBlockedByMode && InstagramUrlFilter.isInstagramUrl(currentUrl)) {
                                coroutineScope.launch {
                                    onPersistAllowedUrlState(currentUrl)
                                }
                            }
                            if (view != null) {
                                ReelsBlocker.inject(view, blockModeState.blocksReels)
                            }
                            publishState(
                                webViewState.copy(
                                    currentUrl = currentUrl,
                                    isLoading = false,
                                    pageLoaded = true,
                                    loadError = false,
                                    isOffline = !isOnlineState,
                                    canGoBack = view?.canGoBack() == true,
                                )
                            )
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            val currentUrl = url ?: view?.url
                            if (currentUrl != null && blockModeState.blocksReels && InstagramUrlFilter.isBlockedInstagramUrl(currentUrl)) {
                                handleBlockedNavigation(view, currentUrl)
                                return
                            }
                            if (currentUrl != null && InstagramUrlFilter.isInstagramUrl(currentUrl)) {
                                coroutineScope.launch {
                                    onPersistAllowedUrlState(currentUrl)
                                }
                            }
                            if (view != null) {
                                ReelsBlocker.inject(view, blockModeState.blocksReels)
                            }
                            updateNavigationState(view, currentUrl)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == false) {
                                return
                            }
                            publishState(
                                webViewState.copy(
                                    isLoading = false,
                                    loadError = true,
                                    isOffline = !isOnlineState,
                                    canGoBack = view?.canGoBack() == true,
                                )
                            )
                        }

                        @Suppress("DEPRECATION")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?,
                        ) {
                            publishState(
                                webViewState.copy(
                                    isLoading = false,
                                    loadError = true,
                                    isOffline = !isOnlineState,
                                    canGoBack = view?.canGoBack() == true,
                                )
                            )
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?,
                        ) {
                            handler?.cancel()
                            publishState(
                                webViewState.copy(
                                    isLoading = false,
                                    loadError = true,
                                    isOffline = !isOnlineState,
                                )
                            )
                        }

                        override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                            publishState(
                                webViewState.copy(
                                    isLoading = false,
                                    loadError = true,
                                    pageLoaded = false,
                                    isOffline = !isOnlineState,
                                    canGoBack = view?.canGoBack() == true,
                                )
                            )
                            return true
                        }
                    }

                    loadUrl(initialUrl)
                    lastLoadedUrl = initialUrl
                }
            },
            update = { view ->
                webView = view
                if (view.settings.cacheMode != if (isOnlineState) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK) {
                    view.settings.cacheMode = if (isOnlineState) {
                        WebSettings.LOAD_DEFAULT
                    } else {
                        WebSettings.LOAD_CACHE_ELSE_NETWORK
                    }
                }
                view.settings.userAgentString = WebSettings.getDefaultUserAgent(view.context)
                updateNavigationState(view)
            },
        )

        LaunchedEffect(initialUrl, webView) {
            val currentWebView = webView ?: return@LaunchedEffect
            if (lastLoadedUrl != initialUrl) {
                currentWebView.loadUrl(initialUrl)
                lastLoadedUrl = initialUrl
            }
        }

        if (webViewState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        PullRefreshIndicator(
            refreshing = webViewState.isLoading && !webViewState.pageLoaded,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private fun handleNavigation(
    context: Context,
    view: WebView?,
    url: String,
    isMainFrame: Boolean,
    blockMode: BlockMode,
    onBlockedNavigation: (String) -> Unit,
): Boolean {
    if (!isMainFrame) {
        return false
    }

    if (blockMode.blocksReels && InstagramUrlFilter.isBlockedInstagramUrl(url)) {
        onBlockedNavigation(url)
        if (view?.canGoBack() == true) {
            view.goBack()
        }
        return true
    }

    if (InstagramUrlFilter.isInstagramUrl(url)) {
        return false
    }

    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return true
    val scheme = uri.scheme?.lowercase() ?: return true

    if (scheme == "javascript" || scheme == "file" || scheme == "content" || scheme == "data") {
        return true
    }

    if (scheme == "intent") {
        return openIntentUrl(context, url)
    }

    if (scheme == "http" || scheme == "https") {
        return openExternalBrowser(context, uri)
    }

    return true
}

private fun openExternalBrowser(context: Context, uri: Uri): Boolean {
    return runCatching {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
        true
    }.getOrDefault(true)
}

private fun openIntentUrl(context: Context, url: String): Boolean {
    return runCatching {
        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
        val packageManager = context.packageManager
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        } else {
            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
            if (!fallbackUrl.isNullOrBlank()) {
                context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUrl.toUri()))
            }
        }
        true
    }.getOrDefault(true)
}

@Composable
private fun rememberCoroutineScopeSafe(): CoroutineScope {
    return androidx.compose.runtime.rememberCoroutineScope()
}
