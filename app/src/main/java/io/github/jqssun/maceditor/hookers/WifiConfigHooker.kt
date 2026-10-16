package io.github.jqssun.maceditor.hookers

import android.content.res.Resources
import android.util.Log
import io.github.jqssun.maceditor.BuildConfig
import io.github.jqssun.maceditor.TAG
import io.github.jqssun.maceditor.utils.Toggle
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.util.concurrent.ConcurrentHashMap

class WifiConfigHooker {
    companion object {
        private var module: XposedModule? = null

        private val KEYS = mapOf(
            "config_wifi_connected_mac_randomization_supported" to Toggle.FORCE_STA,
            "config_wifi_ap_mac_randomization_supported" to Toggle.FORCE_AP,
            "config_wifi_p2p_mac_randomization_supported" to Toggle.FORCE_P2P
        )
        private val logged = ConcurrentHashMap.newKeySet<String>()

        fun hook(param: SystemServerStartingParam, module: XposedModule) {
            this.module = module
            module.hook(
                Resources::class.java.getDeclaredMethod("getBoolean", Int::class.javaPrimitiveType)
            ).intercept(ResourceBoolHooker())
        }

        class ResourceBoolHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val result = chain.proceed()
                val res = chain.thisObject as? Resources ?: return result
                val id = chain.getArg(0) as Int
                try {
                    val name = res.getResourceEntryName(id)
                    val toggle = KEYS[name] ?: return result
                    if (!toggle.get(module?.getRemotePreferences(BuildConfig.APPLICATION_ID))) return result
                    if (logged.add(name)) module?.log(Log.INFO, TAG, "Forced $name to true")
                    return true
                } catch (_: Resources.NotFoundException) {
                }
                return result
            }
        }
    }
}
