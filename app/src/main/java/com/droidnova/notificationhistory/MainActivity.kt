package com.droidnova.notificationhistory

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.billing.PremiumBillingManager
import com.droidnova.notificationhistory.presentation.navigation.AppNavGraph
import com.droidnova.notificationhistory.presentation.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var billingManager: PremiumBillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billingManager = PremiumBillingManager(
            context = applicationContext,
            onPremiumStatusChanged = { isPremium -> viewModel.setPremiumPurchased(isPremium) },
            onError = { message -> Log.w("Billing", message) }
        )
        billingManager.queryActivePurchases()
        billingManager.queryProductDetails()
        setContent {
            CompositionLocalProvider(LocalPremiumBillingManager provides billingManager) {
                AppTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        AppNavGraph()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
