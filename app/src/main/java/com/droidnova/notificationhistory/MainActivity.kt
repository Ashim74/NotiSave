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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.droidnova.notificationhistory.ads.AdsConsentManager
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.billing.PremiumBillingManager
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.presentation.navigation.AppNavGraph
import com.droidnova.notificationhistory.presentation.navigation.LaunchAction
import com.droidnova.notificationhistory.presentation.ui.theme.AppTheme
import com.droidnova.notificationhistory.presentation.ui.theme.isDark
import com.droidnova.notificationhistory.service.ListenerReconnector
import com.droidnova.notificationhistory.utils.Analytics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var billingManager: PremiumBillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        Analytics.init(applicationContext)
        // Hold the splash until we know whether to show onboarding or Home; avoids a flash of
        // the wrong screen on cold start.
        splash.setKeepOnScreenCondition { viewModel.onboardingComplete.value == null }
        val launchAction = LaunchAction.from(intent)
        if (launchAction == LaunchAction.Reconnect) Analytics.log(Analytics.RECONNECT_TAPPED)
        applyEdgeToEdge(darkTheme = null)
        billingManager = PremiumBillingManager(
            context = applicationContext,
            onPremiumStatusChanged = { isPremium -> viewModel.setPremiumPurchased(isPremium) },
            onError = { message -> Log.w("Billing", message) }
        )
        viewModel.incrementLaunchCount()
        billingManager.queryActivePurchases()
        billingManager.queryProductDetails()
        lifecycleScope.launch {
            // Premium users never see ads, so they are never asked for ad consent.
            if (!UserPreferences(applicationContext).isPremium.first()) {
                AdsConsentManager.getInstance(applicationContext).gatherConsent(this@MainActivity)
            }
        }
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = themeMode.isDark()
            // System bar icon contrast must follow the in-app theme, not only the OS setting.
            LaunchedEffect(darkTheme) { applyEdgeToEdge(darkTheme) }

            CompositionLocalProvider(LocalPremiumBillingManager provides billingManager) {
                AppTheme(darkTheme = darkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AppNavGraph(launchAction = launchAction)
                    }
                }
            }
        }
    }

    /** [darkTheme] null → follow the system (used before the preference is known). */
    private fun applyEdgeToEdge(darkTheme: Boolean?) {
        val statusBarStyle = if (darkTheme == null) {
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
        }
        val navigationBarStyle = if (darkTheme == null) {
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        } else {
            SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
        }
        enableEdgeToEdge(statusBarStyle = statusBarStyle, navigationBarStyle = navigationBarStyle)
    }

    override fun onResume() {
        super.onResume()
        // Recover a listener binding the system dropped; no-op while healthy.
        ListenerReconnector.ensureConnected(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
