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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AddWordField
import com.droidnova.notificationhistory.presentation.components.EmptyState
import com.droidnova.notificationhistory.presentation.components.GroupRowGap
import com.droidnova.notificationhistory.presentation.components.HeaderButton
import com.droidnova.notificationhistory.presentation.components.HistoryAppIcon
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ProBadge
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.groupedShape
import com.droidnova.notificationhistory.presentation.components.tileColor
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.dialogs.PremiumUpsell
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens

/** Title filters a free user can keep per app; Premium removes the limit. */
private const val FREE_TITLE_FILTER_LIMIT = 2

/**
 * Per-app capture rules: the app as a header card, then "only save titles containing" (a few
 * free, unlimited with Premium) and "never save if it contains" (Premium block words).
 */
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
    val blockMap by mainViewModel.blockWords.collectAsState()
    val isPremium by mainViewModel.isPremium.collectAsState()
    var showPremium by remember { mutableStateOf(false) }
    val filters = filtersMap[packageName].orEmpty().toList().asReversed()
    val blockWords = blockMap[packageName].orEmpty().toList().asReversed()
    // Filters saved before the limit existed are kept; only adding more needs Premium.
    val filtersLocked = !isPremium && filters.size >= FREE_TITLE_FILTER_LIMIT

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = appLabel,
                onBack = { navController.popBackStack() },
                actions = {
                    if (filters.isNotEmpty() || blockWords.isNotEmpty()) {
                        HeaderButton(
                            icon = Icons.Outlined.DeleteSweep,
                            contentDescription = stringResource(R.string.btn_clear_all_filters),
                            onClick = {
                                mainViewModel.clearTitleFilters(packageName)
                                mainViewModel.clearBlockWords(packageName)
                            },
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

            item(key = "filters-header") {
                RuleHeader(
                    title = stringResource(R.string.filters_only_section),
                    hint = if (isPremium) null else pluralStringResource(
                        R.plurals.filters_free_limit, FREE_TITLE_FILTER_LIMIT, FREE_TITLE_FILTER_LIMIT
                    )
                )
            }
            item(key = "filters-add") {
                AddWordField(
                    placeholder = stringResource(R.string.label_add_title_filter),
                    leadingIcon = Icons.Outlined.FilterAlt,
                    onAdd = { mainViewModel.addTitleFilter(packageName, it) },
                    locked = filtersLocked,
                    onLocked = { showPremium = true },
                    modifier = Modifier
                        .padding(horizontal = Dimens.ScreenHorizontal, vertical = 8.dp)
                        .appearIn(1)
                )
            }
            ruleList(
                keyPrefix = "filter",
                words = filters,
                emptyTitle = R.string.label_no_filters,
                emptyIcon = Icons.Outlined.FilterAltOff,
                icon = Icons.Outlined.Title,
                accent = AccentColors.Teal,
                onRemove = { mainViewModel.removeTitleFilter(packageName, it) }
            )

            item(key = "block-header") {
                RuleHeader(title = stringResource(R.string.filters_block_section), pro = !isPremium)
            }
            item(key = "block-add") {
                AddWordField(
                    placeholder = stringResource(R.string.label_add_block_word),
                    leadingIcon = Icons.Outlined.Block,
                    onAdd = { mainViewModel.addBlockWord(packageName, it) },
                    locked = !isPremium,
                    onLocked = { showPremium = true },
                    modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontal, vertical = 8.dp)
                )
            }
            ruleList(
                keyPrefix = "block",
                words = blockWords,
                emptyTitle = R.string.label_no_block_words,
                emptyIcon = Icons.Outlined.Block,
                icon = Icons.Outlined.Block,
                accent = AccentColors.Rose,
                onRemove = { mainViewModel.removeBlockWord(packageName, it) }
            )
        }
    }

    PremiumUpsell(mainViewModel, visible = showPremium, onDismiss = { showPremium = false })
}

@Composable
private fun RuleHeader(title: String, hint: String? = null, pro: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenHorizontal, end = Dimens.ScreenHorizontal, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(title, Modifier.weight(1f), first = true)
        if (pro) ProBadge()
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One rule list as grouped rows with a remove button, or a small empty state. */
private fun LazyListScope.ruleList(
    keyPrefix: String,
    words: List<String>,
    emptyTitle: Int,
    emptyIcon: ImageVector,
    icon: ImageVector,
    accent: Color,
    onRemove: (String) -> Unit
) {
    if (words.isEmpty()) {
        item(key = "$keyPrefix-empty") {
            EmptyState(title = stringResource(emptyTitle), icon = emptyIcon, modifier = Modifier.padding(vertical = 8.dp))
        }
        return
    }
    itemsIndexed(words, key = { _, word -> "$keyPrefix:$word" }) { index, word ->
        ListRow(
            modifier = Modifier
                .animateItem()
                .padding(horizontal = Dimens.ScreenHorizontal, vertical = GroupRowGap / 2),
            shape = groupedShape(index, words.size),
            title = word,
            leading = { IconBadge(icon, accent = accent, size = 34.dp) },
            trailing = {
                IconButton(onClick = { onRemove(word) }) {
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
