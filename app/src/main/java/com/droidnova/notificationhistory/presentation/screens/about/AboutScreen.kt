package com.droidnova.notificationhistory.presentation.screens.about

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.ListGroup
import com.droidnova.notificationhistory.presentation.components.ListRow
import com.droidnova.notificationhistory.presentation.ui.theme.Dimens
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil
import com.droidnova.notificationhistory.utils.about_utils.getRandomOtherApps

/** Logo + version, then three short groups: support us, community, our other apps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val otherApps = remember { getRandomOtherApps() }
    val version = remember { IntentUtil.fetchAppVersion(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenHorizontal, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AppHeader(version)

            ListGroup {
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.rate_us),
                        leading = { IconBadge(Icons.Outlined.Star) },
                        onClick = { IntentUtil.openRateUs(context) }
                    )
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.share_us),
                        leading = { IconBadge(Icons.Outlined.Share) },
                        onClick = { IntentUtil.shareApp(context) }
                    )
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.report_bugs),
                        leading = { IconBadge(ImageVector.vectorResource(R.drawable.ic_report_bugs)) },
                        onClick = { IntentUtil.sendSupportMail(context, isBug = true) }
                    )
                }
            }

            ListGroup {
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.follow_instagram),
                        leading = { BrandIcon(R.drawable.ic_instagram) },
                        onClick = { IntentUtil.openInstagram(context) }
                    )
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.join_whatsapp),
                        leading = { BrandIcon(R.drawable.ic_whatsapp) },
                        onClick = { IntentUtil.openWhatsApp(context) }
                    )
                }
            }

            ListGroup(title = stringResource(R.string.checkout_other_apps)) {
                otherApps.forEach { app ->
                    row { shape ->
                        ListRow(
                            shape = shape,
                            title = stringResource(app.titleRes),
                            value = stringResource(app.descriptionRes),
                            leading = { BrandIcon(app.iconRes, size = 40) },
                            onClick = { IntentUtil.openPlayStore(context, app.packageName) }
                        )
                    }
                }
                row { shape ->
                    ListRow(
                        shape = shape,
                        title = stringResource(R.string.check_more_apps_playstore),
                        leading = { BrandIcon(R.drawable.ic_play_store) },
                        onClick = { IntentUtil.openDeveloperPlayConsole(context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppHeader(version: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.ic_notification_history),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.about_version, version),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Full-colour logo (Instagram, WhatsApp, other apps): never tinted. */
@Composable
private fun BrandIcon(@DrawableRes iconRes: Int, size: Int = 32) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(size.dp),
        tint = Color.Unspecified
    )
}
