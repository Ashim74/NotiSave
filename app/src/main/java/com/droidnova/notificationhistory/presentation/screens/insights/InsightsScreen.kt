package com.droidnova.notificationhistory.presentation.screens.insights

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.data.insights.AppStat
import com.droidnova.notificationhistory.data.insights.BucketUnit
import com.droidnova.notificationhistory.data.insights.ChartBucket
import com.droidnova.notificationhistory.data.insights.InsightsData
import com.droidnova.notificationhistory.data.insights.InsightsRange
import com.droidnova.notificationhistory.presentation.components.AnimatedText
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.PackageAppIcon
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.fitToWidth
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.navigation.navigateToTab
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.presentation.ui.theme.ScreenListContentPadding
import com.droidnova.notificationhistory.presentation.ui.theme.listItemPadding
import com.droidnova.notificationhistory.utils.Analytics

/** Height available to the tallest bar; value labels sit in an extra strip above it. */
private val ChartBarAreaHeight = 96.dp
private val ChartValueLabelHeight = 16.dp

/** Non-zero bars never shrink below this, so small counts stay visible next to a dominant peak. */
private val MinBarHeight = 4.dp

/** Zero buckets draw a faint stub, so the timeline stays continuous instead of looking blank. */
private val EmptyBarHeight = 2.dp

/** Above this many bars there is no room for a label on each; only peak and current are labelled. */
private const val MAX_BUCKETS_WITH_ALL_VALUES = 12

@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Day boundaries can move while the app is in the background; re-plan on every resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        Analytics.log(Analytics.INSIGHTS_OPEN)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = { ScreenTopBar(title = stringResource(R.string.insights_title)) }
    ) { innerPadding ->
        val sectionPadding = Modifier.listItemPadding()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = ScreenListContentPadding
        ) {
            item(key = "range") {
                RangeSelector(
                    selected = state.range,
                    onSelected = viewModel::selectRange,
                    modifier = Modifier.listItemPadding()
                )
            }
            when {
                state.hasError -> item(key = "error") {
                    ErrorState(onRetry = viewModel::refresh)
                }
                state.data == null -> item(key = "loading") {
                    HistoryLoadingState(Modifier.padding(vertical = 48.dp))
                }
                else -> {
                    val data = checkNotNull(state.data)
                    item(key = "summary") {
                        SummaryCards(data, modifier = Modifier.animateItem().listItemPadding())
                    }
                    if (data.totalNotifications == 0) {
                        item(key = "empty") {
                            EmptyState(
                                title = stringResource(R.string.insights_empty),
                                modifier = Modifier.animateItem(),
                                description = stringResource(R.string.insights_empty_description),
                                icon = ImageVector.vectorResource(R.drawable.ic_insights)
                            )
                        }
                    } else {
                        item(key = "chart") {
                            ActivityChartCard(data, modifier = Modifier.animateItem().then(sectionPadding))
                        }
                        item(key = "top-apps") {
                            TopAppsCard(
                                apps = data.topApps,
                                modifier = Modifier.animateItem().then(sectionPadding),
                                onAppClick = { app ->
                                    // Fresh args must win over any saved History tab state.
                                    navController.navigateToTab(
                                        Screens.History.createRoute(app.packageName),
                                        restoreState = false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(
    selected: InsightsRange,
    onSelected: (InsightsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = InsightsRange.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = range.shortLabel(),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = fitToWidth(MaterialTheme.typography.labelLarge.fontSize)
                    )
                }
            )
        }
    }
}

/** One strip, like Home's: total, apps, and the busiest hour. Top apps are listed below. */
@Composable
private fun SummaryCards(data: InsightsData, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryStat(
                value = data.totalNotifications.toString(),
                label = stringResource(R.string.insights_total)
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryStat(
                value = data.activeApps.toString(),
                label = stringResource(R.string.insights_active_apps)
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SummaryStat(
                // "9 AM–10 AM" → "9 AM": the start is enough at a glance.
                value = data.busiestHour?.label?.substringBefore('–')
                    ?: stringResource(R.string.insights_none),
                label = stringResource(R.string.insights_busiest_hour)
            )
        }
    }
}

@Composable
private fun RowScope.SummaryStat(value: String, label: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedText(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = fitToWidth(MaterialTheme.typography.labelMedium.fontSize)
        )
    }
}

@Composable
private fun ActivityChartCard(data: InsightsData, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stringResource(R.string.insights_activity),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            ActivityBarChart(
                buckets = data.buckets,
                labelledBuckets = data.labelledBuckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
            if (data.buckets.any { it.isCurrent }) {
                CurrentPeriodLegend(unit = data.bucketUnit, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

@Composable
private fun ActivityBarChart(
    buckets: List<ChartBucket>,
    labelledBuckets: Set<Int>,
    modifier: Modifier = Modifier
) {
    val maxCount = remember(buckets) { buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1 }
    val peakIndex = remember(buckets) {
        buckets.indices.maxByOrNull { buckets[it].count }?.takeIf { buckets[it].count > 0 }
    }
    val peak = peakIndex?.let { buckets[it] }
    val chartDescription = if (peak == null) {
        stringResource(R.string.insights_chart_description_empty)
    } else {
        stringResource(R.string.insights_chart_description, buckets.size, peak.label, peak.count)
    }
    val showAllValues = buckets.size <= MAX_BUCKETS_WITH_ALL_VALUES
    val currentColor = MaterialTheme.colorScheme.primary
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val emptyColor = MaterialTheme.colorScheme.outlineVariant
    val barShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
    val gap = if (buckets.size > 24) 2.dp else 4.dp

    Column(modifier = modifier.semantics { contentDescription = chartDescription }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ChartBarAreaHeight + ChartValueLabelHeight),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEachIndexed { index, bucket ->
                val barDescription = stringResource(
                    R.string.insights_chart_bar_description,
                    bucket.label,
                    bucket.count
                )
                val targetHeight = if (bucket.count == 0) {
                    EmptyBarHeight
                } else {
                    max(MinBarHeight, ChartBarAreaHeight * (bucket.count.toFloat() / maxCount))
                }
                val barHeight by animateDpAsState(targetHeight, label = "barHeight")
                val showValue = bucket.count > 0 &&
                    (showAllValues || index == peakIndex || bucket.isCurrent)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = barDescription },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (showValue) {
                        // Unbounded width: a 3-digit label may be wider than a thin bar.
                        Text(
                            text = bucket.count.toString(),
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .padding(bottom = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (bucket.isCurrent) FontWeight.Bold else null,
                            color = if (bucket.isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(barShape)
                            .background(
                                when {
                                    bucket.count == 0 -> emptyColor
                                    bucket.isCurrent -> currentColor
                                    else -> barColor
                                }
                            )
                    )
                }
            }
        }
        // Baseline axis under the bars.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            buckets.forEachIndexed { index, bucket ->
                Box(modifier = Modifier.weight(1f)) {
                    if (index in labelledBuckets) {
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (bucket.isCurrent) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPeriodLegend(unit: BucketUnit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = unit.currentLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TopAppsCard(
    apps: List<AppStat>,
    onAppClick: (AppStat) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.insights_top_apps),
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            apps.forEach { app ->
                TopAppRow(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
private fun TopAppRow(app: AppStat, onClick: () -> Unit) {
    val description = stringResource(
        R.string.insights_top_app_description,
        app.appName,
        app.count,
        app.percent
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        PackageAppIcon(app.packageName)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.insights_count_and_percent, app.count, app.percent),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }
            LinearProgressIndicator(
                progress = { app.percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.insights_error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.insights_retry))
        }
    }
}

@Composable
private fun InsightsRange.shortLabel(): String = stringResource(
    when (this) {
        InsightsRange.Today -> R.string.insights_range_today
        InsightsRange.Last7Days -> R.string.insights_range_7_days
        InsightsRange.Last30Days -> R.string.insights_range_30_days
        InsightsRange.AllTime -> R.string.insights_range_all_time
    }
)

@Composable
private fun BucketUnit.currentLabel(): String = stringResource(
    when (this) {
        BucketUnit.Hour -> R.string.insights_current_hour
        BucketUnit.Day -> R.string.insights_current_day
        BucketUnit.Week -> R.string.insights_current_week
        BucketUnit.Month -> R.string.insights_current_month
        BucketUnit.Year -> R.string.insights_current_year
    }
)
