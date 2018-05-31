package {PACKAGE_NAME};

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;

import java.net.MalformedURLException;
import java.net.URL;

public class ConsentSDK {

    abstract static class ConsentSDKCallback {
        abstract public void onResult();
    }

    private static String ads_preference = "ads_preference";
    private static boolean PERSONALIZED = true;
    private static boolean NON_PERSONALIZED = false;

    private Context context;
    private ConsentForm form;
    public String LOG_TAG = "ID_LOG";
    public String DEVICE_ID = "";
    public boolean DEBUG = false;
    public String pravicyURL;
    public String publisherId;

    private SharedPreferences settings;


    // Initializing
    public ConsentSDK(Context context, String publisherId, String pravicyURL, boolean DEBUG) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.publisherId = publisherId;
        this.pravicyURL = pravicyURL;
        this.DEBUG = DEBUG;
    }

    public ConsentSDK(Context context, String publisherId, String pravicyURL) {
        this.context = context;
        this.settings = PreferenceManager.getDefaultSharedPreferences(context);
        this.publisherId = publisherId;
        this.pravicyURL = pravicyURL;
    }

    // Consent status
    public static boolean isConsentPersonalized(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getBoolean(ads_preference, PERSONALIZED);
    }

    // Consent is personalized
    private void consentIsPersonalized() {
        settings.edit().putBoolean(ads_preference, PERSONALIZED).apply();
    }

    // Consent is non personalized
    private void consentIsNonPersonalized() {
        settings.edit().putBoolean(ads_preference, NON_PERSONALIZED).apply();
    }

    // Get AdRequest
    public static AdRequest getAdRequest(Context context) {
        if(isConsentPersonalized(context)) {
            return new AdRequest.Builder().build();
        } else {
            return new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, getNonPersonalizedAdsBundle())
                    .build();
        }
    }

    // Get Non Personalized Ads Bundle
    private static Bundle getNonPersonalizedAdsBundle() {
        Bundle extras = new Bundle();
        extras.putString("npa", "1");
        return extras;
    }

    // Initialize Consent SDK
    public void checkConsent(final ConsentSDKCallback callback) {
        final ConsentInformation consentInformation = ConsentInformation.getInstance(context);
        if(DEBUG) {
            if(!DEVICE_ID.isEmpty()) {
                consentInformation.addTestDevice(DEVICE_ID);
            }
            consentInformation.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
        }
        String[] publisherIds = {publisherId};
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                // Switch consent
                switch(consentStatus) {
                    case UNKNOWN:
                        // Debugging
                        if (DEBUG) {
                            Log.d(LOG_TAG, "Unknown Consent");
                            Log.d(LOG_TAG, "User location within EEA: " + consentInformation.isRequestLocationInEeaOrUnknown());
                        }
                        // Check if user location
                        if(consentInformation.isRequestLocationInEeaOrUnknown()) {
                            requestConsent(callback);
                        } else {
                            consentIsPersonalized();
                            // Callback
                            callback.onResult();
                        }
                        break;
                    case NON_PERSONALIZED:
                        consentIsNonPersonalized();
                        // Callback
                        callback.onResult();
                        break;
                    default:
                        consentIsPersonalized();
                        // Callback
                        callback.onResult();
                        break;
                }
            }

            @Override
            public void onFailedToUpdateConsentInfo(String reason) {
                if(DEBUG) {
                    Log.d(LOG_TAG, "Failed to update: $reason");
                }
            }
        });
    }

    private void requestConsent(final ConsentSDKCallback callback) {
        URL privacyUrl = null;
        try {
            privacyUrl = new URL(pravicyURL);
        } catch (MalformedURLException e) {
//            e.printStackTrace();
        }
        form = new ConsentForm.Builder(context, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form is loaded!");
                        }
                        form.show();
                    }

                    @Override
                    public void onConsentFormError(String reason) {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form ERROR: $reason");
                        }
                        // Callback on Error
                        callback.onResult();
                    }

                    @Override
                    public void onConsentFormOpened() {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form is opened!");
                        }
                    }

                    @Override
                    public void onConsentFormClosed(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        if(DEBUG) {
                            Log.d(LOG_TAG, "Consent Form Closed!");
                        }
                        switch (consentStatus) {
                            case NON_PERSONALIZED:
                                consentIsNonPersonalized();
                                // callback
                                break;
                            default:
                                consentIsPersonalized();
                                // callback
                                callback.onResult();
                                break;
                        }
//                        // Callback
                        callback.onResult();
                    }
                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .build();
        form.load();
    }

}
