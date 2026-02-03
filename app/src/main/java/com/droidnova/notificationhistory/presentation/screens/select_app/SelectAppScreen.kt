package com.droidnova.notificationhistory.presentation.screens.select_app

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.presentation.navigation.Screens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppScreen(
    mainViewModel: MainViewModel,
    navController: NavController
) {
    // Collect your list from VM (UI only)
    val allInstalledApps by mainViewModel.allInstalledApps.collectAsState()
    Log.e("MyTag", "ManageAppNotificationScreen: $allInstalledApps")
    var uiList by    remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    // Access latest list inside lifecycle callbacks
    val latestApps by rememberUpdatedState(allInstalledApps)
    val lifecycleOwner = LocalLifecycleOwner.current

    // NEW: Merge incoming data WITHOUT changing current UI order
    LaunchedEffect(allInstalledApps) {
        if (uiList.isEmpty()) {
            uiList = allInstalledApps
        } else {
            val latestByPkg = allInstalledApps.associateBy { it.packageName }
            val currentOrder = uiList.map { it.packageName }
            val keepOrderUpdated = currentOrder.mapNotNull { latestByPkg[it] }
            val newOnesAtEnd = allInstalledApps.filter { it.packageName !in currentOrder.toSet() }
            uiList = keepOrderUpdated + newOnesAtEnd
        }
    }

    // NEW: Sort ONLY when screen is resumed (allowed apps jump to top then)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                uiList = latestApps.sortedWith(
                    compareByDescending<AppInfo> { it.isAllowed } // NEW
                        .thenBy { it.appName.lowercase() }        // NEW
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredApps = remember(uiList, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) {
            uiList
        } else {
            uiList.filter { it.appName.contains(query, ignoreCase = true) }
        }
    }
    val areAllSelected = filteredApps.isNotEmpty() && filteredApps.all { it.isAllowed }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Choose App", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick =  {navController.popBackStack()}) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // Header text
            item {
                Text(
                    text = "Select the apps for which you want to track notification",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps") },
                    singleLine = true
                )
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select All",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = areAllSelected,
                        onCheckedChange = { checked ->
                            filteredApps.forEach { app ->
                                if (app.isAllowed != checked) {
                                    mainViewModel.addToAllowedApps(app.packageName, checked)
                                }
                            }
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "All Apps",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }//item

            // Loading / empty
            if (uiList.isEmpty()) {
                item {
                    Column {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Searching... Please wait",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            } else {
                items(
                    filteredApps,
                    key = { it.packageName }
                ) { app ->
                    AppCard(
                        apps = app,
                       // modifier = Modifier.animateItem(),
                        onToggle = { checked ->
                            mainViewModel.addToAllowedApps(app.packageName, checked)
                        },
                        onSettingClick = {
                            navController.navigate(Screens.SettingScreen.createRoute(packageName = app.packageName ))
                        }
                    )
                }
            }
        }//lazy
    }//sc
}

@Composable
fun AppCard(
    apps: AppInfo,
    onToggle: (Boolean) -> Unit,
    onSettingClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AppIcon1(packageName = apps.packageName)

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = apps.appName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            // UI-only switch (no logic)
            Switch(
                checked = apps.isAllowed,
                onCheckedChange = { checked ->
                    Log.e("Mantsh2232"," ManageAppNotificationScreen isallowed ${apps.isAllowed}")
                    Log.e("Mantsh2232","ManageAppNotificationScreen checked $checked")
                    onToggle(checked)
                }
            )
        }
        if (apps.isAllowed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onSettingClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings")
                }
            }
        }//if
    }
}

@Composable
private fun AppIcon1(packageName: String) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap() }
            .getOrNull()
    }?.asImageBitmap()

    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(24.dp))
    }
}
