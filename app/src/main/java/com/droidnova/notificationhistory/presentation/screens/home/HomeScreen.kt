package com.droidnova.notificationhistory.presentation.screens.home

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.res.painterResource
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.component.RateUsCard
import com.droidnova.notificationhistory.data_shared.SettingState
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.utils.IntentUtils
import kotlin.text.compareTo

@Composable
fun HomeScreen(viewmodel: MainViewModel, navController: NavController) {
    val state by viewmodel.settingState.collectAsState()
    val context = LocalContext.current

    // initial data
    LaunchedEffect(Unit) {
        viewmodel.getAllInstalledApps(context)
    }

    // events
    LaunchedEffect(viewmodel.events) {
        viewmodel.events.collect { event ->
            when (event) {
                HomeUiEvent.OpenNotificationAccessSettings -> {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }

                HomeUiEvent.DoWorkAfterEnabled -> {
                    navController.navigate(Screens.ManageNotifications.route)
                }
            }
        }
    }

    // re-check on resume
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) viewmodel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Scaffold(
        topBar = { TopBarApp(navigate = { navController.navigate(Screens.AboutScreen.route) }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding).padding(horizontal = 12.dp)
        ) {
            HomeScreenContent(
                modifier = Modifier.fillMaxSize(),
                state = state,
                navController = navController,
                onSwitchChange = {
                    if (it) viewmodel.onEnableClick() else viewmodel.setToggleTracking(
                        false
                    )
                }
            )

            if (state.launchCount >= state.snoozeUntilLaunch && state.showRateUsCard) {                RateUsCard(
                    modifier = Modifier.align(Alignment.BottomCenter),
                onCancelClicked = { viewmodel.snoozeRateUsCard() },
                    onOkClicked = {
                        IntentUtils.rateUs(context)
                        viewmodel.hideRateUsCard()
                    },
                    onRated = {
                        if (it == 5) {
                            IntentUtils.rateUs(context)
                            viewmodel.hideRateUsCard()
                        }
                    },
                    onFeedbackClicked = {
                        IntentUtils.sendFeedback(context)
                        viewmodel.snoozeRateUsCard()
                    }
                )
            }// if
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarApp(navigate: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Notification History") },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "menu")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        navigate()
                        showMenu = false
                    }
                )
//                DropdownMenuItem(
//                    text = { Text("Remove Ads") },
//                    onClick = {}
//                )
//                DropdownMenuItem(
//                    text = { Text("Settings") },
//                    onClick = {}
//                )
            }
        }
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier,
    state: SettingState,
    onSwitchChange: (Boolean) -> Unit,
    navController: NavController
) {
    Column(
        modifier = modifier.padding(8.dp)
    ) {
        EnableNotificationsCard(
            checked = state.userToggleTracking,
            onCheckedChange = onSwitchChange
        )

        Spacer(Modifier.height(16.dp))

        SelectedAppsCard(
            selectedCount = state.selectedAppsCount,
            onSelectAppsClick = { navController.navigate(Screens.ManageNotifications.route) }
        )

        Spacer(Modifier.height(16.dp))

        ViewHistoryCard(
            onClick = { navController.navigate(Screens.History.route) }
        )
    }
}

@Composable
private fun EnableNotificationsCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth() .border(
            width = 0.5.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)

    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notification")
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = "Enable Tracking",
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = { value ->
                    onCheckedChange(value)
                }
            )
        }
    }

}

@Composable
private fun SelectedAppsCard(
    selectedCount: Int,
    onSelectAppsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth() .border(
            width = 0.5.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSelectAppsClick
            ) {
                Text(
                    "Select Apps",
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "$selectedCount Apps Selected",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun ViewHistoryCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth() .border(
            width = 0.5.dp,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.Start) {
            Icon(painter = painterResource(R.drawable.ic_history), contentDescription = "History")
            Text(
                "View Notification History",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 5.dp)
            )
        }
    }
}
