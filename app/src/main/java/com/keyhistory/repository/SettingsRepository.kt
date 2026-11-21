package com.keyhistory.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "keyhistory_settings")

data class AppSettings(
    /** Consentimento explícito: o registro só ocorre quando o usuário o ativa. */
    val recordingEnabled: Boolean = false,
    val recordingPaused: Boolean = false,
    val consentAccepted: Boolean = false,
    val autoSave: Boolean = true,
    val hapticFeedback: Boolean = true,
    val excludedPackages: Set<String> = emptySet(),
) {
    val isRecordingActive: Boolean get() = recordingEnabled && !recordingPaused
}

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    private object Keys {
        val ENABLED = booleanPreferencesKey("recording_enabled")
        val PAUSED = booleanPreferencesKey("recording_paused")
        val CONSENT = booleanPreferencesKey("consent_accepted")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")
        val EXCLUDED = stringSetPreferencesKey("excluded_packages")
    }

    val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            recordingEnabled = p[Keys.ENABLED] ?: false,
            recordingPaused = p[Keys.PAUSED] ?: false,
            consentAccepted = p[Keys.CONSENT] ?: false,
            autoSave = p[Keys.AUTO_SAVE] ?: true,
            hapticFeedback = p[Keys.HAPTIC] ?: true,
            excludedPackages = p[Keys.EXCLUDED] ?: emptySet(),
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setRecordingEnabled(enabled: Boolean) = store.edit {
        it[Keys.ENABLED] = enabled
        if (enabled) {
            it[Keys.CONSENT] = true
            it[Keys.PAUSED] = false
        }
    }

    suspend fun setPaused(paused: Boolean) = store.edit { it[Keys.PAUSED] = paused }
    suspend fun setAutoSave(value: Boolean) = store.edit { it[Keys.AUTO_SAVE] = value }
    suspend fun setHaptic(value: Boolean) = store.edit { it[Keys.HAPTIC] = value }

    suspend fun setExcluded(packageName: String, excluded: Boolean) = store.edit {
        val current = it[Keys.EXCLUDED] ?: emptySet()
        it[Keys.EXCLUDED] = if (excluded) current + packageName else current - packageName
    }
}
