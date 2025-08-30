package com.droidnova.notificationhistory.presentation.screens.home

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.notificationhistory.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import android.provider.Settings
import androidx.compose.foundation.layout.height
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.droidnova.notificationhistory.presentation.navigation.Screen

private const val MyTAG = "NotifHistory"
@Composable
fun HomeScreen(viewmodel: MainViewModel,navController: NavController) {
    Log.d(MyTAG, "HomeScreen compose. isSwitchOn=$")
    var showMenu by remember { mutableStateOf(false) }
    val state by viewmodel.homeUiState.collectAsState()
    val context = LocalContext.current

/////

    LaunchedEffect(Unit) {
        viewmodel.getAllInstalledApps(context)
    }
    // Collect one-shot events
    LaunchedEffect(viewmodel.events) {
        viewmodel.events.collect { event ->
            Log.d("Mantsha00", "called with event=$event")
            when (event) {
                HomeUiEvent.OpenNotificationAccessSettings -> {
                    Log.d("Mantsha1", "listenre setting called")
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                HomeUiEvent.DoWorkAfterEnabled -> {
                    Log.d("Mantsha2", "DoWorkAfterEnabled called")
                    // TODO: navigate to history or start your tracking work
                    // e.g., navController.navigate("history")
                }
            }
        }
    }//launchedEffect

    // Re-check when returning from Settings
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) viewmodel.onResume()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }//Disposable effect

///////UI
    Scaffold(
        topBar =
            {
                TopBarApp(
                    expanded = showMenu, onExpandedChange =
                        { expand ->
                            showMenu = expand
                        },)
            })
    { padding ->
        Widgets(padding, state = state,  navController = navController, onSwitchChange = { checked ->
            Log.d(MyTAG, "HomeScreen.onSwitchChange() called with checked=$checked")
            if (checked) {
                viewmodel.onEnableClick()
            } else {
                viewmodel.setToggleTracking(false)
            }
            Log.d(MyTAG, "HomeScreen state updated. isSwitchOn=$state.userWantsTracking,")

        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarApp(expanded: Boolean, onExpandedChange: (Boolean) -> Unit) {
    Log.d("MyTAG", "TopBarApp: Show MEnu")
    TopAppBar(
        title = { Text("Notification History") },
        actions = {
            IconButton(onClick = { onExpandedChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "menu")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {}
                )
                DropdownMenuItem(
                    text = { Text("Remove Ads") },
                    onClick = {}
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {}
                )
            }
        }
    )//
}

@Composable
fun Widgets(
    padding: PaddingValues,
    state: HomeUiState,
    onSwitchChange: (Boolean) -> Unit,
    navController: NavController
) {
    Log.d(MyTAG, "Widgets compose. isSwitchOn=${state.userToggleTracking}")

    Column(
        modifier = Modifier
            .padding(padding)
            .padding(24.dp)
            .fillMaxSize()
    ) {
        Row {
            Text(text = "Enable Notifications")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = state.userToggleTracking,
                onCheckedChange = { checked ->
                    onSwitchChange(checked)
                }
            )
        }
            Button(
                onClick = { navController.navigate(Screen.History.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Notification History")
            }
        Spacer(Modifier.height(12.dp))
        Button(
                onClick = { navController.navigate(Screen.ManageNotifications.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Notification")
            }
    }
}