package com.droidnova.notificationhistory.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.appearIn
import com.droidnova.notificationhistory.presentation.components.floating
import com.droidnova.notificationhistory.presentation.components.tintedCardColor
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors

/**
 * Premium offer: a floating gradient crown, one line of pitch, the three benefits as icon tiles
 * side by side, then the buy button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPurchaseBottomSheet(
    priceLabel: String?,
    isLoading: Boolean,
    onPurchaseClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .floating()
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.premium_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.premium_sheet_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(tintedCardColor())
                    .padding(vertical = 12.dp, horizontal = 4.dp)
            ) {
                BenefitTile(Icons.Outlined.Block, stringResource(R.string.premium_sheet_benefit_no_ads), AccentColors.Rose, 0, Modifier.weight(1f))
                BenefitTile(Icons.Outlined.Update, stringResource(R.string.premium_sheet_benefit_updates), AccentColors.Blue, 1, Modifier.weight(1f))
                BenefitTile(Icons.Outlined.SupportAgent, stringResource(R.string.premium_sheet_benefit_support), AccentColors.Green, 2, Modifier.weight(1f))
            }

            Button(
                onClick = onPurchaseClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isLoading
            ) {
                val btnText = when {
                    priceLabel != null -> stringResource(
                        R.string.premium_sheet_button_with_price,
                        priceLabel
                    )
                    else -> stringResource(R.string.premium_sheet_button_loading)
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(text = btnText, fontWeight = FontWeight.SemiBold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.premium_sheet_secure_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BenefitTile(icon: ImageVector, label: String, accent: Color, index: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.appearIn(index, stepMillis = 80),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconBadge(icon, accent = accent, size = 44.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun PremiumWelcomeDialog(
    onDismiss: () -> Unit,
    onRestart: () -> Unit,
    onOpenInstagram: () -> Unit,
    onOpenWhatsapp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            IconBadge(
                Icons.Outlined.Celebration,
                accent = AccentColors.Amber,
                size = 56.dp,
                modifier = Modifier.floating(distance = 3.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.premium_welcome_title),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.premium_welcome_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.premium_welcome_connect_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumSocialButton(
                        iconRes = R.drawable.ic_instagram,
                        label = stringResource(R.string.set_instagram),
                        onClick = onOpenInstagram,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumSocialButton(
                        iconRes = R.drawable.ic_whatsapp,
                        label = stringResource(R.string.set_whatsapp),
                        onClick = onOpenWhatsapp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onRestart) {
                Text(text = stringResource(R.string.premium_welcome_restart_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.premium_welcome_later_button))
            }
        }
    )
}

@Composable
private fun PremiumSocialButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
