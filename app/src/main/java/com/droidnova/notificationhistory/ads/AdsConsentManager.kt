package com.droidnova.notificationhistory.ads

import android.app.Activity
import android.content.Context
import com.droidnova.notificationhistory.utils.CrashReporter
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gathers UMP consent (GDPR / US state privacy) and initializes the Mobile Ads SDK only once
 * consent allows ad requests. Ad views observe [adsReady] and stay empty until then.
 *
 * Follows Google's reference flow: request consent info on every launch, show the form if
 * required, and initialize the SDK off the main thread as soon as `canRequestAds()` is true —
 * which for returning users is immediately, without waiting on the network.
 */
class AdsConsentManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)
    private val mobileAdsInitialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _adsReady = MutableStateFlow(false)
    /** True once the SDK is initialized and consent permits ad requests. */
    val adsReady: StateFlow<Boolean> = _adsReady.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** UMP requires the app to expose a "privacy options" entry point while this is true. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    fun gatherConsent(activity: Activity, onDone: (FormError?) -> Unit = {}) {
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    refreshState()
                    onDone(formError)
                }
            },
            { requestError ->
                refreshState()
                onDone(requestError)
            }
        )
        // Returning users already consented in a previous session; don't block on the network.
        refreshState()
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismissed: (FormError?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onDismissed)
    }

    private fun refreshState() {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (canRequestAds) initializeMobileAdsIfNeeded()
    }

    private fun initializeMobileAdsIfNeeded() {
        if (!mobileAdsInitialized.compareAndSet(false, true)) return
        scope.launch {
            runCatching {
                MobileAds.initialize(appContext) { _adsReady.value = true }
            }.onFailure { error ->
                mobileAdsInitialized.set(false)
                CrashReporter.record(error)
            }
        }
    }

    companion object {
        @Volatile private var instance: AdsConsentManager? = null

        fun getInstance(context: Context): AdsConsentManager =
            instance ?: synchronized(this) {
                instance ?: AdsConsentManager(context).also { instance = it }
            }
    }
}
