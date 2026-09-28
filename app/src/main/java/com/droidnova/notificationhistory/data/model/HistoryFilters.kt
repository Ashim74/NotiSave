package com.droidnova.notificationhistory.data.model

import java.time.LocalDate
import java.time.ZoneId

enum class HistoryDateFilter {
    AllTime,
    Today,
    Yesterday,
    Last7Days,
    Last30Days,
    Custom
}

data class HistoryFilterState(
    val searchQuery: String = "",
    val packageName: String? = null,
    val dateFilter: HistoryDateFilter = HistoryDateFilter.AllTime,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null
) {
    val hasActiveFilters: Boolean
        get() = packageName != null || dateFilter != HistoryDateFilter.AllTime
}

data class HistoryDateBounds(
    val startInclusive: Long,
    val endExclusive: Long
)

fun HistoryFilterState.toDateBounds(
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): HistoryDateBounds {
    val (startDate, endDateExclusive) = when (dateFilter) {
        HistoryDateFilter.AllTime -> return HistoryDateBounds(Long.MIN_VALUE, Long.MAX_VALUE)
        HistoryDateFilter.Today -> today to today.plusDays(1)
        HistoryDateFilter.Yesterday -> today.minusDays(1) to today
        HistoryDateFilter.Last7Days -> today.minusDays(6) to today.plusDays(1)
        HistoryDateFilter.Last30Days -> today.minusDays(29) to today.plusDays(1)
        HistoryDateFilter.Custom -> {
            val start = requireNotNull(customStartDate) { "Custom start date is required" }
            val end = requireNotNull(customEndDate) { "Custom end date is required" }
            require(!start.isAfter(end)) { "Start date must not be after end date" }
            start to end.plusDays(1)
        }
    }

    return HistoryDateBounds(
        startInclusive = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endExclusive = endDateExclusive.atStartOfDay(zoneId).toInstant().toEpochMilli()
    )
}
