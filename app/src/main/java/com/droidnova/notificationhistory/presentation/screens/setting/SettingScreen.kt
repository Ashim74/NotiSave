package com.droidnova.notificationhistory.presentation.screens.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** Per-app title filters: one field to add, a list to remove, "clear all" in the top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navController: NavHostController, packageName: String, mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val appLabel = remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }
    val filtersMap by mainViewModel.titleFilters.collectAsState()
    val filters = filtersMap[packageName].orEmpty().toList().asReversed()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HistoryAppIcon(packageName = packageName, size = 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(appLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (filters.isNotEmpty()) {
                        IconButton(onClick = { mainViewModel.clearTitleFilters(packageName) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.btn_clear_all_filters)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            item(key = "add") {
                AddFilterField(
                    onAdd = { mainViewModel.addTitleFilter(packageName, it) },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontal)
                )
            }
            item(key = "hint") {
                Row(
                    modifier = Modifier.padding(
                        start = Dimens.ScreenHorizontal + 4.dp,
                        end = Dimens.ScreenHorizontal,
                        top = 6.dp,
                        bottom = 16.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.warning_only_matching_titles),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (filters.isEmpty()) {
                item(key = "empty") {
                    EmptyState(title = stringResource(R.string.label_no_filters))
                }
            } else {
                itemsIndexed(filters, key = { _, title -> title }) { index, title ->
                    ListRow(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
                        shape = groupedShape(index, filters.size),
                        title = title,
                        trailing = {
                            IconButton(onClick = { mainViewModel.removeTitleFilter(packageName, title) }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove))
                            }
                        }
                    )
                }
            }
        }
    }
}

/** A single field with its own "add" button; Enter on the keyboard adds too. */
@Composable
private fun AddFilterField(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    val submit = {
        if (input.isNotBlank()) {
            onAdd(input.trim())
            input = ""
        }
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        label = { Text(stringResource(R.string.label_add_title_filter)) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            IconButton(onClick = submit, enabled = input.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.btn_add))
            }
        }
    )
}
