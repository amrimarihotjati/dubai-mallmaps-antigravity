package uk.dubai.mall.maps.construction.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import uk.dubai.mall.maps.construction.R

@Composable
fun NativeAdComponent(
    adUnitId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad: NativeAd ->
                nativeAd = ad
                isAdLoaded = true
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    if (isAdLoaded && nativeAd != null) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                val adView = LayoutInflater.from(ctx)
                    .inflate(R.layout.ad_native, null) as NativeAdView
                
                // Set views
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)
                adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)
                adView.advertiserView = adView.findViewById(R.id.ad_advertiser)

                // Populate views
                val ad = nativeAd!!
                (adView.headlineView as TextView).text = ad.headline
                
                if (ad.body == null) {
                    adView.bodyView?.visibility = android.view.View.INVISIBLE
                } else {
                    adView.bodyView?.visibility = android.view.View.VISIBLE
                    (adView.bodyView as TextView).text = ad.body
                }

                if (ad.callToAction == null) {
                    adView.callToActionView?.visibility = android.view.View.INVISIBLE
                } else {
                    adView.callToActionView?.visibility = android.view.View.VISIBLE
                    (adView.callToActionView as Button).text = ad.callToAction
                }

                if (ad.icon == null) {
                    adView.iconView?.visibility = android.view.View.GONE
                } else {
                    (adView.iconView as ImageView).setImageDrawable(ad.icon?.drawable)
                    adView.iconView?.visibility = android.view.View.VISIBLE
                }

                if (ad.advertiser == null) {
                    adView.advertiserView?.visibility = android.view.View.INVISIBLE
                } else {
                    (adView.advertiserView as TextView).text = ad.advertiser
                    adView.advertiserView?.visibility = android.view.View.VISIBLE
                }

                adView.setNativeAd(ad)
                adView
            },
            update = { view ->
                // Called when view is recomposed
            }
        )
    }
}
