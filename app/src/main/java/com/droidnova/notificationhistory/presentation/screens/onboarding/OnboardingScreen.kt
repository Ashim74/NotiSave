package com.droidnova.notificationhistory.presentation.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.droidnova.notificationhistory.presentation.components.ListGroup
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.groupedShape
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import com.droidnova.notificationhistory.presentation.components.openNotificationAccessSettings
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

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDots(current = step, modifier = Modifier.weight(1f))
                if (step < STEP_APPS) {
                    TextButton(onClick = { step++ }) { Text(stringResource(R.string.onboarding_skip)) }
                }
            }

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    stringResource(
                        when (step) {
                            STEP_INTRO -> R.string.onboarding_get_started
                            STEP_ACCESS -> if (hasPermission) R.string.onboarding_continue
                            else R.string.onboarding_allow_access
                            else -> R.string.onboarding_done
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun StepDots(current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEP_COUNT) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == current) 20.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
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
        Image(
            painter = painterResource(R.drawable.ic_notification_history),
            contentDescription = null,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_intro_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        // Three icon lines instead of a paragraph.
        ListGroup {
            row { shape ->
                ListRow(
                    shape = shape,
                    title = stringResource(R.string.onboarding_feature_saves),
                    leading = { IconBadge(Icons.Default.Notifications) }
                )
            }
            row { shape ->
                ListRow(
                    shape = shape,
                    title = stringResource(R.string.onboarding_feature_deleted),
                    leading = { IconBadge(Icons.Default.Email) }
                )
            }
            row { shape ->
                ListRow(
                    shape = shape,
                    title = stringResource(R.string.onboarding_feature_private),
                    leading = { IconBadge(Icons.Default.Lock) }
                )
            }
        }
    }
}

@Composable
private fun AccessStep(hasPermission: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Crossfade(targetState = hasPermission, label = "accessIcon") { granted ->
            IconBadge(
                icon = if (granted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                modifier = Modifier.size(88.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_access_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        // The big button below does the asking; this line only says why.
        AnimatedText(
            text = stringResource(
                if (hasPermission) R.string.onboarding_access_granted_hint
                else R.string.onboarding_access_description
            ),
            style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
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
        Text(
            text = stringResource(R.string.onboarding_apps_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.onboarding_apps_description),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                    ListRow(
                        modifier = Modifier.padding(vertical = GroupRowGap / 2),
                        shape = groupedShape(index, apps.size),
                        title = app.appName,
                        leading = { HistoryAppIcon(packageName = app.packageName, size = 32.dp) },
                        onClick = { mainViewModel.addToAllowedApps(app.packageName, !app.isAllowed) },
                        trailing = { Switch(checked = app.isAllowed, onCheckedChange = null) }
                    )
                }
            }
        }
    }
}
