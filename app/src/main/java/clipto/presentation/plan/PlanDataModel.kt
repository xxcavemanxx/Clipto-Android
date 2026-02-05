package clipto.presentation.plan

import com.android.billingclient.api.SkuDetails

data class PlanDataModel(
        val limit: Int,
        val totalLimit:Int,
        val limitTitle:String,
        var isActive: Boolean = false,
        val canBeSelected: Boolean = false,
        val skuDetails: SkuDetails? = null,
        val debugTitle: String? = null,
        val warning:CharSequence? = null
)