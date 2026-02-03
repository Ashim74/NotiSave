package com.droidnova.notificationhistory.presentation.screens.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.utils.Constants
import com.droidnova.notificationhistory.utils.IntentUtils
import com.droidnova.notificationhistory.utils.IntentUtils.openBVRAppOnPlayStore
import com.droidnova.notificationhistory.utils.IntentUtils.openClipboardHistoryAppOnPlayStore
import com.droidnova.notificationhistory.utils.IntentUtils.reportBugs


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(topBar =
        {
            TopAppBar(
                title = {Text("About",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge)},
                navigationIcon ={
                    IconButton(onClick = {navController.popBackStack()}) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->


        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(12.dp)
        ) {
            Items(
                icon = painterResource(R.drawable.ic_rate_us),
                headingText = "Rate US",
                labelText = "Give us your feedback by rating our app on the Play Store.",
                onClick = { IntentUtils.rateUs(context = context) }
            )
            SpacerHeight(8.dp)

            Items(
                icon = painterResource(R.drawable.ic_share_app),
                headingText = "Share Us",
                labelText = "Spread the word about our app to your friends and family.",
                onClick = { IntentUtils.shareApp(context = context) }
            )

            SpacerHeight(8.dp)

            Items(
                icon = painterResource(R.drawable.ic_report_bugs),
                headingText = "Report Bugs",
                labelText = "Report any bugs or issues you encounter.",
                onClick = { reportBugs(context = context) }
            )

            SpacerHeight(8.dp)

            Items(
                icon = painterResource(R.drawable.ic_instagram),
                headingText = "Follow Us on Instagram",
                labelText = "Get the latest updates, feature previews, and more on our Instagram.",
                onClick = { IntentUtils.joinInstagramCommunity(context) },
                overrideColor = false
            )

            SpacerHeight(8.dp)

            Items(
                icon = painterResource(R.drawable.ic_whatsapp),
                headingText = "Join Our WhatsApp Community",
                labelText = "Connect with other users, share feedback, and get support in real-time.",
                onClick = { IntentUtils.joinWhatsappCommunity(context) },
                overrideColor = false
            )

            SpacerHeight(8.dp)

            val version = IntentUtils.fetchAppVersion(context)

            Items(
                icon = painterResource(R.drawable.ic_version),
                headingText = "App Version",
                labelText = version,
                onClick = { }
            )
            SpacerHeight(8.dp)


            Text(
                text = "CheckOut Other Apps ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Gray
                )
            )
            SpacerHeight(8.dp)
            AppCard(
                onClick = { openClipboardHistoryAppOnPlayStore(context) },
                title = Constants.CLIPBOARD_HISTORY_APP_TITLE,
                description = Constants.CLIPBOARD_HISTORY_APP_DESCRIPTION,
                icon = R.drawable.clipboard_history_icon
            )
            SpacerHeight(8.dp)

            AppCard(
                onClick = { openBVRAppOnPlayStore(context) },
                title = Constants.BVR_APP_TITLE,
                description = Constants.BVR_APP_DESCRIPTION,
                icon = R.drawable.ic_bvr
            )
            SpacerHeight(8.dp)


            AppCard(
                onClick = { IntentUtils.openDeveloperDashboardOnPlayStore(context) },
                title = "Check more apps on PlayStore",
                description = "",
                icon = R.drawable.ic_play_store
            )

        }
    }
}

@Composable
fun AppCard(
    onClick: () -> Unit,
    title: String,
    description: String,
    icon: Int
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = "BVR",
                modifier = Modifier.size(50.dp)
            )
            SpacerWidth(8)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                )
                if (description.isNotEmpty()){
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Gray
                        )
                    )
                }

            }
        }

    }
}


@Composable
fun Items(icon: Painter, headingText: String, labelText: String, onClick: () -> Unit,overrideColor:Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
    ) {
        if (overrideColor){
            Icon(
                painter = icon,
                contentDescription = "Icon ",
                modifier = Modifier.padding(12.dp)
            )
        }else{
            Image(
                painter = icon,
                contentDescription = "Icon ",
                modifier = Modifier.padding(12.dp)
            )
        }

        Column() {
            Text(
                text = headingText,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.bodySmall,
                color = Gray
            )
        }
    }
}

@Composable
fun SpacerHeight(height: Dp) {
    Spacer(modifier = Modifier.height(height))
}

@Composable
fun SpacerWidth(width: Int) {
    Spacer(modifier = Modifier.width(width.dp))
}
