package io.github.jqssun.maceditor.utils

import android.content.SharedPreferences
import io.github.jqssun.maceditor.BuildConfig
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

class PrefManager {
    companion object {
        private var prefs: SharedPreferences? = null
        private var loaded = false

        fun loadPrefs(onReady: (() -> Unit)? = null) {
            if (loaded) {
                if (XposedChecker.isEnabled()) onReady?.invoke()
                return
            }
            loaded = true
            XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    XposedChecker.flagAsEnabled()
                    prefs = service.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    onReady?.invoke()
                }

                override fun onServiceDied(service: XposedService) {}
            })
        }

        fun get(t: Toggle) = t.get(prefs)

        fun set(t: Toggle, on: Boolean) {
            prefs?.edit()?.putBoolean(t.key, on)?.apply()
        }

        fun getCustomMac() = customMac(prefs)

        fun setCustomMac(mac: String) {
            prefs?.edit()?.putString(KEY_MAC, mac)?.apply()
        }
    }
}
