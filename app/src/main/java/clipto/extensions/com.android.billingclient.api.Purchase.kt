package clipto.extensions

import com.android.billingclient.api.Purchase

fun Purchase.sku():String? = skus.firstOrNull()
