package com.example.instagramwrapper

import android.net.Uri
import java.util.Locale

object InstagramUrlFilter {
    const val defaultHomeUrl: String = "https://www.instagram.com/"

    fun isInstagramUrl(url: String): Boolean {
        val uri = parseUri(url) ?: return false
        if (hasCredentials(uri)) return false

        val scheme = uri.scheme?.lowercase(Locale.US) ?: return false
        if (scheme != "https" && scheme != "http") return false

        return isInstagramHost(uri.host)
    }

    fun isBlockedInstagramUrl(url: String): Boolean {
        val uri = parseUri(url) ?: return false
        if (!isInstagramUrl(url)) return false

        val segments = normalizedPathSegments(uri)
        return segments.firstOrNull()?.equals("reels", ignoreCase = true) == true
    }

    fun normalizeAllowedInstagramUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val uri = parseUri(url) ?: return null
        if (!isInstagramUrl(url) || isBlockedInstagramUrl(url)) return null

        return if (uri.scheme.equals("http", ignoreCase = true)) {
            uri.buildUpon().scheme("https").build().toString()
        } else {
            uri.toString()
        }
    }

    fun shouldOpenInExternalBrowser(url: String): Boolean {
        val uri = parseUri(url) ?: return false
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return false
        return scheme == "http" || scheme == "https" || scheme == "intent"
    }

    private fun parseUri(url: String): Uri? {
        if (url.isBlank()) return null
        return runCatching { Uri.parse(url.trim()) }.getOrNull()
    }

    private fun hasCredentials(uri: Uri): Boolean {
        return uri.encodedAuthority?.contains('@') == true
    }

    private fun isInstagramHost(host: String?): Boolean {
        val normalizedHost = host?.lowercase(Locale.US) ?: return false
        return normalizedHost == "instagram.com" || normalizedHost.endsWith(".instagram.com")
    }

    private fun normalizedPathSegments(uri: Uri): List<String> {
        val encodedPath = uri.encodedPath ?: return emptyList()
        val decodedPath = runCatching { Uri.decode(encodedPath) }.getOrElse { encodedPath }
        return decodedPath
            .split('/')
            .filter { it.isNotBlank() }
            .map { it.trim().lowercase(Locale.US) }
    }
}
