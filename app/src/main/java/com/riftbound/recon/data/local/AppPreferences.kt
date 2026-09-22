package com.riftbound.recon.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

    companion object {
        private const val PREFS_NAME = "recon_preferences"
        const val KEY_SKIP_DELETE_CARD_CONFIRMATION = "skip_delete_card_confirmation"
    }
}
