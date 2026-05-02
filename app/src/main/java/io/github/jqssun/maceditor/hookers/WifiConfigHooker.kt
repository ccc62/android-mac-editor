package io.github.jqssun.maceditor.hookers

import android.content.res.Resources
import io.github.jqssun.maceditor.BuildConfig
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import android.util.Log

class WifiConfigHooker {
    companion object {
        private var module: XposedModule? = null
        private var isMacRandomizationForced: Boolean? = null

        private val TARGET_KEYS = setOf(
            "config_wifi_connected_mac_randomization_supported",
            "config_wifi_p2p_mac_randomization_supported",
            "config_wifi_ap_mac_randomization_supported"
        )

        fun hook(param: SystemServerStartingParam, module: XposedModule) {
            this.module = module
            module.hook(
                Resources::class.java.getDeclaredMethod("getBoolean", Int::class.javaPrimitiveType)
            ).intercept(ResourceBoolHooker())
        }

        class ResourceBoolHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                // 缓存 forceShowMacRandomization 的状态，避免频繁访问 SharedPreferences
                if (isMacRandomizationForced == null) {
                    val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                    isMacRandomizationForced = prefs?.getBoolean("forceShowMacRandomization", true) ?: true
                }

                // 如果配置未启用强制设置，直接返回原结果
                if (isMacRandomizationForced != true) return chain.proceed()

                val res = chain.thisObject as? Resources ?: return chain.proceed()
                val id = chain.getArg(0) as Int
                try {
                    val name = res.getResourceEntryName(id)
                    if (name in TARGET_KEYS) {
                        // 只在强制修改时记录日志，避免频繁记录
                        // module?.log(Log.INFO, TAG, "Forced $name to true") // 如果不需要日志，注释掉
                        return true
                    }
                } catch (_: Resources.NotFoundException) {
                }
                return chain.proceed()
            }
        }
    }
}
