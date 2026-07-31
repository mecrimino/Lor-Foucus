package com.lorfocus.app.ui

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * F6.2 — optional uninstall protection. While this admin is active the OS won't let the app
 * be uninstalled without first deactivating it here, which slows an impulsive removal. The app
 * explains removal plainly and the user can turn it off any time (Strict mode).
 */
class LorDeviceAdminReceiver : DeviceAdminReceiver()

object DeviceAdmin {
    private fun component(ctx: Context) = ComponentName(ctx, LorDeviceAdminReceiver::class.java)
    private fun dpm(ctx: Context) = ctx.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    fun isActive(ctx: Context): Boolean = dpm(ctx).isAdminActive(component(ctx))

    fun requestEnable(ctx: Context) {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(ctx))
            .putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Optional. Makes Lor Focus harder to uninstall on impulse. " +
                    "You can turn this off any time in Strict mode."
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    fun disable(ctx: Context) {
        runCatching { dpm(ctx).removeActiveAdmin(component(ctx)) }
    }
}
