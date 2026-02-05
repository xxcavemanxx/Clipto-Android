package clipto.store.purchase

import android.app.Application
import clipto.common.extensions.disposeSilently
import clipto.common.logging.L
import clipto.config.IAppConfig
import clipto.store.StoreObject
import clipto.store.StoreState
import com.android.billingclient.api.*
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PurchaseState @Inject constructor(
    app: Application,
    appConfig: IAppConfig
) : StoreState(appConfig), PurchasesUpdatedListener, AcknowledgePurchaseResponseListener, ConsumeResponseListener {

    private val billingClient = lazy { BillingClient.newBuilder(app).enablePendingPurchases().setListener(this).build() }
    private var reconnectDisposable: Disposable? = null

    val subs by lazy { StoreObject<List<Purchase>>("subs", emptyList()) }
    val purchaseUpdate by lazy {
        StoreObject<PurchaseUpdate>(
            id = "purchase_update",
            onChanged = { _, newValue ->
                subs.setValue(newValue?.purchases ?: emptyList())
            }
        )
    }

    fun onClientReady(retryCount: Int = 20, callback: (client: BillingClient) -> Unit) {
        val client = billingClient.value
        if (client.isReady) {
            reconnectDisposable.disposeSilently()
            callback.invoke(client)
        } else {
            billingClient.value.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        callback.invoke(client)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    if (retryCount > 0) {
                        reconnectDisposable.disposeSilently()
                        reconnectDisposable = getViewScheduler().scheduleDirect(
                            { onClientReady(retryCount - 1, callback) },
                            appConfig.getUiTimeout(),
                            TimeUnit.MILLISECONDS
                        )
                    }
                }
            })
        }
    }

    fun fetchActiveSubscriptions(): StoreObject<List<Purchase>> {
        val activeSubscriptions = StoreObject<List<Purchase>>("active_subscriptions")
        L.log(this, "fetchActiveSubscriptions")
        onClientReady { client ->
            client.queryPurchasesAsync(BillingClient.SkuType.SUBS) { subsResult, purchasesList ->
                if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchasesList.addAll(purchasesList)
                    subs.setValue(purchasesList)
                    activeSubscriptions.setValue(purchasesList)
                }
            }
        }
        return activeSubscriptions
    }

    fun fetchPlans(plans: List<String>): StoreObject<List<SkuDetails>> {
        val skuPlans = StoreObject<List<SkuDetails>>("plans")
        L.log(this, "fetchPlans")
        onClientReady { client ->
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(plans).setType(BillingClient.SkuType.SUBS)
            client.querySkuDetailsAsync(params.build()) { _, skuDetailsList ->
                skuDetailsList?.let { skuPlans.setValue(it) }
            }
        }
        return skuPlans
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        L.log(this, "onPurchasesUpdated: responseCode={}, purchases={}", billingResult.responseCode, purchases)
        val list = purchases ?: emptyList()
        acknowledge(list)
        subs.setValue(list)
        purchaseUpdate.setValue(PurchaseUpdate(billingResult, list))
    }

    private fun acknowledge(list: List<Purchase>?) {
        onClientReady { client ->
            runCatching {
                list?.filter { !it.isAcknowledged }?.forEach {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(it.purchaseToken)
                        .build()
                    L.log(this, "acknowledge: {}", it)
                    client.acknowledgePurchase(params, this)
                }
            }
        }
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        L.log(this, "onAcknowledgePurchaseResponse: responseCode={}", billingResult.responseCode)
        onClientReady { client ->
            client.queryPurchasesAsync(BillingClient.SkuType.SUBS) { billingResult, list ->
                val purchasesList = list.filter { it.isAcknowledged }
                if (!purchasesList.isNullOrEmpty()) {
                    onPurchasesUpdated(billingResult, purchasesList)
                }
            }
        }
    }

    override fun onConsumeResponse(result: BillingResult, string: String) {
        L.log(this, "onConsumeResponse: responseCode={}", result.responseCode)
    }

    data class PurchaseUpdate(
        val billingResult: BillingResult,
        val purchases: List<Purchase>
    )

}