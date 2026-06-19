package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
        private val DISPLAY_NAME_KEY = stringPreferencesKey("display_name")
        private val AUTO_SHARE_LOCATION_KEY = booleanPreferencesKey("auto_share_location")
        private val PLAY_SOUND_ON_SOS_KEY = booleanPreferencesKey("play_sound_on_sos")
        private val VIBRATE_ON_INCOMING_KEY = booleanPreferencesKey("vibrate_on_incoming")
    }

    val deviceIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DEVICE_ID_KEY] ?: ""
    }

    val displayNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[DISPLAY_NAME_KEY] ?: ""
    }

    val autoShareLocationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SHARE_LOCATION_KEY] ?: true
    }

    val playSoundOnSosFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PLAY_SOUND_ON_SOS_KEY] ?: true
    }

    val vibrateOnIncomingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATE_ON_INCOMING_KEY] ?: true
    }

    suspend fun getDeviceId(): String {
        val prefs = context.dataStore.data.first()
        var currentId = prefs[DEVICE_ID_KEY]
        if (currentId.isNullOrEmpty()) {
            currentId = UUID.randomUUID().toString()
            setDeviceId(currentId)
        }
        return currentId
    }

    suspend fun setDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun setDisplayName(displayName: String) {
        context.dataStore.edit { preferences ->
            preferences[DISPLAY_NAME_KEY] = displayName
        }
    }

    suspend fun setAutoShareLocation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SHARE_LOCATION_KEY] = enabled
        }
    }

    suspend fun setPlaySoundOnSos(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PLAY_SOUND_ON_SOS_KEY] = enabled
        }
    }

    suspend fun setVibrateOnIncoming(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATE_ON_INCOMING_KEY] = enabled
        }
    }
}
