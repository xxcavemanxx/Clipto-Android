package clipto.store.purchase

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import clipto.analytics.Analytics
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.SkuDetails
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseManager @Inject constructor(private val purchaseState: PurchaseState) {

    fun startPaymentFlow(activity: FragmentActivity, sku: SkuDetails) {
        purchaseState.onClientReady { client ->
            val flowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(sku)
                    .build()
            client.launchBillingFlow(safeActivity(activity), flowParams)
        }
    }

    fun startPaymentFlow(activity: FragmentActivity, sku: SkuDetails, purchaseToken: String) {
        purchaseState.onClientReady { client ->
            val flowParams = BillingFlowParams.newBuilder()
                    .setSubscriptionUpdateParams(
                            BillingFlowParams.SubscriptionUpdateParams
                                    .newBuilder()
                                    .setOldSkuPurchaseToken(purchaseToken)
                                    .build()
                    )
                    .setSkuDetails(sku)
                    .build()
            client.launchBillingFlow(safeActivity(activity), flowParams)
        }
    }

    private fun safeActivity(activity: FragmentActivity): FragmentActivity {
        if (activity.intent == null) {
            Analytics.errorWrongActivityStartIntent()
            activity.intent = Intent(activity, activity.javaClass)
        }
        return activity
    }

}