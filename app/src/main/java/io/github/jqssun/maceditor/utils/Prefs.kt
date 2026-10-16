package io.github.jqssun.maceditor.utils

import android.content.SharedPreferences

const val KEY_MAC = "customMac"

fun customMac(prefs: SharedPreferences?) = prefs?.getString(KEY_MAC, null).orEmpty()

enum class Toggle(val key: String, val def: Boolean) {
    OVERRIDE_STA("overrideSta", true),
    OVERRIDE_AP("overrideAp", false),
    FORCE_STA("forceSta", true),
    FORCE_AP("forceAp", false),
    FORCE_P2P("forceP2p", false);

    fun get(prefs: SharedPreferences?) = prefs?.getBoolean(key, def) ?: def
}
