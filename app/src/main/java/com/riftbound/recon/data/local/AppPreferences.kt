package com.riftbound.recon.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode(val title: String, val description: String) {
    DARK("Modo Escuro", "Aparência Obsidian & Glow Arcane"),
    LIGHT("Modo Claro", "Aparência Ice-White & Royal Purple"),
    SYSTEM("Padrão do Sistema", "Acompanha o tema configurado no dispositivo")
}

@Singleton
class AppPreferences(
    private val prefsProvider: () -> SharedPreferences
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this({
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    })

    var skipDeleteCardConfirmation: Boolean
        get() = prefsProvider().getBoolean(KEY_SKIP_DELETE_CARD_CONFIRMATION, false)
        set(value) = prefsProvider().edit().putBoolean(KEY_SKIP_DELETE_CARD_CONFIRMATION, value).apply()

    var themeMode: String
        get() = prefsProvider().getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        set(value) = prefsProvider().edit().putString(KEY_THEME_MODE, value).apply()

    var appIconStyle: String
        get() = prefsProvider().getString(KEY_APP_ICON_STYLE, "DARK") ?: "DARK"
        set(value) = prefsProvider().edit().putString(KEY_APP_ICON_STYLE, value).apply()

    var isGuestMode: Boolean
        get() = prefsProvider().getBoolean(KEY_IS_GUEST_MODE, false)
        set(value) = prefsProvider().edit().putBoolean(KEY_IS_GUEST_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "recon_preferences"
        const val KEY_SKIP_DELETE_CARD_CONFIRMATION = "skip_delete_card_confirmation"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_APP_ICON_STYLE = "app_icon_style"
        const val KEY_IS_GUEST_MODE = "is_guest_mode"
    }
}

