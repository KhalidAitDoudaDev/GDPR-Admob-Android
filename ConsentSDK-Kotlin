package {PACKAGE_NAME}

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import java.net.MalformedURLException
import java.net.URL


class ConsentSDK(private val context: Context, private val publisherId: String, private val pravicyURL: String, var DEBUG: Boolean=false) {

    companion object {

        private val ads_preference = "ads_preference"
        private val PERSONALIZED = true
        private val NON_PERSONALIZED = false

        // Consent status
        fun isConsentPersonalized(context: Context): Boolean {
            val settings = PreferenceManager.getDefaultSharedPreferences(context)
            return settings.getBoolean(ads_preference, PERSONALIZED)
        }

        // Get AdRequest
        fun getAdRequest(context: Context): AdRequest {
            // Check consent Personalized
            return if(isConsentPersonalized(context)) {
                AdRequest.Builder()
                        .build()
            } else {
                AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, getNonPersonalizedAdsBundle())
                        .build()
            }
        }

        // Get Non Personalized Ads Bundle
        private fun getNonPersonalizedAdsBundle(): Bundle {
            val extras = Bundle()
            extras.putString("npa", "1")
            return extras
        }
    }

    // Parameters
    private lateinit var form: ConsentForm
    var LOG_TAG = "ID_LOG"
    var DEVICE_ID = ""

    // User choice handler
    private val settings: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)



    // Initialize Consent SDK
    fun checkConsent(callback: (() -> Unit)) {
        val consentInformation = ConsentInformation.getInstance(context)
        // Use for debugging
        if(DEBUG) {
            if(DEVICE_ID.isNotBlank()) {
                consentInformation.addTestDevice(DEVICE_ID)
            }
            consentInformation.debugGeography = DebugGeography.DEBUG_GEOGRAPHY_EEA
        }

        // Initialize Publisher ids
        val publisherIds = arrayOf(publisherId)
        consentInformation.requestConsentInfoUpdate(publisherIds, object: ConsentInfoUpdateListener {

            // When consent info has been updated
            override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
                when(consentStatus) {
                    // If the consent status is unknown
                    ConsentStatus.UNKNOWN -> {
                        // Debugging
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Unknown Consent")
                            Log.d(LOG_TAG, "User location within EEA: ${consentInformation.isRequestLocationInEeaOrUnknown}")
                        }
                        // Check if the user location
                        if(consentInformation.isRequestLocationInEeaOrUnknown) {
                            requestConsent(callback)
                        } else {
                            consentIsPersonalized()
                            callback.invoke()
                        }
                    }
                    // Consent is non-personalized
                    ConsentStatus.NON_PERSONALIZED -> {
                        consentIsNonPersonalized()
                        callback.invoke()
                    }
                    // Consent is personalized
                    else -> {
                        consentIsPersonalized()
                        callback.invoke()
                    }
                }
            }

            // When the consent info has been failed
            override fun onFailedToUpdateConsentInfo(reason: String?) {
                if(DEBUG) {
                    Log.d(LOG_TAG, "Failed to update: $reason")
                }
            }
        })
    }

    // Consent is personalized
    private fun consentIsPersonalized() {
        settings.edit().putBoolean(ads_preference, PERSONALIZED).apply()
    }

    // Consent is non personalized
    private fun consentIsNonPersonalized() {
        settings.edit().putBoolean(ads_preference, NON_PERSONALIZED).apply()
    }

    private fun requestConsent(callback: () -> Unit) {
        var privacyUrl: URL? = null

        try {
            privacyUrl = URL(pravicyURL)
        } catch(e: MalformedURLException) {
//            e.printStackTrace()
        }


        Log.d(LOG_TAG, "Initialize Form")
        // Initialize form
        form = ConsentForm.Builder(context, privacyUrl)
                .withListener(object: ConsentFormListener() {

                    // When the consent form is loaded
                    override fun onConsentFormLoaded() {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form is loaded!")
                        }
                        form.show()
                    }

                    // When the consent form is opened
                    override fun onConsentFormOpened() {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form is opened!")
                        }
                    }

                    // When the consent form is closed
                    override fun onConsentFormClosed(consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form Closed!")
                        }
                        // Check the status of consent
                        when(consentStatus) {
                            ConsentStatus.NON_PERSONALIZED -> {
                                consentIsNonPersonalized()
                            }
                            else -> {
                                consentIsPersonalized()
                            }
                        }
                        // Callback
                        callback()
                    }

                    // When received an error
                    override fun onConsentFormError(reason: String?) {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form ERROR: $reason")
                        }
                        // Resume if error occurred
                        callback()
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build()
        Log.d(LOG_TAG, "Loading Form!")
        form.load()
    }

}
