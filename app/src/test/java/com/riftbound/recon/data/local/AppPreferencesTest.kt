package com.riftbound.recon.data.local

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class AppPreferencesTest {

    private lateinit var fakePrefs: SharedPreferences
    private lateinit var appPreferences: AppPreferences
    private val memoryStorage = mutableMapOf<String, Any?>()

    @Before
    fun setup() {
        memoryStorage.clear()
        fakePrefs = createFakeSharedPreferences(memoryStorage)
        appPreferences = AppPreferences { fakePrefs }
    }

    @Test
    fun default_skipDeleteCardConfirmation_isFalse() {
        assertFalse(appPreferences.skipDeleteCardConfirmation)
    }

    @Test
    fun setSkipDeleteCardConfirmation_persistsTrue() {
        appPreferences.skipDeleteCardConfirmation = true
        assertTrue(appPreferences.skipDeleteCardConfirmation)
    }

    @Test
    fun setSkipDeleteCardConfirmation_canBeToggledBackToFalse() {
        appPreferences.skipDeleteCardConfirmation = true
        assertTrue(appPreferences.skipDeleteCardConfirmation)

        appPreferences.skipDeleteCardConfirmation = false
        assertFalse(appPreferences.skipDeleteCardConfirmation)
    }

    private fun createFakeSharedPreferences(storage: MutableMap<String, Any?>): SharedPreferences {
        val editorHandler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                when (method?.name) {
                    "putBoolean" -> {
                        val key = args?.get(0) as String
                        val value = args.get(1) as Boolean
                        storage[key] = value
                        return proxy
                    }
                    "apply", "commit" -> {
                        return true
                    }
                    else -> return proxy
                }
            }
        }
        val editorProxy = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java),
            editorHandler
        ) as SharedPreferences.Editor

        val prefsHandler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
                when (method?.name) {
                    "getBoolean" -> {
                        val key = args?.get(0) as String
                        val defValue = args.get(1) as Boolean
                        return storage[key] as? Boolean ?: defValue
                    }
                    "edit" -> return editorProxy
                    else -> return null
                }
            }
        }

        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
            prefsHandler
        ) as SharedPreferences
    }
}
