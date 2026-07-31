package com.lorfocus.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Single-value app state. Lists (feeds) live in Room; everything else here. */
data class AppSettings(
    val onboarded: Boolean = false,
    val theme: String = "system",   // light | dark | system
    val logo: String = "line",
    val strict: Boolean = true,
    val uninstall: Boolean = false,
    val cooldownMinutes: Int = 10,
    val goal: String = "I want my evenings back — reading, not scrolling until 1am.",
    // Timed Focus session: active while now < focusEndsAt (epoch millis). 0 = off.
    val focusEndsAt: Long = 0L,
    val focusDurationMin: Int = 30,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("lorfocus_prefs")

class Prefs(private val context: Context) {
    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val theme = stringPreferencesKey("theme")
        val logo = stringPreferencesKey("logo")
        val strict = booleanPreferencesKey("strict")
        val uninstall = booleanPreferencesKey("uninstall")
        val cooldown = intPreferencesKey("cooldown")
        val goal = stringPreferencesKey("goal")
        val focusEndsAt = longPreferencesKey("focusEndsAt")
        val focusDurationMin = intPreferencesKey("focusDurationMin")
    }

    val flow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = AppSettings()
        AppSettings(
            onboarded = p[K.onboarded] ?: d.onboarded,
            theme = p[K.theme] ?: d.theme,
            logo = p[K.logo] ?: d.logo,
            strict = p[K.strict] ?: d.strict,
            uninstall = p[K.uninstall] ?: d.uninstall,
            cooldownMinutes = p[K.cooldown] ?: d.cooldownMinutes,
            goal = p[K.goal] ?: d.goal,
            focusEndsAt = p[K.focusEndsAt] ?: d.focusEndsAt,
            focusDurationMin = p[K.focusDurationMin] ?: d.focusDurationMin,
        )
    }

    suspend fun setOnboarded(v: Boolean) = context.dataStore.edit { it[K.onboarded] = v }
    suspend fun setTheme(v: String) = context.dataStore.edit { it[K.theme] = v }
    suspend fun setLogo(v: String) = context.dataStore.edit { it[K.logo] = v }
    suspend fun setStrict(v: Boolean) = context.dataStore.edit { it[K.strict] = v }
    suspend fun setUninstall(v: Boolean) = context.dataStore.edit { it[K.uninstall] = v }
    suspend fun setGoal(v: String) = context.dataStore.edit { it[K.goal] = v }
    suspend fun setFocusEnds(v: Long) = context.dataStore.edit { it[K.focusEndsAt] = v }
    suspend fun setFocusDuration(v: Int) = context.dataStore.edit { it[K.focusDurationMin] = v }
    suspend fun clear() { context.dataStore.edit { it.clear() } }   // F9.5 reset all data
}
