package com.droidnova.notificationhistory.data.insights

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class InsightsRange { Today, Last7Days, Last30Days, AllTime }

/** Chart bucket granularity; [AllTime] picks one adaptively from the data span. */
enum class BucketUnit { Hour, Day, Week, Month, Year }

/** One `[startInclusive, endExclusive)` slice of the selected range, in local time. */
data class TimeBucket(
    val startInclusive: Long,
    val endExclusive: Long,
    val label: String
)

/**
 * A stretch of the selected range with a constant UTC offset. Used to derive the local
 * hour-of-day in SQL exactly, even when the range crosses a DST transition.
 */
data class OffsetSegment(
    val startInclusive: Long,
    val offsetMillis: Long
)

/**
 * Everything the statistics queries need for one range, computed once in Kotlin with
 * [ZoneId] rules so that day boundaries and DST are handled outside SQL.
 */
data class InsightsPlan(
    val range: InsightsRange,
    /** Bounds applied to totals and top apps. `Long.MIN_VALUE`/`MAX_VALUE` for All Time. */
    val startInclusive: Long,
    val endExclusive: Long,
    val bucketUnit: BucketUnit,
    val buckets: List<TimeBucket>,
    val offsetSegments: List<OffsetSegment>
) {
    /** Indexes of buckets whose label should be rendered so labels never overlap. */
    fun labelledBucketIndexes(maxLabels: Int): Set<Int> {
        if (buckets.isEmpty() || maxLabels <= 0) return emptySet()
        val step = ((buckets.size + maxLabels - 1) / maxLabels).coerceAtLeast(1)
        return buckets.indices.filter { it % step == 0 }.toSet()
    }
}

object InsightsPlanner {

    private const val MAX_DAILY_BUCKETS = 31
    private const val MAX_WEEKLY_BUCKETS = 30
    private const val MAX_MONTHLY_BUCKETS = 36
    private const val MAX_OFFSET_SEGMENTS = 200

    fun plan(
        range: InsightsRange,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        earliestActiveTimestamp: Long? = null,
        locale: Locale = Locale.getDefault()
    ): InsightsPlan {
        val today = now.atZone(zone).toLocalDate()
        val tomorrowStart = today.plusDays(1).startOfDay(zone)

        return when (range) {
            InsightsRange.Today -> {
                val start = today.startOfDay(zone)
                build(range, start, tomorrowStart, BucketUnit.Hour, zone, locale, bounded = true)
            }
            InsightsRange.Last7Days -> {
                val start = today.minusDays(6).startOfDay(zone)
                build(range, start, tomorrowStart, BucketUnit.Day, zone, locale, bounded = true)
            }
            InsightsRange.Last30Days -> {
                val start = today.minusDays(29).startOfDay(zone)
                build(range, start, tomorrowStart, BucketUnit.Day, zone, locale, bounded = true)
            }
            InsightsRange.AllTime -> {
                val earliestDate = earliestActiveTimestamp
                    ?.let { runCatching { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.getOrNull() }
                    ?.takeIf { !it.isAfter(today) }
                    ?: today
                val spanDays = ChronoUnit.DAYS.between(earliestDate, today) + 1
                val unit = when {
                    spanDays <= MAX_DAILY_BUCKETS -> BucketUnit.Day
                    spanDays <= MAX_WEEKLY_BUCKETS * 7L -> BucketUnit.Week
                    ChronoUnit.MONTHS.between(
                        earliestDate.withDayOfMonth(1),
                        today.withDayOfMonth(1)
                    ) < MAX_MONTHLY_BUCKETS -> BucketUnit.Month
                    else -> BucketUnit.Year
                }
                val start = earliestDate.alignTo(unit).startOfDay(zone)
                build(range, start, tomorrowStart, unit, zone, locale, bounded = false)
            }
        }
    }

    /** Human label such as `9 AM–10 AM` for a local hour of day. */
    fun hourLabel(hourOfDay: Int, locale: Locale = Locale.getDefault()): String {
        val hour = ((hourOfDay % 24) + 24) % 24
        val formatter = DateTimeFormatter.ofPattern("h a", locale)
        val start = LocalTime.of(hour, 0).format(formatter)
        val end = LocalTime.of((hour + 1) % 24, 0).format(formatter)
        return "$start–$end"
    }

    private fun build(
        range: InsightsRange,
        start: ZonedDateTime,
        end: ZonedDateTime,
        unit: BucketUnit,
        zone: ZoneId,
        locale: Locale,
        bounded: Boolean
    ): InsightsPlan {
        val buckets = buckets(start, end, unit, locale)
        return InsightsPlan(
            range = range,
            startInclusive = if (bounded) start.toEpochMilli() else Long.MIN_VALUE,
            endExclusive = if (bounded) end.toEpochMilli() else Long.MAX_VALUE,
            bucketUnit = unit,
            buckets = buckets,
            offsetSegments = offsetSegments(start.toInstant(), end.toInstant(), zone, bounded)
        )
    }

    private fun buckets(
        start: ZonedDateTime,
        end: ZonedDateTime,
        unit: BucketUnit,
        locale: Locale
    ): List<TimeBucket> {
        val formatter = DateTimeFormatter.ofPattern(
            when (unit) {
                BucketUnit.Hour -> "h a"
                BucketUnit.Day -> "d MMM"
                BucketUnit.Week -> "d MMM"
                BucketUnit.Month -> "MMM yy"
                BucketUnit.Year -> "yyyy"
            },
            locale
        )
        val result = mutableListOf<TimeBucket>()
        var cursor = start
        while (cursor.isBefore(end)) {
            // Stepping by calendar units (not fixed millis) keeps buckets aligned across DST.
            val next = when (unit) {
                BucketUnit.Hour -> cursor.plusHours(1)
                BucketUnit.Day -> cursor.toLocalDate().plusDays(1).startOfDay(cursor.zone)
                BucketUnit.Week -> cursor.toLocalDate().plusWeeks(1).startOfDay(cursor.zone)
                BucketUnit.Month -> cursor.toLocalDate().plusMonths(1).startOfDay(cursor.zone)
                BucketUnit.Year -> cursor.toLocalDate().plusYears(1).startOfDay(cursor.zone)
            }
            val bucketEnd = if (next.isAfter(end)) end else next
            result += TimeBucket(
                startInclusive = cursor.toEpochMilli(),
                endExclusive = bucketEnd.toEpochMilli(),
                label = cursor.format(formatter)
            )
            cursor = next
        }
        return result
    }

    /**
     * Splits `[start, end)` at every zone offset transition. For All Time the first segment
     * also covers timestamps before [start] and the last one covers timestamps after [end].
     */
    private fun offsetSegments(
        start: Instant,
        end: Instant,
        zone: ZoneId,
        bounded: Boolean
    ): List<OffsetSegment> {
        val rules = zone.rules
        val segments = mutableListOf(
            OffsetSegment(
                startInclusive = if (bounded) start.toEpochMilli() else Long.MIN_VALUE,
                offsetMillis = rules.getOffset(start).totalSeconds * 1000L
            )
        )
        var transition = rules.nextTransition(start)
        while (transition != null && transition.instant.isBefore(end) && segments.size < MAX_OFFSET_SEGMENTS) {
            segments += OffsetSegment(
                startInclusive = transition.instant.toEpochMilli(),
                offsetMillis = transition.offsetAfter.totalSeconds * 1000L
            )
            transition = rules.nextTransition(transition.instant)
        }
        return segments
    }

    private fun LocalDate.alignTo(unit: BucketUnit): LocalDate = when (unit) {
        BucketUnit.Hour, BucketUnit.Day -> this
        BucketUnit.Week -> with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        BucketUnit.Month -> withDayOfMonth(1)
        BucketUnit.Year -> withDayOfYear(1)
    }

    private fun LocalDate.startOfDay(zone: ZoneId): ZonedDateTime = atStartOfDay(zone)

    private fun ZonedDateTime.toEpochMilli(): Long = toInstant().toEpochMilli()
}
