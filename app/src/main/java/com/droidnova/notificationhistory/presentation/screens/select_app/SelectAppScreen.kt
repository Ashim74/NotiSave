package com.droidnova.notificationhistory.presentation.screens.select_app

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AppCardDefaults
import com.droidnova.notificationhistory.presentation.components.ChoicePill
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HeaderButton
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.components.tileColor
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.navigation.Screens
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import kotlinx.coroutines.launch

/** Which apps the list shows; picked with the pills under the top bar. */
private enum class AppFilter(@param:StringRes val labelRes: Int) {
    All(R.string.set_filter_all),
    Saved(R.string.set_filter_saved),
    NotSaved(R.string.set_filter_not_saved)
}

/** Rows past this index skip the arrival animation, so fast scrolling never waits on it. */
private const val APPEAR_LIMIT = 14

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
    var filter by rememberSaveable { mutableStateOf(AppFilter.All) }
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

    val filteredApps = remember(uiList, searchQuery, filter) {
        val query = searchQuery.trim()
        uiList.filter { app ->
            (query.isEmpty() || app.appName.contains(query, ignoreCase = true)) &&
                when (filter) {
                    AppFilter.All -> true
                    AppFilter.Saved -> app.isAllowed
                    AppFilter.NotSaved -> !app.isAllowed
                }
        }
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = tintedCardColor(),
                                unfocusedContainerColor = tintedCardColor(),
                                disabledContainerColor = tintedCardColor(),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            trailingIcon = {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                ScreenTopBar(
                    title = stringResource(R.string.home_manage_apps),
                    subtitle = if (uiList.isNotEmpty()) {
                        stringResource(R.string.select_apps_count, selectedCount, uiList.size)
                    } else {
                        null
                    },
                    onBack = { navController.popBackStack() },
                    actions = {
                        HeaderButton(
                            icon = Icons.Default.Search,
                            contentDescription = stringResource(R.string.content_description_search),
                            onClick = { isSearchActive = true },
                            modifier = Modifier.padding(end = 8.dp)
                        )
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
            contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
        ) {
            if (uiList.isNotEmpty()) {
                item(key = "filters") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontal),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(AppFilter.entries) { option ->
                            ChoicePill(
                                label = stringResource(option.labelRes),
                                selected = option == filter,
                                onClick = { filter = option }
                            )
                        }
                    }
                }
            }
            if (searchQuery.isBlank() && filteredApps.isNotEmpty()) {
                item(key = "select-all") {
                    val shape = MaterialTheme.shapes.medium
                    ListRow(
                        modifier = Modifier
                            .padding(
                                start = Dimens.ScreenHorizontal,
                                end = Dimens.ScreenHorizontal,
                                bottom = 10.dp
                            )
                            .clip(shape)
                            .toggleable(
                                value = areAllSelected,
                                enabled = !isSelectingAll,
                                role = Role.Switch,
                                onValueChange = { checked ->
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
                            ),
                        shape = shape,
                        title = stringResource(R.string.select_all),
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        titleColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leading = {
                            IconBadge(
                                Icons.Outlined.DoneAll,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailing = {
                            Switch(checked = areAllSelected, enabled = !isSelectingAll, onCheckedChange = null)
                        }
                    )
                }
            }

            when {
                uiList.isEmpty() -> item(key = "loading") {
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
                filteredApps.isEmpty() -> item(key = "no-match") {
                    EmptyState(
                        title = stringResource(R.string.set_no_apps_match),
                        icon = Icons.Outlined.SearchOff
                    )
                }
                else -> itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                    SelectableAppRow(
                        modifier = Modifier
                            .animateItem()
                            .then(if (index < APPEAR_LIMIT) Modifier.appearIn(index) else Modifier),
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
 * One line per app: icon in a rounded tile, name, and the switch. Tapping anywhere toggles;
 * saved apps also get a small filter button that pops in, so the list never grows a second line.
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
        modifier = modifier
            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2)
            .clip(shape)
            .toggleable(value = app.isAllowed, role = Role.Switch, onValueChange = onToggle),
        shape = shape,
        title = app.appName,
        highlight = searchQuery,
        containerColor = AppCardDefaults.containerColor(),
        leading = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                    .background(tileColor()),
                contentAlignment = Alignment.Center
            ) {
                HistoryAppIcon(packageName = app.packageName, size = 30.dp)
            }
        },
        trailing = {
            AnimatedVisibility(
                visible = app.isAllowed,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                IconButton(onClick = onFiltersClick) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = filtersLabel,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Switch(checked = app.isAllowed, onCheckedChange = null)
        }
    )
}
