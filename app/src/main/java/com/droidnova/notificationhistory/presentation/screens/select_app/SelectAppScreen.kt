package com.droidnova.notificationhistory.presentation.screens.select_app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectAppScreen(
    mainViewModel: MainViewModel,
    navController: NavController
) {
    val allInstalledApps by mainViewModel.allInstalledApps.collectAsState()
    var uiList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var isSelectingAll by remember { mutableStateOf(false) }
    var bulkActionIsSelect by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val latestApps by rememberUpdatedState(allInstalledApps)
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(allInstalledApps) {
        if (uiList.isEmpty()) {
            uiList = allInstalledApps
        } else {
            // Keep rows where they are while the user toggles; new apps go to the end.
            val latestByPkg = allInstalledApps.associateBy { it.packageName }
            val currentOrder = uiList.map { it.packageName }
            val keepOrderUpdated = currentOrder.mapNotNull { latestByPkg[it] }
            val newOnesAtEnd = allInstalledApps.filter { it.packageName !in currentOrder.toSet() }
            uiList = keepOrderUpdated + newOnesAtEnd
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                uiList = latestApps.sortedWith(
                    compareByDescending<AppInfo> { it.isAllowed }
                        .thenBy { it.appName.lowercase() }
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val filteredApps = remember(uiList, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) uiList else uiList.filter { it.appName.contains(query, ignoreCase = true) }
    }
    val selectedCount = uiList.count { it.isAllowed }
    val areAllSelected = filteredApps.isNotEmpty() && filteredApps.all { it.isAllowed }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_apps_hint)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearchActive = false
                            searchQuery = ""
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_description_close_search)
                            )
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.content_description_clear_search)
                                )
                            }
                        }
                    }
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.home_manage_apps)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.content_description_search)
                            )
                        }
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(bottom = 4.dp)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
        ) {
            if (searchQuery.isBlank() && uiList.isNotEmpty()) {
                item(key = "select-all") {
                    ListRow(
                        modifier = Modifier.padding(
                            start = Dimens.ScreenHorizontal,
                            end = Dimens.ScreenHorizontal,
                            bottom = 12.dp
                        ),
                        title = stringResource(R.string.select_all),
                        value = stringResource(R.string.select_apps_count, selectedCount, uiList.size),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        valueColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        trailing = {
                            Switch(
                                checked = areAllSelected,
                                enabled = !isSelectingAll,
                                onCheckedChange = { checked ->
                                    if (filteredApps.isNotEmpty()) {
                                        coroutineScope.launch {
                                            bulkActionIsSelect = checked
                                            isSelectingAll = true
                                            mainViewModel.setAllowedAppsForPackages(
                                                filteredApps.map { it.packageName },
                                                checked
                                            )
                                            isSelectingAll = false
                                        }
                                    }
                                }
                            )
                        }
                    )
                }
            }

            if (uiList.isEmpty()) {
                item(key = "loading") {
                    Column(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_apps),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                    SelectableAppRow(
                        modifier = Modifier.animateItem(),
                        app = app,
                        searchQuery = searchQuery,
                        shape = groupedShape(index, filteredApps.size),
                        onToggle = { checked -> mainViewModel.addToAllowedApps(app.packageName, checked) },
                        onFiltersClick = {
                            navController.navigate(Screens.SettingScreen.createRoute(packageName = app.packageName))
                        }
                    )
                }
            }
        }
    }

    if (isSelectingAll) {
        Dialog(onDismissRequest = {}) {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.padding(start = 16.dp))
                    Text(
                        text = stringResource(
                            if (bulkActionIsSelect) R.string.select_apps_bulk_selecting
                            else R.string.select_apps_bulk_removing
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * One line per app: icon, name, and the switch. Tapping anywhere toggles; saved apps also get a
 * small filter button, so the list doesn't grow a second row per app.
 */
@Composable
private fun SelectableAppRow(
    app: AppInfo,
    searchQuery: String,
    shape: Shape,
    onToggle: (Boolean) -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtersLabel = stringResource(R.string.select_apps_filters, app.appName)
    ListRow(
        modifier = modifier.padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
        shape = shape,
        title = app.appName,
        highlight = searchQuery,
        containerColor = AppCardDefaults.containerColor(),
        leading = { HistoryAppIcon(packageName = app.packageName, size = 32.dp) },
        onClick = { onToggle(!app.isAllowed) },
        showChevron = false,
        trailing = {
            if (app.isAllowed) {
                IconButton(onClick = onFiltersClick) {
                    Icon(Icons.Default.Settings, contentDescription = filtersLabel)
                }
            }
            Switch(checked = app.isAllowed, onCheckedChange = onToggle)
        }
    )
}
