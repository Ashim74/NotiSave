package com.droidnova.notificationhistory.presentation.screens.setting

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.MainViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navController: NavHostController, packageName: String, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val appLabel = remember(packageName) {
        runCatching {
            val pm = context.packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        }.getOrDefault(packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = appLabel) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }

    ) {innerPadding->
        SettingScreenContent(
            innerPaddingValues = innerPadding,
            packageName = packageName,
            mainViewModel = mainViewModel
        )
    }
}

@Composable
fun SettingScreenContent(innerPaddingValues: PaddingValues, packageName: String, mainViewModel: MainViewModel) {
    val filtersMap by mainViewModel.titleFilters.collectAsState()
    val filters = filtersMap[packageName] ?: emptySet()

    var input by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(innerPaddingValues)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        WarningCard(
            message = stringResource(id = R.string.warning_only_matching_titles)
        )
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.label_add_title_filter),
                fontWeight = FontWeight.Bold
            ) }
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = {
                if (input.isNotBlank()) {
                    mainViewModel.addTitleFilter(packageName, input)
                    input = ""
                }
            })
            { Text(stringResource(id = R.string.btn_add)) }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = { mainViewModel.clearTitleFilters(packageName) }) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(id = R.string.btn_clear_all_filters))
            }
        }

        Divider()

        Text(
            text = stringResource(id = R.string.label_current_filters),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W900
        )
        Box(
            modifier = Modifier
                .weight(1f) // Take up all remaining space for scrollable content
                .fillMaxWidth()
        ) {
            if (filters.isEmpty()) {
                Text(text = stringResource(id = R.string.label_no_filters))
            } else {
                LazyColumn {
                    items(filters.toList().asReversed()) { title ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                mainViewModel.removeTitleFilter(
                                    packageName,
                                    title
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(id = R.string.remove)
                                )
                            }
                        }
                    }
                }//lazy
            }
        } // <-- Box
    }// <-- Column
}

@Composable
private fun WarningCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0),
            contentColor = Color(0xFF7A4E00)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                modifier = Modifier
                    .padding(start = 8.dp)
            )
        }
    }
}
