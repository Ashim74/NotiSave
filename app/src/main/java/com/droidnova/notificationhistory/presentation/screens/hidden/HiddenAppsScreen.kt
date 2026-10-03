package com.droidnova.notificationhistory.presentation.screens.hidden

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.core.apps.AppInfoCache
import com.droidnova.notificationhistory.core.lock.LocalBiometricGate
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/**
 * Premium: choose apps to keep out of History, Home and Insights, and open their history from
 * here. With hidden apps present, entering asks for the phone's own unlock first.
 */
@Composable
fun HiddenAppsScreen(navController: NavController, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val gate = LocalBiometricGate.current
    val hidden by mainViewModel.hiddenApps.collectAsState()
    val saved by mainViewModel.savedApps.collectAsState()
    var verified by rememberSaveable { mutableStateOf(false) }
    val verifyTitle = stringResource(R.string.hidden_verify_title)
    val verifyDescription = stringResource(R.string.hidden_verify_desc)

    // A result that arrived after Android recreated the activity mid-check.
    val restored = gate?.restoredDeviceCredentialResult?.collectAsState()?.value
    LaunchedEffect(restored) {
        if (restored != null) {
            gate.consumeRestoredDeviceCredentialResult()
            if (restored) verified = true else navController.popBackStack()
        }
    }
    LaunchedEffect(Unit) {
        when {
            verified -> Unit
            mainViewModel.currentHiddenApps().isEmpty() || gate == null || !gate.isDeviceSecure() ->
                verified = true
            else -> gate.confirmDeviceCredential(verifyTitle, verifyDescription) { ok ->
                if (ok) verified = true else navController.popBackStack()
            }
        }
    }

    val labels = remember(saved, hidden) {
        (saved + hidden).associateWith { AppInfoCache.label(context.packageManager, it) }
    }
    val hiddenList = hidden.sortedBy { labels[it]?.lowercase() }
    val visibleList = (saved - hidden).sortedBy { labels[it]?.lowercase() }

    Scaffold(
        topBar = {
            ScreenTopBar(title = stringResource(R.string.settings_hidden_apps), onBack = { navController.popBackStack() })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!verified) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = Dimens.ScreenHorizontal, end = Dimens.ScreenHorizontal, bottom = 16.dp)
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.hidden_intro),
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hiddenList.isNotEmpty()) {
                item(key = "hidden-header") {
                    SectionHeader(stringResource(R.string.hidden_section), Modifier.padding(top = 8.dp, bottom = 6.dp), first = true)
                }
                itemsIndexed(hiddenList, key = { _, pkg -> "hidden:$pkg" }) { index, pkg ->
                    AppRow(
                        packageName = pkg,
                        label = labels[pkg].orEmpty(),
                        index = index,
                        count = hiddenList.size,
                        action = stringResource(R.string.hidden_unhide),
                        onAction = { mainViewModel.setAppHidden(pkg, hidden = false) },
                        onOpen = { navController.navigate(Screens.AppsNotificationListScreen.createRoute(pkg)) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            item(key = "saved-header") {
                SectionHeader(stringResource(R.string.hidden_add_section), Modifier.padding(top = 14.dp, bottom = 6.dp), first = true)
            }
            if (visibleList.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = stringResource(R.string.hidden_empty), icon = Icons.Outlined.VisibilityOff)
                }
            } else {
                itemsIndexed(visibleList, key = { _, pkg -> "saved:$pkg" }) { index, pkg ->
                    AppRow(
                        packageName = pkg,
                        label = labels[pkg].orEmpty(),
                        index = index,
                        count = visibleList.size,
                        action = stringResource(R.string.hidden_hide),
                        onAction = { mainViewModel.setAppHidden(pkg, hidden = true) },
                        onOpen = null,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    packageName: String,
    label: String,
    index: Int,
    count: Int,
    action: String,
    onAction: () -> Unit,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ListRow(
        modifier = modifier.padding(vertical = GroupRowGap / 2),
        shape = groupedShape(index, count),
        title = label.ifBlank { packageName },
        leading = { HistoryAppIcon(packageName = packageName, size = 34.dp) },
        onClick = onOpen,
        trailing = { TextButton(onClick = onAction) { Text(action) } }
    )
}
