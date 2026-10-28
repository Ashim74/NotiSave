package com.droidnova.notificationhistory.presentation.components

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.utils.CrashReporter
import java.util.Locale

/** Vendors whose battery managers kill background apps beyond what stock Android does. */
enum class OemVendor { Samsung, Xiaomi, Oppo, Vivo, Huawei, Other }

internal fun oemVendorOf(manufacturer: String?): OemVendor =
    when (manufacturer.orEmpty().trim().lowercase(Locale.ROOT)) {
        "samsung" -> OemVendor.Samsung
        "xiaomi", "redmi", "poco" -> OemVendor.Xiaomi
        "oppo", "realme" -> OemVendor.Oppo
        "vivo", "iqoo" -> OemVendor.Vivo
        "huawei", "honor" -> OemVendor.Huawei
        else -> OemVendor.Other
    }

/**
 * Done hides the step only while capture works. A listener that stays disconnected is exactly
 * what the step fixes (MIUI rejects every bind without autostart), so it comes back then.
 */
internal fun shouldShowOemStep(
    hasStep: Boolean,
    dismissed: Boolean,
    captureExpected: Boolean,
    listenerConnected: Boolean
): Boolean = hasStep && (!dismissed || (captureExpected && !listenerConnected))

/** One extra "let the app run in the background" step for this phone's maker. */
class OemReliabilityStep(
    @StringRes val textRes: Int,
    private val intent: Intent
) {
    fun open(context: Context) {
        try {
            context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            CrashReporter.record(e)
        } catch (e: SecurityException) {
            // Some OEM screens resolve but aren't exported to third-party apps.
            CrashReporter.record(e)
            openAppDetails(context)
        }
    }
}

/**
 * The step for this device, or null when there's nothing beyond battery optimization to do.
 * Samsung always gets one (its "Never sleeping apps" list is the fix for the reported kills),
 * falling back to the app's own battery page; other vendors only when their screen resolves.
 */
fun oemReliabilityStep(context: Context): OemReliabilityStep? {
    val vendor = oemVendorOf(Build.MANUFACTURER)
    val pm = context.packageManager
    val resolved = candidateIntents(vendor).firstOrNull { it.resolves(pm) }
    return when {
        vendor == OemVendor.Samsung -> OemReliabilityStep(
            R.string.reliability_step_samsung,
            resolved ?: appBatteryIntent(context).takeIf { it.resolves(pm) } ?: appDetailsIntent(context)
        )
        resolved != null -> OemReliabilityStep(R.string.reliability_step_autostart, resolved)
        else -> null
    }
}

private fun candidateIntents(vendor: OemVendor): List<Intent> = when (vendor) {
    OemVendor.Samsung -> listOf(
        component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
    )
    OemVendor.Xiaomi -> listOf(
        component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
    )
    OemVendor.Oppo -> listOf(
        component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
    )
    OemVendor.Vivo -> listOf(
        component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
    )
    OemVendor.Huawei -> listOf(
        component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
    )
    OemVendor.Other -> emptyList()
}

private fun component(pkg: String, cls: String) = Intent().setComponent(ComponentName(pkg, cls))

/** The app's own battery usage page (Allow background usage); not on every build. */
private fun appBatteryIntent(context: Context) =
    Intent("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL")
        .setData(Uri.parse("package:${context.packageName}"))

private fun appDetailsIntent(context: Context) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))

private fun openAppDetails(context: Context) {
    runCatching {
        context.startActivity(appDetailsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure(CrashReporter::record)
}

private fun Intent.resolves(pm: PackageManager): Boolean =
    runCatching { resolveActivity(pm) != null }.getOrDefault(false)
