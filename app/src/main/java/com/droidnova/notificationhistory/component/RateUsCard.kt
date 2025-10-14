package com.droidnova.notificationhistory.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidnova.notificationhistory.R

@Composable
fun RateUsCard(
    onOkClicked: () -> Unit, // Will be called for 5 stars (Play Store)
    onCancelClicked: () -> Unit,
    modifier: Modifier = Modifier,
    onRated: (Int) -> Unit,
    onFeedbackClicked: () -> Unit, // New: called for <5 stars
) {
    var selectedStars by remember { mutableStateOf(0) }
    var showPopup by remember { mutableStateOf(false) }
    var popupStarIndex by remember { mutableStateOf(-1) }

    val rateUsTitles = remember {
        listOf(
            "Your 5-star rating motivates us to bring you more awesome features!",
            "A 5-star rating from you inspires us to keep improving and adding new features!",
            "Your 5-star support helps us build even better experiences!"
        )
    }
    val randomTitle = remember { rateUsTitles.random() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            )
    ) {
        Column(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = randomTitle,
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(5) { index ->
                    val isFilled = index < selectedStars
                    val scale by animateFloatAsState(
                        targetValue = if (showPopup && popupStarIndex == index) 1.4f else 1f,
                        animationSpec = tween(durationMillis = 200),
                        finishedListener = {
                            showPopup = false
                            popupStarIndex = -1
                        }
                    )

                    Icon(
                        painter = painterResource(if (isFilled) R.drawable.ic_star_filled else R.drawable.ic_star_outline),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clickable {
                                selectedStars = index + 1
                                onRated(selectedStars)
                                popupStarIndex = index
                                showPopup = true
                            },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(1f),
                    onClick = onCancelClicked
                ) {
                    Text(text = "Rate Later")
                }

                Button(
                    modifier = Modifier
                        .padding(4.dp)
                        .weight(1f),
                    onClick = {
                        if (selectedStars == 5) {
                            onOkClicked() // Play Store
                        } else {
                            onFeedbackClicked() // Feedback dialog or email intent
                        }
                    }
                ) {
                    Text(
                        text = "Give Feedback",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
