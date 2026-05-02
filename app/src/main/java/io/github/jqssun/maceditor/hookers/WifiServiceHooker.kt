package io.github.jqssun.maceditor.hookers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.MacAddress
import io.github.jqssun.maceditor.BuildConfig
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.lang.reflect.Method

class WifiServiceHooker {
    companion object {
        var module: XposedModule? = null
            private set

        const val ACTION_APPLY_MAC = "${BuildConfig.APPLICATION_ID}.ACTION_APPLY_MAC"
        const val ACTION_MAC_DETECTED = "${BuildConfig.APPLICATION_ID}.ACTION_MAC_DETECTED"
        private const val RECEIVER_CLASS = "${BuildConfig.APPLICATION_ID}.MacBroadcastReceiver"

        // cached WifiNative state
        private var nativeInstance: Any? = null
        private var nativeSetStaMethod: Method? = null
        private var lastIfaceName: String? = null
        private var receiverRegistered = false

        @SuppressLint("PrivateApi")
        fun hook(param: SystemServerStartingParam, module: XposedModule) {
            this.module = module
            module.hook(
                param.classLoader.loadClass("com.android.server.SystemServiceManager")
                    .getDeclaredMethod("loadClassFromLoader", String::class.java, ClassLoader::class.java)
            ).intercept { chain ->
                val result = chain.proceed()
                val className = chain.getArg(0) as String
                if (className == "com.android.server.wifi.WifiService") {
                    val cl = chain.getArg(1) as ClassLoader
                    val nativeClass = cl.loadClass("com.android.server.wifi.WifiNative")
                    val setStaMethod = nativeClass.getDeclaredMethod("setStaMacAddress", String::class.java, MacAddress::class.java)
                    val setApMethod = nativeClass.getDeclaredMethod("setApMacAddress", String::class.java, MacAddress::class.java)
                    nativeSetStaMethod = setStaMethod
                    val hooker = MacAddrHooker()
                    module.hook(setStaMethod).intercept(hooker)
                    module.hook(setApMethod).intercept(hooker)
                }
                result
            }
        }

        @SuppressLint("PrivateApi")
        private fun _getSystemContext(): Context? {
            return try {
                val at = Class.forName("android.app.ActivityThread")
                at.getMethod("currentApplication").invoke(null) as? Context
            } catch (_: Exception) {
                null
            }
        }

        private fun _registerApplyReceiver() {
            if (receiverRegistered) return
            val ctx = _getSystemContext() ?: return
            ctx.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    _applyMacDirectly()
                }
            }, IntentFilter(ACTION_APPLY_MAC), Context.RECEIVER_EXPORTED)
            receiverRegistered = true
        }

        private fun _applyMacDirectly() {
            val native = nativeInstance
            val method = nativeSetStaMethod
            val iface = lastIfaceName
            if (native == null || method == null || iface == null) {
                return
            }
            val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
            val mac = prefs?.getString("customMac", "") ?: ""
            if (mac.isEmpty()) return

            try {
                // calls WifiNative.setStaMacAddress which does disconnect() + HAL call
                method.invoke(native, iface, MacAddress.fromString(mac))
            } catch (e: Exception) {
                // Failure to apply MAC, but no log output
            }
        }

        private fun _broadcastDeviceMac(mac: MacAddress) {
            try {
                val ctx = _getSystemContext() ?: return
                val intent = Intent(ACTION_MAC_DETECTED).apply {
                    putExtra("mac", mac.toString())
                    setClassName(BuildConfig.APPLICATION_ID, RECEIVER_CLASS)
                }
                ctx.sendBroadcast(intent)
            } catch (e: Exception) {
                // Failure to broadcast MAC, but no log output
            }
        }

        class MacAddrHooker : XposedInterface.Hooker {
            override fun intercept(chain: XposedInterface.Chain): Any? {
                val prefs = module?.getRemotePreferences(BuildConfig.APPLICATION_ID)
                val hookActive = prefs?.getBoolean("hookActive", true) ?: true

                if (!hookActive) return chain.proceed()

                // Cache WifiNative instance and iface
                nativeInstance = chain.thisObject
                lastIfaceName = chain.getArg(0) as? String
                _registerApplyReceiver()

                // Broadcast the system-assigned MAC to the app
                (chain.getArg(1) as? MacAddress)?.let { _broadcastDeviceMac(it) }

                val customMac = prefs?.getString("customMac", "") ?: ""
                if (customMac.isNotEmpty()) {
                    val args = chain.args.toTypedArray()
                    args[1] = MacAddress.fromString(customMac)
                    return chain.proceed(args)
                }

                return chain.proceed()
            }
        }
    }
}
