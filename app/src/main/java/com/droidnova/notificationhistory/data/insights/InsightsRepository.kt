package com.droidnova.notificationhistory.data.insights

import android.content.pm.PackageManager
import androidx.sqlite.db.SimpleSQLiteQuery
import com.droidnova.notificationhistory.data.db.NotificationDao
import com.droidnova.notificationhistory.data.mapper.fetchAppName
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class AppStat(
    val packageName: String,
    val appName: String,
    val count: Int,
    /** Rounded share of the range total, 0–100. */
    val percent: Int
)

data class BusiestHour(
    val hourOfDay: Int,
    val label: String,
    val count: Int
)

data class ChartBucket(
    val label: String,
    val count: Int,
    /** True for the bucket containing "now" (this hour, today, this week...), highlighted in the chart. */
    val isCurrent: Boolean = false
)

data class InsightsData(
    val range: InsightsRange,
    val bucketUnit: BucketUnit,
    val totalNotifications: Int,
    val activeApps: Int,
    val mostActiveApp: AppStat?,
    val busiestHour: BusiestHour?,
    val buckets: List<ChartBucket>,
    /** Indexes into [buckets] whose label is rendered under the chart. */
    val labelledBuckets: Set<Int>,
    val topApps: List<AppStat>
)

/**
 * Reactive statistics over active (non-trashed) notifications. A bounded set of aggregate
 * queries runs per range; Room re-emits them whenever the `apps` table changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InsightsRepository(
    private val dao: NotificationDao,
    private val packageManager: PackageManager,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
    private val now: () -> Instant = { Instant.now() }
) {

    fun observe(range: InsightsRange): Flow<InsightsData> {
        // Only All Time depends on the data span; other ranges plan from the clock alone.
        val earliest: Flow<Long?> = if (range == InsightsRange.AllTime) {
            dao.observeEarliestActiveTimestamp().distinctUntilChanged()
        } else {
            flowOf(null)
        }
        return earliest
            .map { InsightsPlanner.plan(range, now(), zone(), it) }
            .distinctUntilChanged()
            .flatMapLatest { plan -> observe(plan) }
            .conflate()
    }

    private fun observe(plan: InsightsPlan): Flow<InsightsData> {
        val summary = dao.observeInsightsSummary(plan.startInclusive, plan.endExclusive)
        val topApps = dao.observeTopApps(plan.startInclusive, plan.endExclusive, TOP_APPS_LIMIT)
        val buckets = dao.observeBucketCounts(InsightsQueries.bucketCounts(plan).toRoomQuery())
        val busiestHour = dao.observeBusiestHour(InsightsQueries.busiestHour(plan).toRoomQuery())

        return combine(summary, topApps, buckets, busiestHour) { total, apps, counts, hours ->
            val appStats = apps.map { row ->
                AppStat(
                    packageName = row.packageName,
                    appName = fetchAppName(packageManager, row.packageName),
                    count = row.count,
                    percent = percentOf(row.count, total.total)
                )
            }
            val countsByBucket = counts.associate { it.bucket to it.count }
            val nowMillis = now().toEpochMilli()
            InsightsData(
                range = plan.range,
                bucketUnit = plan.bucketUnit,
                totalNotifications = total.total,
                activeApps = total.activeApps,
                mostActiveApp = appStats.firstOrNull(),
                busiestHour = hours.firstOrNull()?.let { row ->
                    BusiestHour(
                        hourOfDay = row.hourOfDay,
                        label = InsightsPlanner.hourLabel(row.hourOfDay),
                        count = row.count
                    )
                },
                // Missing buckets are zero-filled so the timeline stays continuous.
                buckets = plan.buckets.mapIndexed { index, bucket ->
                    ChartBucket(
                        label = bucket.label,
                        count = countsByBucket[index] ?: 0,
                        isCurrent = nowMillis >= bucket.startInclusive && nowMillis < bucket.endExclusive
                    )
                },
                labelledBuckets = plan.labelledBucketIndexes(MAX_CHART_LABELS),
                topApps = appStats
            )
        }
    }

    private fun SqlWithArgs.toRoomQuery() = SimpleSQLiteQuery(sql, args.toTypedArray())

    private fun percentOf(count: Int, total: Int): Int =
        if (total <= 0) 0 else ((count * 100L + total / 2) / total).toInt().coerceIn(0, 100)

    private companion object {
        const val TOP_APPS_LIMIT = 5
        const val MAX_CHART_LABELS = 6
    }
}
