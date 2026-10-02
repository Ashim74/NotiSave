package com.droidnova.notificationhistory.presentation.screens.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
import com.droidnova.notificationhistory.presentation.components.ChoicePill
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.HistoryLoadingState
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.fitToWidth
import com.droidnova.notificationhistory.presentation.components.pressScale
import com.droidnova.notificationhistory.presentation.components.tileColor
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.navigation.navigateToTab
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.presentation.ui.theme.ScreenListContentPadding
import com.droidnova.notificationhistory.presentation.ui.theme.listItemPadding
import com.droidnova.notificationhistory.utils.Analytics

/** Height available to the tallest bar; value labels sit in an extra strip above it. */
private val ChartBarAreaHeight = 88.dp
private val ChartValueLabelHeight = 16.dp

/** Non-zero bars never shrink below this, so small counts stay visible next to a dominant peak. */
private val MinBarHeight = 4.dp

/** Zero buckets draw a faint stub, so the timeline stays continuous instead of looking blank. */
private val EmptyBarHeight = 3.dp

/** Above this many bars there is no room for a label on each; only peak and current are labelled. */
private const val MAX_BUCKETS_WITH_ALL_VALUES = 12

/** How long bars and progress tracks take to grow in when a range is shown. */
private const val GROW_MS = 650

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
        topBar = { ScreenTopBar(title = stringResource(R.string.insights_title)) },
        containerColor = MaterialTheme.colorScheme.background
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
                    EmptyState(
                        title = stringResource(R.string.insights_error),
                        icon = Icons.Filled.Warning,
                        actionLabel = stringResource(R.string.insights_retry),
                        onAction = viewModel::refresh
                    )
                }
                state.data == null -> item(key = "loading") {
                    HistoryLoadingState(Modifier.padding(vertical = 48.dp))
                }
                else -> {
                    val data = checkNotNull(state.data)
                    item(key = "summary") {
                        SummaryTiles(data, modifier = Modifier.animateItem().listItemPadding())
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
                            ActivityChartCard(
                                data,
                                modifier = Modifier
                                    .animateItem()
                                    .then(sectionPadding)
                                    .appearIn(3)
                            )
                        }
                        item(key = "top-apps") {
                            TopAppsCard(
                                apps = data.topApps,
                                modifier = Modifier
                                    .animateItem()
                                    .then(sectionPadding)
                                    .appearIn(4),
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

/** The ranges as pills in one line; the selected one is tinted and outlined. */
@Composable
private fun RangeSelector(
    selected: InsightsRange,
    onSelected: (InsightsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsightsRange.entries.forEach { range ->
            ChoicePill(
                label = range.shortLabel(),
                selected = selected == range,
                onClick = { onSelected(range) }
            )
        }
    }
}

/** Three compact tiles: total, apps, and the busiest hour, each with its own colored badge. */
@Composable
private fun SummaryTiles(data: InsightsData, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile(
            icon = Icons.Filled.Notifications,
            accent = AccentColors.Blue,
            value = data.totalNotifications.toString(),
            label = stringResource(R.string.insights_total),
            index = 0
        )
        StatTile(
            icon = ImageVector.vectorResource(R.drawable.ic_apps),
            accent = AccentColors.Purple,
            value = data.activeApps.toString(),
            label = stringResource(R.string.insights_active_apps),
            index = 1
        )
        StatTile(
            icon = Icons.Outlined.Schedule,
            accent = AccentColors.Orange,
            // "9 AM–10 AM" → "9 AM": the start is enough at a glance.
            value = data.busiestHour?.label?.substringBefore('–')
                ?: stringResource(R.string.insights_none),
            label = stringResource(R.string.insights_busiest_hour),
            index = 2
        )
    }
}

@Composable
private fun RowScope.StatTile(
    icon: ImageVector,
    accent: Color,
    value: String,
    label: String,
    index: Int
) {
    AppCard(
        modifier = Modifier
            .weight(1f)
            .appearIn(index)
            .semantics(mergeDescendants = true) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconBadge(icon = icon, accent = accent, size = 34.dp)
            AnimatedText(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = fitToWidth(MaterialTheme.typography.labelSmall.fontSize)
            )
        }
    }
}

/** Card title line: colored badge, bold title, and an optional trailing slot. */
@Composable
private fun CardHeader(
    icon: ImageVector,
    accent: Color,
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, accent = accent, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing()
    }
}

@Composable
private fun ActivityChartCard(data: InsightsData, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            CardHeader(
                icon = ImageVector.vectorResource(R.drawable.ic_insights),
                accent = AccentColors.Teal,
                title = stringResource(R.string.insights_activity),
                trailing = {
                    if (data.buckets.any { it.isCurrent }) CurrentPeriodLegend(unit = data.bucketUnit)
                }
            )
            ActivityBarChart(
                buckets = data.buckets,
                labelledBuckets = data.labelledBuckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            )
        }
    }
}

/** 0 → 1 once when the composable first appears (and again for a new [key]), for grow-in motion. */
@Composable
private fun rememberGrowProgress(key: Any?): Float {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(1f, tween(GROW_MS, easing = FastOutSlowInEasing))
    }
    return progress.value
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
    val colors = MaterialTheme.colorScheme
    val currentBrush = Brush.verticalGradient(listOf(colors.tertiary, colors.primary))
    val barBrush = Brush.verticalGradient(
        listOf(colors.primary.copy(alpha = 0.55f), colors.primary.copy(alpha = 0.3f))
    )
    val emptyColor = colors.outlineVariant.copy(alpha = 0.6f)
    val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
    val gap = if (buckets.size > 24) 2.dp else 5.dp
    // Bars rise from the baseline whenever the bucket layout (range) changes.
    val grow = rememberGrowProgress(buckets.size)

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
                            color = if (bucket.isCurrent) colors.primary else colors.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    val barModifier = Modifier
                        .fillMaxWidth()
                        .height(max(EmptyBarHeight, barHeight * grow))
                        .clip(barShape)
                    Box(
                        modifier = when {
                            bucket.count == 0 -> barModifier.background(emptyColor)
                            bucket.isCurrent -> barModifier.background(currentBrush)
                            else -> barModifier.background(barBrush)
                        }
                    )
                }
            }
        }
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
                            color = if (bucket.isCurrent) colors.primary else colors.onSurfaceVariant,
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

/** A small pill naming the highlighted (current) bar. */
@Composable
private fun CurrentPeriodLegend(unit: BucketUnit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = unit.currentLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
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
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            CardHeader(
                icon = Icons.Filled.Star,
                accent = AccentColors.Amber,
                title = stringResource(R.string.insights_top_apps),
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Spacer(Modifier.height(4.dp))
            apps.forEachIndexed { index, app ->
                TopAppRow(app = app, index = index, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
private fun TopAppRow(app: AppStat, index: Int, onClick: () -> Unit) {
    val description = stringResource(
        R.string.insights_top_app_description,
        app.appName,
        app.count,
        app.percent
    )
    val interaction = remember { MutableInteractionSource() }
    val grow = rememberGrowProgress(app.packageName)
    val fraction by animateFloatAsState((app.percent / 100f).coerceIn(0f, 1f), label = "appShare")
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .pressScale(interaction, 0.98f)
            .clip(RoundedCornerShape(Dimens.TileCornerRadius))
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                .background(tileColor()),
            contentAlignment = Alignment.Center
        ) {
            HistoryAppIcon(packageName = app.packageName, size = 26.dp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.appName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.insights_count_and_percent, app.count, app.percent),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (index == 0) FontWeight.Bold else null,
                    color = if (index == 0) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
            // Rounded share track that fills in from the left.
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.primary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction * grow)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(colors.primary, colors.tertiary)))
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.padding(start = 4.dp),
            tint = colors.onSurfaceVariant
        )
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
