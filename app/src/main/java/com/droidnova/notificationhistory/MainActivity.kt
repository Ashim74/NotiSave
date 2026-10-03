package com.droidnova.notificationhistory

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.droidnova.notificationhistory.ads.AdsConsentManager
import com.droidnova.notificationhistory.billing.LocalPremiumBillingManager
import com.droidnova.notificationhistory.billing.PremiumBillingManager
import com.droidnova.notificationhistory.core.lock.AppLock
import com.droidnova.notificationhistory.core.lock.AppLockController
import com.droidnova.notificationhistory.core.lock.BiometricGate
import com.droidnova.notificationhistory.core.lock.LocalBiometricGate
import com.droidnova.notificationhistory.presentation.screens.lock.AppLockGate
import com.droidnova.notificationhistory.presentation.screens.lock.LockScreen
import com.droidnova.notificationhistory.data.datastore.UserPreferences
import com.droidnova.notificationhistory.presentation.navigation.AppNavGraph
import com.droidnova.notificationhistory.presentation.navigation.LaunchAction
import com.droidnova.notificationhistory.presentation.screens.splash.SplashIntro
import com.droidnova.notificationhistory.presentation.ui.theme.AppTheme
import com.droidnova.notificationhistory.presentation.ui.theme.isDark
import com.droidnova.notificationhistory.service.ListenerReconnector
import com.droidnova.notificationhistory.utils.Analytics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// FragmentActivity (not ComponentActivity) because BiometricPrompt needs a fragment host.
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var billingManager: PremiumBillingManager
    private val appLock by lazy { AppLock.get(applicationContext) }
    private lateinit var biometricGate: BiometricGate

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        Analytics.init(applicationContext)
        // Hold the splash until we know whether to show onboarding or Home, and whether the
        // app is locked; avoids a flash of the wrong screen (or of history) on cold start.
        // A fresh launch hands over to the animated intro on the first frame (the intro covers the
        // loading); otherwise (recreated activity) the system splash covers it as before.
        val playIntro = savedInstanceState == null
        splash.setKeepOnScreenCondition {
            !playIntro && !isStartupReady()
        }
        biometricGate = BiometricGate(this, onHandoff = appLock.controller::beginHandoff)
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

            val gate by appLock.controller.gate.collectAsState()

            CompositionLocalProvider(
                LocalPremiumBillingManager provides billingManager,
                LocalBiometricGate provides biometricGate
            ) {
                AppTheme(darkTheme = darkTheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // The launch action waits behind the lock: AppNavGraph only composes
                        // (and routes the shortcut / alert) once unlocked.
                        // Once per launch (kept across rotation): the icon-and-name intro. The app
                        // is only built after its entrance, so building can't stutter it.
                        var showIntro by rememberSaveable { mutableStateOf(playIntro) }
                        var buildApp by rememberSaveable { mutableStateOf(!playIntro) }
                        if (buildApp) {
                            AppLockGate(
                                gate = gate,
                                lockContent = { LockScreen() },
                                appContent = { AppNavGraph(launchAction = launchAction) }
                            )
                        }
                        if (showIntro) {
                            val onboarding by viewModel.onboardingComplete.collectAsState()
                            SplashIntro(
                                ready = onboarding != null && gate != AppLockController.Gate.Loading,
                                onEntranceDone = { buildApp = true },
                                onFinished = { showIntro = false }
                            )
                        }
                    }
                }
            }
        }
    }

    /** [darkTheme] null → follow the system (used before the preference is known). */
    /** True once we know whether to show onboarding or Home, and whether the app is locked. */
    private fun isStartupReady(): Boolean =
        viewModel.onboardingComplete.value != null &&
            appLock.controller.gate.value != AppLockController.Gate.Loading

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

    override fun onStart() {
        super.onStart()
        appLock.controller.onAppStarted()
    }

    override fun onStop() {
        super.onStop()
        // Rotation / theme change recreate the activity; that is not leaving the app.
        if (!isChangingConfigurations) appLock.controller.onAppStopped()
    }

    /**
     * Every in-app launch of another screen (system settings, share sheet, mail, Play billing,
     * battery exemption, screen-lock check) funnels through here — `startActivity` included —
     * so returning from it doesn't ask for the PIN again.
     */
    @Deprecated("Deprecated in Java")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        appLock.controller.beginHandoff()
        try {
            @Suppress("DEPRECATION")
            super.startActivityForResult(intent, requestCode, options)
        } catch (error: RuntimeException) {
            appLock.controller.onAppResumed()
            throw error
        }
    }

    override fun onResume() {
        super.onResume()
        appLock.controller.onAppResumed()
        // Recover a listener binding the system dropped; no-op while healthy.
        ListenerReconnector.ensureConnected(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
