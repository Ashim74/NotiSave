package com.droidnova.notificationhistory.presentation.screens.applist

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.data.model.NotificationModel
import com.droidnova.notificationhistory.presentation.screens.history.ItemHistoryCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    mainViewModel: MainViewModel,
    packageName: String,
    navController: NavController
) {
    var notifications by remember { mutableStateOf<List<NotificationModel>>(emptyList()) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    LaunchedEffect(packageName) {
        notifications = mainViewModel.getNotificationsForPackage(packageName)
            .sortedByDescending {
                runCatching { LocalDateTime.parse(it.receivedAt, formatter) }.getOrNull()
                    ?: LocalDateTime.MIN
            }
    }

    val title = notifications.firstOrNull()?.appName?.ifBlank { packageName } ?: packageName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(notifications) { item ->
                ItemHistoryCard(item, onClick = {})
            }
        }
    }
}

