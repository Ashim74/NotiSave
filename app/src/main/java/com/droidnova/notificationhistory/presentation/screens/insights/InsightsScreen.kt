package com.droidnova.notificationhistory.presentation.screens.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import com.droidnova.notificationhistory.presentation.components.HistoryEmptyState
import com.droidnova.notificationhistory.presentation.components.PackageAppIcon
import com.droidnova.notificationhistory.presentation.navigation.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel,
    navController: NavController
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Day boundaries can move while the app is in the background; re-plan on every resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.insights_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "range") {
                RangeSelector(selected = state.range, onSelected = viewModel::selectRange)
            }
            when {
                state.hasError -> item(key = "error") {
                    ErrorState(onRetry = viewModel::refresh)
                }
                state.data == null -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                else -> {
                    val data = checkNotNull(state.data)
                    item(key = "summary") { SummaryCards(data) }
                    if (data.totalNotifications == 0) {
                        item(key = "empty") {
                            HistoryEmptyState(
                                message = stringResource(R.string.insights_empty),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        item(key = "chart") { ActivityChartCard(data) }
                        item(key = "top-apps") {
                            TopAppsCard(
                                apps = data.topApps,
                                onAppClick = { app ->
                                    navController.navigate(
                                        Screens.History.createRoute(app.packageName)
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
private fun RangeSelector(selected: InsightsRange, onSelected: (InsightsRange) -> Unit) {
    val options = InsightsRange.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, range ->
            SegmentedButton(
                selected = selected == range,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = {
                    Text(
                        text = range.shortLabel(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun SummaryCards(data: InsightsData) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.insights_total),
                value = data.totalNotifications.toString()
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.insights_active_apps),
                value = data.activeApps.toString()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.insights_most_active_app),
                value = data.mostActiveApp?.appName ?: stringResource(R.string.insights_none),
                supporting = data.mostActiveApp?.let {
                    stringResource(R.string.insights_notification_count, it.count)
                },
                leading = data.mostActiveApp?.let { { PackageAppIcon(it.packageName) } }
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.insights_busiest_hour),
                value = data.busiestHour?.label ?: stringResource(R.string.insights_none),
                supporting = data.busiestHour?.let {
                    stringResource(R.string.insights_notification_count, it.count)
                }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    label: String,
    value: String,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .heightIn(min = 88.dp)
            .semantics(mergeDescendants = true) {},
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (supporting != null) {
                Text(
                    text = supporting,
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActivityChartCard(data: InsightsData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.insights_activity),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = data.bucketUnit.description(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ActivityBarChart(
                buckets = data.buckets,
                labelledBuckets = data.labelledBuckets,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
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
    val peak = remember(buckets) { buckets.maxByOrNull { it.count } }
    val chartDescription = if (peak == null || peak.count == 0) {
        stringResource(R.string.insights_chart_description_empty)
    } else {
        stringResource(R.string.insights_chart_description, buckets.size, peak.label, peak.count)
    }
    val barColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val barShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
    val gap = if (buckets.size > 24) 1.dp else 3.dp

    Column(modifier = modifier.semantics { contentDescription = chartDescription }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEach { bucket ->
                val fraction = bucket.count.toFloat() / maxCount
                val barDescription = stringResource(
                    R.string.insights_chart_bar_description,
                    bucket.label,
                    bucket.count
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(barShape)
                        .background(trackColor)
                        .semantics { contentDescription = barDescription },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (bucket.count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Keep tiny values visible against a dominant bar.
                                .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                .clip(barShape)
                                .background(barColor)
                        )
                    }
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun TopAppsCard(apps: List<AppStat>, onAppClick: (AppStat) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.insights_top_apps),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
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
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
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
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
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
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
private fun BucketUnit.description(): String = stringResource(
    when (this) {
        BucketUnit.Hour -> R.string.insights_bucket_hourly
        BucketUnit.Day -> R.string.insights_bucket_daily
        BucketUnit.Week -> R.string.insights_bucket_weekly
        BucketUnit.Month -> R.string.insights_bucket_monthly
        BucketUnit.Year -> R.string.insights_bucket_yearly
    }
)
