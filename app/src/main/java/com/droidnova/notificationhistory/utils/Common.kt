package com.droidnova.notificationhistory.utils

import android.content.Context
import android.content.pm.LauncherApps
import android.os.UserManager
import android.util.Log
import com.droidnova.notificationhistory.presentation.screens.select_app.AppInfo


fun getInstalledApps(context: Context, allowedList: Set<String>): List<AppInfo> {
    Log.e("Mantsh2232","getInstalledAppsCalled()  allowedList $allowedList")
    val appList = mutableListOf<AppInfo>()
    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val pm = context.packageManager
    val seen = HashSet<String>() // avoid duplicates across profiles/activities


    for (profile in userManager.userProfiles) {
        val activities = launcherApps.getActivityList(null, profile)
        for (app in activities) {
            val ai = app.applicationInfo
            if (!seen.add(ai.packageName)) continue

            val label = pm.getApplicationLabel(ai).toString()
            val isAllowed = ai.packageName in allowedList

            appList.add(AppInfo(packageName = ai.packageName, appName =  label, isAllowed= isAllowed))
        }
    }

    return appList
        .sortedWith(
            compareByDescending<AppInfo> { it.isAllowed }
                .thenBy { it.appName.lowercase() }
        )
}

