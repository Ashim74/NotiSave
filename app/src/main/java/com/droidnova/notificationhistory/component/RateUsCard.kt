package com.droidnova.notificationhistory.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.presentation.components.AppCard
import com.droidnova.notificationhistory.presentation.components.IconBadge
import com.droidnova.notificationhistory.presentation.components.floating
import com.droidnova.notificationhistory.presentation.ui.theme.AccentColors
import kotlinx.coroutines.delay

/** Gold for filled stars, readable on both themes. */
private val StarGold = Color(0xFFFFB800)

/**
 * Compact rating card: a floating star badge with one line, five stars that pop as they fill
 * (a quick wave from left to right), then Later / Feedback.
 */
@Composable
fun RateUsCard(
    onOkClicked: () -> Unit, // Will be called for 5 stars (Play Store)
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onRated: (Int) -> Unit,
    onFeedbackClicked: () -> Unit, // Called for <5 stars
) {
    var selectedStars by remember { mutableIntStateOf(0) }
    // Stars light up one after another up to the selection, instead of all at once.
    var shownStars by remember { mutableIntStateOf(0) }
    var bounceKey by remember { mutableStateOf(0) }

    val rateUsTitles = remember {
        listOf(
            "Your 5-star rating motivates us to bring you more awesome features!",
            "A 5-star rating from you inspires us to keep improving and adding new features!",
            "Your 5-star support helps us build even better experiences!"
        )
    }
    val randomTitle = remember { rateUsTitles.random() }

    LaunchedEffect(selectedStars, bounceKey) {
        if (shownStars > selectedStars) shownStars = selectedStars
        while (shownStars < selectedStars) {
            delay(55)
            shownStars++
        }
    }

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    ImageVector.vectorResource(R.drawable.ic_star_filled),
                    accent = AccentColors.Amber,
                    size = 40.dp,
                    modifier = Modifier.floating(distance = 3.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = randomTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) { index ->
                    val isFilled = index < shownStars
                    val scale by animateFloatAsState(
                        targetValue = if (isFilled) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "starScale"
                    )

                    // Star inside a 48 dp touch target, announced as "N stars".
                    Icon(
                        painter = painterResource(if (isFilled) R.drawable.ic_star_filled else R.drawable.ic_star_outline),
                        contentDescription = pluralStringResource(
                            R.plurals.rate_us_star_description,
                            index + 1,
                            index + 1
                        ),
                        tint = if (isFilled) StarGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable {
                                selectedStars = index + 1
                                bounceKey++
                                onRated(selectedStars)
                            }
                            .padding(9.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancelClicked) {
                    Text(text = "Rate Later")
                }
                Spacer(Modifier.width(6.dp))
                Button(
                    onClick = {
                        if (selectedStars == 5) {
                            onOkClicked() // Play Store
                        } else {
                            onFeedbackClicked() // Feedback dialog or email intent
                        }
                    }
                ) {
                    Text(text = "Give Feedback")
                }
            }
        }
    }
}
