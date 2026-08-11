package com.example.instagramwrapper

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lastViewedDataStore by preferencesDataStore(name = "instagram_last_viewed")

data class LastViewedState(
    val url: String?,
)

class LastViewedRepository(context: Context) {
    private val dataStore = context.applicationContext.lastViewedDataStore

    private object Keys {
        val lastViewedUrl = stringPreferencesKey("last_viewed_url")
    }

    val lastViewedStateFlow: Flow<LastViewedState> = dataStore.data.map { preferences ->
        LastViewedState(url = preferences[Keys.lastViewedUrl])
    }

    suspend fun saveLastViewedUrl(url: String?) {
        val normalizedUrl = InstagramUrlFilter.normalizeAllowedInstagramUrl(url)
        if (normalizedUrl == null) return

        dataStore.edit { preferences ->
            preferences[Keys.lastViewedUrl] = normalizedUrl
        }
    }
}
