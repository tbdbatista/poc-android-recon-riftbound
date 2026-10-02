package com.riftbound.recon.ui.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconTheme(val title: String, val isDark: Boolean) {
    DARK("Tema Escuro (Padrão)", true),
    LIGHT("Tema Claro", false)
}

object AppIconHelper {

    private const val DEFAULT_ACTIVITY = "com.riftbound.recon.ui.MainActivity"
    private const val LIGHT_ALIAS_ACTIVITY = "com.riftbound.recon.ui.MainActivityLightIcon"

    fun getCurrentIconTheme(context: Context): AppIconTheme {
        val pm = context.packageManager
        val lightState = pm.getComponentEnabledSetting(
            ComponentName(context.packageName, LIGHT_ALIAS_ACTIVITY)
        )
        return if (lightState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            AppIconTheme.LIGHT
        } else {
            AppIconTheme.DARK
        }
    }

    fun setAppIcon(context: Context, theme: AppIconTheme) {
        val pm = context.packageManager
        val defaultComponent = ComponentName(context.packageName, DEFAULT_ACTIVITY)
        val lightComponent = ComponentName(context.packageName, LIGHT_ALIAS_ACTIVITY)

        when (theme) {
            AppIconTheme.DARK -> {
                // Enable default Dark icon, disable Light icon alias
                pm.setComponentEnabledSetting(
                    defaultComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    lightComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            AppIconTheme.LIGHT -> {
                // Enable Light icon alias, disable default Dark icon
                pm.setComponentEnabledSetting(
                    lightComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    defaultComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
