package com.example.instagramwrapper

data class WebViewState(
    val currentUrl: String? = null,
    val isLoading: Boolean = true,
    val pageLoaded: Boolean = false,
    val loadError: Boolean = false,
    val isOffline: Boolean = false,
    val canGoBack: Boolean = false,
)
