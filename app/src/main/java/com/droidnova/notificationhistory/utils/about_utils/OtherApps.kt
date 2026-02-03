package com.droidnova.notificationhistory.utils.about_utils

import com.droidnova.notificationhistory.R

data class OtherAppItem(
    val titleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int,
    val packageName: String
)

private val otherApps = listOf(
    OtherAppItem(
        titleRes = R.string.other_app_clipboard_title,
        descriptionRes = R.string.other_app_clipboard_description,
        iconRes = R.drawable.ic_clipboard_history,
        packageName = "com.droidnova.clipboardhistory"
    ),
//    OtherAppItem(
//        titleRes = R.string.notification_history_app_title,
//        descriptionRes = R.string.notification_history_app_description,
//        iconRes = R.drawable.ic_notification_history,
//        packageName = "com.droidnova.notificationhistory"
//    ),
    OtherAppItem(
        titleRes = R.string.other_app_flash_alert_title,
        descriptionRes = R.string.other_app_flash_alert_description,
        iconRes = R.drawable.ic_flash_alert,
        packageName = "com.droidnova.flashalert"
    ),
    OtherAppItem(
        titleRes = R.string.secret_calculator_app_title,
        descriptionRes = R.string.secret_calculator_app_description,
        iconRes = R.drawable.ic_calculator,
        packageName = "com.droidnova.secretcalculator"
    ),
    OtherAppItem(
        titleRes = R.string.bvr_app_title,
        descriptionRes = R.string.bvr_app_description,
        iconRes = R.drawable.ic_bvr,
        packageName = "com.droidnova.backgroundcamera"
    ),
)

fun getRandomOtherApps(count: Int = 2): List<OtherAppItem> {
    if (otherApps.isEmpty()) return emptyList()
    val filteredApps = otherApps.filter { it.packageName != "com.droidnova.notificationhistory" }
    return filteredApps.shuffled().take(count.coerceAtMost(filteredApps.size))
}
