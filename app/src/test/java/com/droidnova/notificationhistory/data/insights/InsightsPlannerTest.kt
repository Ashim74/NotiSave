package com.droidnova.notificationhistory.data.insights

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsPlannerTest {

    private val zone: ZoneId = ZoneId.of("America/New_York")
    private val locale = Locale.US

    private fun at(date: LocalDate, hour: Int = 15) =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, 0)).atZone(zone).toInstant()

    private fun LocalDate.startMillis() = atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `today spans one local day with hourly buckets`() {
        val today = LocalDate.of(2024, 6, 12)
        val plan = InsightsPlanner.plan(InsightsRange.Today, at(today), zone, locale = locale)

        assertEquals(today.startMillis(), plan.startInclusive)
        assertEquals(today.plusDays(1).startMillis(), plan.endExclusive)
        assertEquals(BucketUnit.Hour, plan.bucketUnit)
        assertEquals(24, plan.buckets.size)
        assertEquals("12 AM", plan.buckets.first().label)
        assertEquals("11 PM", plan.buckets.last().label)
        assertContiguous(plan)
    }

    @Test
    fun `dst day has 23 hourly buckets and a single range`() {
        val dstDay = LocalDate.of(2024, 3, 10)
        val plan = InsightsPlanner.plan(InsightsRange.Today, at(dstDay), zone, locale = locale)

        assertEquals(23, plan.buckets.size)
        assertEquals(Duration.ofHours(23).toMillis(), plan.endExclusive - plan.startInclusive)
        assertContiguous(plan)
    }

    @Test
    fun `last 7 days ends tomorrow and starts six days ago`() {
        val today = LocalDate.of(2024, 6, 12)
        val plan = InsightsPlanner.plan(InsightsRange.Last7Days, at(today), zone, locale = locale)

        assertEquals(today.minusDays(6).startMillis(), plan.startInclusive)
        assertEquals(today.plusDays(1).startMillis(), plan.endExclusive)
        assertEquals(7, plan.buckets.size)
        assertEquals(BucketUnit.Day, plan.bucketUnit)
        assertContiguous(plan)
    }

    @Test
    fun `last 30 days has 30 daily buckets across a dst transition`() {
        val today = LocalDate.of(2024, 3, 20)
        val plan = InsightsPlanner.plan(InsightsRange.Last30Days, at(today), zone, locale = locale)

        assertEquals(30, plan.buckets.size)
        assertEquals(today.minusDays(29).startMillis(), plan.startInclusive)
        assertContiguous(plan)
        // The transition on 10 March splits the range into two UTC-offset segments.
        assertEquals(2, plan.offsetSegments.size)
        assertEquals(-5L * 3_600_000, plan.offsetSegments[0].offsetMillis)
        assertEquals(-4L * 3_600_000, plan.offsetSegments[1].offsetMillis)
        assertNotEquals(plan.offsetSegments[0].startInclusive, plan.offsetSegments[1].startInclusive)
    }

    @Test
    fun `all time without data falls back to today`() {
        val today = LocalDate.of(2024, 6, 12)
        val plan = InsightsPlanner.plan(InsightsRange.AllTime, at(today), zone, null, locale)

        assertEquals(Long.MIN_VALUE, plan.startInclusive)
        assertEquals(Long.MAX_VALUE, plan.endExclusive)
        assertEquals(1, plan.buckets.size)
        assertEquals(Long.MIN_VALUE, plan.offsetSegments.first().startInclusive)
    }

    @Test
    fun `all time picks daily weekly monthly or yearly buckets`() {
        val today = LocalDate.of(2024, 6, 12)

        fun unitFor(earliest: LocalDate) =
            InsightsPlanner.plan(InsightsRange.AllTime, at(today), zone, earliest.startMillis(), locale)

        val daily = unitFor(today.minusDays(20))
        assertEquals(BucketUnit.Day, daily.bucketUnit)
        assertEquals(21, daily.buckets.size)

        val weekly = unitFor(today.minusDays(90))
        assertEquals(BucketUnit.Week, weekly.bucketUnit)
        assertTrue(weekly.buckets.size in 13..15)

        val monthly = unitFor(today.minusYears(2))
        assertEquals(BucketUnit.Month, monthly.bucketUnit)
        assertEquals(25, monthly.buckets.size)
        assertEquals("Jun 22", monthly.buckets.first().label)

        val yearly = unitFor(today.minusYears(6))
        assertEquals(BucketUnit.Year, yearly.bucketUnit)
        assertEquals(7, yearly.buckets.size)
        assertEquals("2018", yearly.buckets.first().label)

        listOf(daily, weekly, monthly, yearly).forEach(::assertContiguous)
    }

    @Test
    fun `future or malformed earliest timestamp is ignored`() {
        val today = LocalDate.of(2024, 6, 12)
        val plan = InsightsPlanner.plan(
            InsightsRange.AllTime, at(today), zone, today.plusDays(10).startMillis(), locale
        )

        assertEquals(1, plan.buckets.size)
    }

    @Test
    fun `labelled bucket indexes are spread and never exceed the maximum`() {
        val today = LocalDate.of(2024, 6, 12)
        val plan = InsightsPlanner.plan(InsightsRange.Last30Days, at(today), zone, locale = locale)

        val labelled = plan.labelledBucketIndexes(6)
        assertTrue(labelled.size <= 6)
        assertTrue(0 in labelled)
        assertEquals(setOf(0, 5, 10, 15, 20, 25), labelled)
    }

    @Test
    fun `hour label is a readable local range`() {
        assertEquals("9 AM–10 AM", InsightsPlanner.hourLabel(9, locale))
        assertEquals("11 PM–12 AM", InsightsPlanner.hourLabel(23, locale))
        assertEquals("12 AM–1 AM", InsightsPlanner.hourLabel(0, locale))
    }

    @Test
    fun `bucket query has one boundary per bucket gap and the range bounds`() {
        val today = LocalDate.of(2024, 6, 12)
        val plan = InsightsPlanner.plan(InsightsRange.Last7Days, at(today), zone, locale = locale)

        val query = InsightsQueries.bucketCounts(plan)

        assertEquals(6 + 2, query.args.size)
        assertEquals(plan.buckets[0].endExclusive, query.args[0])
        assertEquals(plan.startInclusive, query.args[6])
        assertEquals(plan.endExclusive, query.args[7])
        assertTrue(query.sql.contains("ELSE 6 END"))
    }

    @Test
    fun `busiest hour query uses one offset per dst segment`() {
        val today = LocalDate.of(2024, 3, 20)
        val plan = InsightsPlanner.plan(InsightsRange.Last30Days, at(today), zone, locale = locale)

        val query = InsightsQueries.busiestHour(plan)

        // boundary + (offset, hourMillis) for the first segment, (offset, hourMillis) for the ELSE, bounds.
        assertEquals(1 + 2 + 2 + 2, query.args.size)
        assertEquals(plan.offsetSegments[1].startInclusive, query.args[0])
        assertEquals(plan.offsetSegments[0].offsetMillis, query.args[1])
        assertEquals(plan.offsetSegments[1].offsetMillis, query.args[3])
        assertTrue(query.sql.contains("ORDER BY count DESC, hourOfDay ASC"))
    }

    private fun assertContiguous(plan: InsightsPlan) {
        plan.buckets.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endExclusive, next.startInclusive)
        }
        plan.buckets.forEach { assertTrue(it.startInclusive < it.endExclusive) }
        if (plan.startInclusive != Long.MIN_VALUE) {
            assertEquals(plan.startInclusive, plan.buckets.first().startInclusive)
            assertEquals(plan.endExclusive, plan.buckets.last().endExclusive)
        }
    }
}
