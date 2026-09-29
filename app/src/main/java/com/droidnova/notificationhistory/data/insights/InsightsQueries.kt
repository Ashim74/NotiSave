package com.droidnova.notificationhistory.data.insights

/** Raw SQL plus bind arguments, kept free of Android types so it is unit-testable. */
data class SqlWithArgs(val sql: String, val args: List<Any>)

/**
 * Builds the two aggregate queries whose grouping depends on zone rules. Both scan
 * `index_apps_isTrashed_receivedAt_id` once and group in SQL, so no rows are loaded.
 */
object InsightsQueries {

    private const val MILLIS_PER_HOUR = 3_600_000L

    /** `bucket` index (per [InsightsPlan.buckets]) with its active notification count. */
    fun bucketCounts(plan: InsightsPlan): SqlWithArgs {
        val args = mutableListOf<Any>()
        val case = StringBuilder("CASE")
        // Boundaries between consecutive buckets; the last bucket is the ELSE branch, so for
        // All Time any timestamp after the range end (clock skew) still lands in a bucket.
        plan.buckets.dropLast(1).forEachIndexed { index, bucket ->
            case.append(" WHEN receivedAt < ? THEN ").append(index)
            args += bucket.endExclusive
        }
        case.append(" ELSE ").append(plan.buckets.lastIndex.coerceAtLeast(0)).append(" END")

        args += plan.startInclusive
        args += plan.endExclusive
        val sql = """
            SELECT bucket, COUNT(*) AS count FROM (
                SELECT $case AS bucket
                FROM apps
                WHERE isTrashed = 0 AND receivedAt >= ? AND receivedAt < ?
            )
            GROUP BY bucket
            ORDER BY bucket ASC
        """.trimIndent()
        return SqlWithArgs(sql, args)
    }

    /** Single row: the local hour of day with the most notifications (earliest hour on ties). */
    fun busiestHour(plan: InsightsPlan): SqlWithArgs {
        val args = mutableListOf<Any>()
        val hourExpression = StringBuilder()
        val segments = plan.offsetSegments
        if (segments.size <= 1) {
            hourExpression.append(localHour(segments.firstOrNull()?.offsetMillis ?: 0L, args))
        } else {
            hourExpression.append("CASE")
            segments.drop(1).forEachIndexed { index, segment ->
                hourExpression.append(" WHEN receivedAt < ? THEN ")
                args += segment.startInclusive
                hourExpression.append(localHour(segments[index].offsetMillis, args))
            }
            hourExpression.append(" ELSE ").append(localHour(segments.last().offsetMillis, args))
            hourExpression.append(" END")
        }

        args += plan.startInclusive
        args += plan.endExclusive
        val sql = """
            SELECT hourOfDay, COUNT(*) AS count FROM (
                SELECT $hourExpression AS hourOfDay
                FROM apps
                WHERE isTrashed = 0 AND receivedAt >= ? AND receivedAt < ?
            )
            GROUP BY hourOfDay
            ORDER BY count DESC, hourOfDay ASC
            LIMIT 1
        """.trimIndent()
        return SqlWithArgs(sql, args)
    }

    /** Hour of day for a UTC-offset segment; the double modulo keeps negative timestamps in 0–23. */
    private fun localHour(offsetMillis: Long, args: MutableList<Any>): String {
        args += offsetMillis
        args += MILLIS_PER_HOUR
        return "((((receivedAt + ?) / ?) % 24) + 24) % 24"
    }
}
