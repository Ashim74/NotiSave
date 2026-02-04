package com.droidnova.notificationhistory.presentation.screens.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.about_utils.getRandomOtherApps

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val otherApps = remember { getRandomOtherApps() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about_title),
                        fontWeight = FontWeight.W900,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(bottom = 4.dp)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            AppHeader()
            SpacerHeight(16.dp)

            Items(
                imageVector = Icons.Outlined.Star,
                headingText = stringResource(R.string.rate_us),
                labelText = stringResource(R.string.rate_us_desc),
                onClick = { IntentUtil.openRateUs(context) }
            )
            SpacerHeight(8.dp)

            Items(
                imageVector = Icons.Outlined.Share,
                headingText = stringResource(R.string.share_us),
                labelText = stringResource(R.string.share_us_desc),
                onClick = { IntentUtil.shareApp(context) }
            )
            SpacerHeight(8.dp)

            Items(
                iconRes = R.drawable.ic_report_bugs,
                headingText = stringResource(R.string.report_bugs),
                labelText = stringResource(R.string.report_bugs_desc),
                onClick = { IntentUtil.sendSupportMail(context, isBug = true) }
            )
            SpacerHeight(8.dp)

            Items(
                iconRes = R.drawable.ic_instagram,
                headingText = stringResource(R.string.follow_instagram),
                labelText = stringResource(R.string.follow_instagram_desc),
                onClick = { IntentUtil.openInstagram(context) },
                overrideTint = false
            )
            SpacerHeight(8.dp)

            Items(
                iconRes = R.drawable.ic_whatsapp,
                headingText = stringResource(R.string.join_whatsapp),
                labelText = stringResource(R.string.join_whatsapp_desc),
                onClick = { IntentUtil.openWhatsApp(context) },
                overrideTint = false
            )
            SpacerHeight(8.dp)

            val version = IntentUtil.fetchAppVersion(context)

            Items(
                imageVector = Icons.Outlined.Info,
                headingText = stringResource(R.string.app_version),
                labelText = version,
                onClick = {},
                enabled = false
            )
            SpacerHeight(8.dp)

            Text(
                text = stringResource(R.string.checkout_other_apps),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            )
            SpacerHeight(8.dp)

            otherApps.forEach { app ->
                AppCard(
                    onClick = { IntentUtil.openPlayStore(context, app.packageName) },
                    title = stringResource(app.titleRes),
                    description = stringResource(app.descriptionRes),
                    iconRes = app.iconRes
                )
                SpacerHeight(8.dp)
            }

            AppCard(
                onClick = { IntentUtil.openDeveloperPlayConsole(context) },
                title = stringResource(R.string.check_more_apps_playstore),
                description = "",
                iconRes = R.drawable.ic_play_store
            )
            SpacerHeight(16.dp)
        }
    }
}

@Composable
private fun AppHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_notification_history),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(72.dp)
        )
        SpacerHeight(12.dp)
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        SpacerHeight(4.dp)
        Text(
            text = stringResource(R.string.app_about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun AppCard(
    onClick: () -> Unit,
    title: String,
    description: String,
    @DrawableRes iconRes: Int
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = title,
                modifier = Modifier.size(50.dp),
                tint = Color.Unspecified
            )
            SpacerWidth(8)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Items(
    headingText: String,
    labelText: String,
    onClick: () -> Unit,
    imageVector: ImageVector? = null,
    @DrawableRes iconRes: Int? = null,
    overrideTint: Boolean = true,
    enabled: Boolean = true
) {
    val rowModifier = if (enabled) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier
    ) {
        when {
            imageVector != null -> {
                Icon(
                    imageVector = imageVector,
                    contentDescription = headingText,
                    modifier = Modifier.padding(12.dp),
                    tint = if (overrideTint) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            }

            iconRes != null -> {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = headingText,
                    modifier = Modifier.padding(12.dp),
                    tint = if (overrideTint) MaterialTheme.colorScheme.onSurface else Color.Unspecified
                )
            }

            else -> {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Column {
            Text(
                text = headingText,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun SpacerHeight(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
private fun SpacerWidth(width: Int) {
    Spacer(modifier = Modifier.width(width.dp))
}
