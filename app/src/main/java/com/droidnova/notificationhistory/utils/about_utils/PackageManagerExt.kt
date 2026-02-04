package com.droidnova.notificationhistory.utils.about_utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

object PackageManagerExt {

    fun isPackageInstalled(packageName: String, context: Context): Boolean {
        return getPackageInfo(context.packageManager, packageName) != null
    }

    fun getApplicationInfo(pm: PackageManager, packageName: String, flags: Int = 0): ApplicationInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, flags)
            }
        }.getOrNull()
    }

    fun getApplicationLabel(pm: PackageManager, packageName: String, flags: Int = 0): String? {
        val info = getApplicationInfo(pm, packageName, flags) ?: return null
        return runCatching { pm.getApplicationLabel(info).toString() }.getOrNull()
    }

    fun getApplicationIcon(pm: PackageManager, packageName: String, flags: Int = 0) =
        getApplicationInfo(pm, packageName, flags)?.let { info ->
            runCatching { pm.getApplicationIcon(info) }.getOrNull()
        }

    fun getPackageInfo(pm: PackageManager, packageName: String, flags: Int = 0): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
        }.getOrNull()
    }
}
