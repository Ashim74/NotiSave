package com.droidnova.notificationhistory.presentation.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AnimatedText
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.floating
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
import com.droidnova.notificationhistory.presentation.components.pulsing
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.utils.Analytics

private const val STEP_INTRO = 0
private const val STEP_ACCESS = 1
private const val STEP_APPS = 2
private const val STEP_COUNT = 3

/**
 * First-run flow: what the app does → grant notification access → pick apps. Every step can be
 * skipped; finishing (or skipping at the end) marks onboarding complete via [onFinished].
 */
@Composable
fun OnboardingScreen(mainViewModel: MainViewModel, onFinished: () -> Unit) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(STEP_INTRO) }
    val hasPermission by mainViewModel.hasNotificationAccess.collectAsState()

    // The user comes back from system settings mid-flow; re-check and move on automatically.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mainViewModel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(hasPermission, step) {
        if (hasPermission && step == STEP_ACCESS) step = STEP_APPS
    }

    fun finish() {
        Analytics.log(context, Analytics.ONBOARDING_COMPLETE)
        onFinished()
    }

    // The only notification we ever post is "recording stopped"; ask once, at the very end.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { finish() }

    fun finishRequestingAlerts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            finish()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDots(current = step, modifier = Modifier.weight(1f))
                if (step < STEP_APPS) {
                    TextButton(onClick = { step++ }) { Text(stringResource(R.string.onboarding_skip)) }
                }
            }

            // Steps slide in from the side they come from, so moving on feels like turning a page.
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(220)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(160)))
                },
                label = "onboarding-step"
            ) { current ->
                when (current) {
                    STEP_INTRO -> IntroStep()
                    STEP_ACCESS -> AccessStep(hasPermission = hasPermission)
                    else -> AppsStep(mainViewModel = mainViewModel)
                }
            }

            Button(
                onClick = {
                    when (step) {
                        STEP_INTRO -> step = STEP_ACCESS
                        STEP_ACCESS -> if (hasPermission) step = STEP_APPS else {
                            mainViewModel.onPermissionSettingsOpened()
                            openNotificationAccessSettings(context)
                        }
                        else -> finishRequestingAlerts()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(52.dp)
            ) {
                AnimatedText(
                    text = stringResource(
                        when (step) {
                            STEP_INTRO -> R.string.onboarding_get_started
                            STEP_ACCESS -> if (hasPermission) R.string.onboarding_continue
                            else R.string.onboarding_allow_access
                            else -> R.string.onboarding_done
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/** Progress dots; the current one stretches into a pill. */
@Composable
private fun StepDots(current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEP_COUNT) { index ->
            val width by animateDpAsState(
                if (index == current) 22.dp else 8.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "stepDotWidth"
            )
            val color by animateColorAsState(
                if (index <= current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "stepDotColor"
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun IntroStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The app icon floats over a soft halo.
        Box(contentAlignment = Alignment.Center, modifier = Modifier.floating()) {
            Box(
                Modifier
                    .size(124.dp)
                    .pulsing(minScale = 0.9f, periodMillis = 2400)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            )
            Image(
                painter = painterResource(R.drawable.ic_notification_history),
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_intro_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        // Three icon lines instead of a paragraph.
        Column(verticalArrangement = Arrangement.spacedBy(GroupRowGap)) {
            FeatureRow(0, Icons.Default.Notifications, AccentColors.Blue, stringResource(R.string.onboarding_feature_saves))
            FeatureRow(1, Icons.Default.Delete, AccentColors.Orange, stringResource(R.string.onboarding_feature_deleted))
            FeatureRow(2, Icons.Default.Lock, AccentColors.Green, stringResource(R.string.onboarding_feature_private))
        }
    }
}

@Composable
private fun FeatureRow(index: Int, icon: ImageVector, accent: Color, title: String) {
    ListRow(
        modifier = Modifier.appearIn(index + 1, stepMillis = 80),
        shape = groupedShape(index, 3),
        title = title,
        leading = { IconBadge(icon, accent = accent) }
    )
}

@Composable
private fun AccessStep(hasPermission: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.floating()) {
            // A breathing ring while access is still needed; it settles once granted.
            Box(
                Modifier
                    .size(124.dp)
                    .then(if (hasPermission) Modifier else Modifier.pulsing(minScale = 0.88f))
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
            )
            Crossfade(targetState = hasPermission, label = "accessIcon") { granted ->
                IconBadge(
                    icon = if (granted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    containerColor = tintedCardColor(),
                    size = 96.dp
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_access_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        // The big button below does the asking; this line only says why.
        AnimatedText(
            text = stringResource(
                if (hasPermission) R.string.onboarding_access_granted_hint
                else R.string.onboarding_access_description
            ),
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3
        )
    }
}

@Composable
private fun AppsStep(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val apps by mainViewModel.allInstalledApps.collectAsState()
    var preselected by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { mainViewModel.getAllInstalledApps(context) }
    LaunchedEffect(apps) {
        if (!preselected && apps.isNotEmpty()) {
            preselected = true
            mainViewModel.preselectCommonApps(apps)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            IconBadge(
                ImageVector.vectorResource(R.drawable.ic_apps),
                containerColor = tintedCardColor(),
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.onboarding_apps_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.onboarding_apps_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    ListRow(
                        modifier = Modifier
                            .padding(vertical = GroupRowGap / 2)
                            .appearIn(index.coerceAtMost(12)),
                        shape = groupedShape(index, apps.size),
                        title = app.appName,
                        leading = {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
                                contentAlignment = Alignment.Center
                            ) {
                                HistoryAppIcon(packageName = app.packageName, size = 28.dp)
                            }
                        },
                        onClick = { mainViewModel.addToAllowedApps(app.packageName, !app.isAllowed) },
                        trailing = { Switch(checked = app.isAllowed, onCheckedChange = null) }
                    )
                }
            }
        }
    }
}
