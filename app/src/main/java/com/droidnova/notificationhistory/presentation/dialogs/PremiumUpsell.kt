package com.droidnova.notificationhistory.presentation.dialogs

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.droidnova.notificationhistory.MainViewModel
import com.droidnova.notificationhistory.R
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.utils.about_utils.IntentUtil

/**
 * The Premium sheet for a locked feature, with everything around it: the price lookup, the
 * purchase flow, billing errors and the welcome dialog once bought. Shown while [visible];
 * closes itself when the purchase lands.
 */
@Composable
fun PremiumUpsell(mainViewModel: MainViewModel, visible: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val billingManager = LocalPremiumBillingManager.current
    val isPremium by mainViewModel.isPremium.collectAsState()
    val showPremiumWelcome by mainViewModel.showPremiumWelcome.collectAsState()
    val productDetails = billingManager?.productDetails?.collectAsState()?.value
    val isFetchingPrice = billingManager?.isFetchingProductDetails?.collectAsState()?.value ?: false
    val isPurchaseInProgress = billingManager?.isPurchaseInProgress?.collectAsState()?.value ?: false
    val priceLabel = productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        if (billingManager == null) {
            Toast.makeText(context, context.getString(R.string.billing_unavailable), Toast.LENGTH_LONG).show()
            onDismiss()
        } else {
            mainViewModel.onRemoveAdsClicked()
            billingManager.queryProductDetails()
        }
    }
    LaunchedEffect(billingManager, visible) {
        if (visible) billingManager?.errors?.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(isPremium) { if (isPremium && visible) onDismiss() }

    if (visible && !isPremium && billingManager != null) {
        PremiumPurchaseBottomSheet(
            priceLabel = priceLabel,
            isLoading = isFetchingPrice || isPurchaseInProgress,
            onPurchaseClick = { (context as? Activity)?.let(billingManager::launchPurchaseFlow) },
            onDismiss = onDismiss
        )
    }
    if (showPremiumWelcome) {
        PremiumWelcomeDialog(
            onDismiss = mainViewModel::dismissPremiumWelcome,
            onRestart = {
                mainViewModel.dismissPremiumWelcome()
                IntentUtil.restartApp(context)
            },
            onOpenInstagram = { IntentUtil.openInstagram(context) },
            onOpenWhatsapp = { IntentUtil.openWhatsApp(context) }
        )
    }
}
