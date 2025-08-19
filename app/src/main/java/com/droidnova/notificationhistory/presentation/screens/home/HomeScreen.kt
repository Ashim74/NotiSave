package com.droidnova.notificationhistory.presentation.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History") },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "menu")
                    }
                    DropdownMenu(
                        expanded = false,
                        onDismissRequest = {}
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
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize()) {
            Row {
                Text(text = "Enable Notifications")
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = true,
                    onCheckedChange = {}
                )
            }
        }
    }
}