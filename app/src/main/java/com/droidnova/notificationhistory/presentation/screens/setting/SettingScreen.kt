package com.droidnova.notificationhistory.presentation.screens.setting

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
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
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** Per-app title filters: the app as a header card, one field to add, a list to remove. */
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
            ScreenTopBar(
                title = appLabel,
                onBack = { navController.popBackStack() },
                actions = {
                    if (filters.isNotEmpty()) {
                        HeaderButton(
                            icon = Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.btn_clear_all_filters),
                            onClick = { mainViewModel.clearTitleFilters(packageName) },
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 2.dp, bottom = 16.dp)
        ) {
            item(key = "app") {
                AppHeaderRow(
                    packageName = packageName,
                    filtering = filters.isNotEmpty(),
                    modifier = Modifier
                        .padding(horizontal = Dimens.ScreenHorizontal)
                        .appearIn(0)
                )
            }
            item(key = "add") {
                AddFilterField(
                    onAdd = { mainViewModel.addTitleFilter(packageName, it) },
                    modifier = Modifier
                        .padding(start = Dimens.ScreenHorizontal, end = Dimens.ScreenHorizontal, top = 10.dp, bottom = 12.dp)
                        .appearIn(1)
                )
            }
            if (filters.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.label_no_filters),
                        icon = Icons.Outlined.FilterAltOff
                    )
                }
            } else {
                itemsIndexed(filters, key = { _, title -> title }) { index, title ->
                    ListRow(
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
                        shape = groupedShape(index, filters.size),
                        title = title,
                        leading = { IconBadge(Icons.Outlined.Title, accent = AccentColors.Teal, size = 34.dp) },
                        trailing = {
                            IconButton(onClick = { mainViewModel.removeTitleFilter(packageName, title) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.remove),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * The app's icon in a rounded tile and what adding a filter does; the badge turns
 * teal while filters are on.
 */
@Composable
private fun AppHeaderRow(packageName: String, filtering: Boolean, modifier: Modifier = Modifier) {
    val accent by animateColorAsState(
        if (filtering) AccentColors.Teal else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "filterAccent"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(tintedCardColor())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(Dimens.TileCornerRadius))
                .background(tileColor()),
            contentAlignment = Alignment.Center
        ) {
            HistoryAppIcon(packageName = packageName, size = 36.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.warning_only_matching_titles),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (filtering) Icons.Outlined.FilterAlt else Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A single rounded field with its own "add" button; Enter on the keyboard adds too. */
@Composable
private fun AddFilterField(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var input by remember { mutableStateOf("") }
    val submit = {
        if (input.isNotBlank()) {
            onAdd(input.trim())
            input = ""
        }
    }
    TextField(
        value = input,
        onValueChange = { input = it },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(50),
        placeholder = { Text(stringResource(R.string.label_add_title_filter)) },
        leadingIcon = { Icon(Icons.Outlined.FilterAlt, contentDescription = null) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = tintedCardColor(),
            unfocusedContainerColor = tintedCardColor(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        trailingIcon = {
            FilledIconButton(
                onClick = submit,
                enabled = input.isNotBlank(),
                modifier = Modifier.padding(end = 4.dp),
                colors = IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.btn_add))
            }
        }
    )
}
