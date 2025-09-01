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
import com.droidnova.notificationhistory.MainViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.droidnova.notificationhistory.presentation.navigation.Screen

private const val MyTAG = "NotifHistory"

@Composable
fun HomeScreen(viewmodel: MainViewModel, navController: NavController) {
    Log.d(MyTAG, "HomeScreen compose. isSwitchOn=$")
    val state by viewmodel.homeUiState.collectAsState()
    val context = LocalContext.current


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
                    navController.navigate(Screen.ManageNotifications.route)
                }
            }
        }
    }

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
                    navigate = { navController.navigate(Screen.AboutScreen.route) })
            })
    { padding ->
        Widgets(padding, state = state, navController = navController, onSwitchChange = { checked ->
            Log.d("toggle", "HomeScreen.onSwitchChange() called with checked=$checked")
            if (checked) {
                Log.e("toggle", "checked=$checked")
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
fun TopBarApp(navigate: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    Log.d("MyTAG", "TopBarApp: Show MEnu")
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
    )//
}

@Composable
fun Widgets(
    padding: PaddingValues,
    state: HomeUiState,
    onSwitchChange: (Boolean) -> Unit,
    navController: NavController
) {
    Log.d("switchTag", "Widgets compose. isSwitchOn=${state.userToggleTracking}")

    Column(
        modifier = Modifier
            .padding(padding)
            .padding(24.dp)
            .fillMaxSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Enable Notifications",
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = state.userToggleTracking,
                onCheckedChange = { checked ->
                    Log.e("switch", "Widgets.onSwitchChange() called with checked=$checked")
                    onSwitchChange(checked)
                }
            )
        }//row
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                navController.navigate(Screen.History.route)
            }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )

        ) {
            Text(
                "View Notification History",
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,      // background
                contentColor = MaterialTheme.colorScheme.onSurface       // text
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.ManageNotifications.route) },
                    border = BorderStroke(2.dp, color = Color.Black)
                ) {
                    Text(
                        "Select Apps", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.weight(1f))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Selected Apps",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${state.selectedAppsCount}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }//col
            }//row
        }
    }
}

