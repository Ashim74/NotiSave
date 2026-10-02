package com.droidnova.notificationhistory.presentation.screens.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.ActionTile
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.ListGroup
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.components.ScreenTopBar
import com.droidnova.notificationhistory.presentation.components.SectionHeader
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.floating
import com.droidnova.notificationhistory.presentation.components.pressScale
import com.droidnova.notificationhistory.presentation.components.tileColor
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.about_utils.getRandomOtherApps

/** Logo + version, a 3×2 grid of quick actions, then our other apps. */
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val otherApps = remember { getRandomOtherApps() }
    val version = remember { IntentUtil.fetchAppVersion(context) }

    Scaffold(
        topBar = {
            ScreenTopBar(
                title = stringResource(R.string.about_title),
                onBack = { navController.popBackStack() }
            )
        },
        contentWindowInsets = WindowInsets(bottom = 4.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenHorizontal, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppHeader(version, Modifier.appearIn(0))

            AppCard(Modifier.fillMaxWidth().appearIn(1)) {
                Column(Modifier.padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        ActionTile(
                            icon = Icons.Outlined.StarRate,
                            label = stringResource(R.string.rate_us),
                            accent = AccentColors.Amber,
                            onClick = { IntentUtil.openRateUs(context) },
                            modifier = Modifier.weight(1f)
                        )
                        ActionTile(
                            icon = Icons.Outlined.Share,
                            label = stringResource(R.string.share_us),
                            accent = AccentColors.Blue,
                            onClick = { IntentUtil.shareApp(context) },
                            modifier = Modifier.weight(1f)
                        )
                        ActionTile(
                            icon = Icons.Outlined.BugReport,
                            label = stringResource(R.string.report_bugs),
                            accent = AccentColors.Rose,
                            onClick = { IntentUtil.sendSupportMail(context, isBug = true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(Modifier.fillMaxWidth()) {
                        BrandTile(
                            iconRes = R.drawable.ic_instagram,
                            label = stringResource(R.string.set_instagram),
                            description = stringResource(R.string.follow_instagram),
                            onClick = { IntentUtil.openInstagram(context) },
                            modifier = Modifier.weight(1f)
                        )
                        BrandTile(
                            iconRes = R.drawable.ic_whatsapp,
                            label = stringResource(R.string.set_whatsapp),
                            description = stringResource(R.string.join_whatsapp),
                            onClick = { IntentUtil.openWhatsApp(context) },
                            modifier = Modifier.weight(1f)
                        )
                        BrandTile(
                            iconRes = R.drawable.ic_play_store,
                            label = stringResource(R.string.set_more_apps),
                            description = stringResource(R.string.check_more_apps_playstore),
                            onClick = { IntentUtil.openDeveloperPlayConsole(context) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(Modifier.appearIn(2)) {
                SectionHeader(
                    text = stringResource(R.string.checkout_other_apps),
                    first = true,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                ListGroup {
                    otherApps.forEach { app ->
                        row { shape ->
                            ListRow(
                                shape = shape,
                                title = stringResource(app.titleRes),
                                value = stringResource(app.descriptionRes),
                                leading = { AppLogo(app.iconRes) },
                                onClick = { IntentUtil.openPlayStore(context, app.packageName) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AppHeader(version: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.ic_notification_history),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .floating(distance = 3.dp)
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.about_version, version),
            modifier = Modifier
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(tintedCardColor())
                .padding(horizontal = 10.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Like [ActionTile], but for a full-colour logo (Instagram, WhatsApp, Play Store). */
@Composable
private fun BrandTile(
    @DrawableRes iconRes: Int,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(interaction)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tileColor()),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(26.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Another app's icon in a rounded tile, never tinted. */
@Composable
private fun AppLogo(@DrawableRes iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(Dimens.TileCornerRadius)),
        tint = Color.Unspecified
    )
}
