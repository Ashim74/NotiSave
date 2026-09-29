package com.droidnova.notificationhistory.data.db

/** Totals for a `[start, end)` range of active notifications. */
data class InsightsSummaryRow(
    val total: Int,
    val activeApps: Int
)

/** Active notification count of one package within a range. */
data class AppCountRow(
    val packageName: String,
    val count: Int
)

/** Active notification count of one chart bucket (index into the plan's bucket list). */
data class BucketCountRow(
    val bucket: Int,
    val count: Int
)

/** Local hour of day (0–23) with its active notification count. */
data class HourCountRow(
    val hourOfDay: Int,
    val count: Int
)
