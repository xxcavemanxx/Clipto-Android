package clipto.presentation.plan

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import clipto.action.CheckUserSessionAction
import clipto.common.extensions.disposeSilently
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.common.presentation.mvvm.lifecycle.UniqueLiveData
import clipto.config.IAppConfig
import clipto.domain.LicenseType
import clipto.extensions.sku
import clipto.repository.IUserRepository
import clipto.store.app.AppState
import clipto.store.purchase.PurchaseManager
import clipto.store.purchase.PurchaseState
import clipto.store.user.UserState
import com.android.billingclient.api.Purchase
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Completable
import io.reactivex.disposables.Disposable
import javax.inject.Inject

@HiltViewModel
class SelectPlanViewModel @Inject constructor(
    app: Application,
    val appConfig: IAppConfig,
    private val appState: AppState,
    private val userState: UserState,
    private val purchaseState: PurchaseState,
    private val userRepository: IUserRepository,
    private val purchaseManager: PurchaseManager,
    private val checkUserSessionAction: CheckUserSessionAction
) : RxViewModel(app) {

    val selectedPlanLiveData: MutableLiveData<PlanDataModel> = UniqueLiveData()
    val availablePlansLiveData: MutableLiveData<List<PlanDataModel>> = SingleLiveData()

    private var purchaseStateDisposable: Disposable? = null
    private var hasActiveSubscribtion = false

    fun isContributorProgramEnabled() = appConfig.syncPlanContributorProgramEnabled()
    fun getSyncedNotesCount() = userState.getSyncedNotesCount()
    fun canShowWarning() = !userState.canSyncNewNotes()
    fun getSyncLimit() = userState.getSyncLimit()

    fun onFetchPlans() {
        val defaultPlan: PlanDataModel
        val plans = mutableListOf<PlanDataModel>()
        val user = userState.user.requireValue()
        val freeLimit = userState.getSyncFreeLimit() + userState.getSyncBonusForPublicKits()
        if (user.license.isSubscriptionPlan()) {
            val limit = user.syncLimit
            defaultPlan = PlanDataModel(
                limit = limit,
                totalLimit = userState.getSyncLimit(),
                canBeSelected = false,
                limitTitle = "+${limit}"
            )
        } else {
            val limit = userState.getSyncLimit()
            defaultPlan = PlanDataModel(
                limit = limit,
                totalLimit = limit,
                canBeSelected = false,
                limitTitle = limit.toString(),
                warning = string(R.string.sync_free_limit_concept)
            )
        }
        val notesCount = getSyncedNotesCount()
        purchaseState.fetchActiveSubscriptions().getLiveChanges()
            .filter { it.isNotNull() }
            .firstElement()
            .map { it.value!! }
            .flatMapCompletable { activeSubs ->
                hasActiveSubscribtion = activeSubs.find { it.purchaseToken == user.syncSubscriptionToken } != null

                val skuMatrix = appConfig.syncPlans()
                var skuList = emptyList<String>()

                val syncSubscriptionId = user.syncSubscriptionId
                if (syncSubscriptionId != null) {
                    val skus = skuMatrix.find { it.contains(syncSubscriptionId) }
                    if (skus != null) {
                        skuList = skus
                    }
                }
                if (skuList.isEmpty() && activeSubs.isNotEmpty()) {
                    for (skus in skuMatrix) {
                        if (skus.find { sku -> activeSubs.any { it.sku() == sku } } == null) {
                            skuList = skus
                            break
                        }
                    }
                }
                if (skuList.isEmpty()) {
                    skuList = skuMatrix.first()
                }

                log("fetch plans: {}", skuList)

                purchaseState.fetchPlans(skuList).getLiveChanges()
                    .filter { it.isNotNull() }
                    .firstElement()
                    .map { it.value!! }
                    .flatMapCompletable {
                        it.forEach { sku ->
                            val id = sku.sku
                            val limit = getSkuLimit(id)
                            if (limit != null) {
                                log("add sku: {} -> {}", limit, id)
                                val totalLimit = limit + freeLimit
                                val isActive = id == user.syncSubscriptionId
                                val canBeSelected = notesCount <= totalLimit
                                val warning = if (canBeSelected) null else string(R.string.account_sync_plan_warning_limit_reached_caption)
                                plans.add(
                                    PlanDataModel(
                                        limit = limit,
                                        skuDetails = sku,
                                        limitTitle = "+${limit}",
                                        totalLimit = totalLimit,
                                        isActive = isActive,
                                        canBeSelected = canBeSelected,
                                        warning = warning
                                    )
                                )
                            }
                        }

                        plans.sortBy { it.totalLimit }

                        var plan = plans.find { it.isActive }
                        if (plan == null) {
                            val recommendedLimit = appConfig.syncPlanNotesRecommendedLimit()
                            if (recommendedLimit > 0) {
                                plan = plans.find { it.canBeSelected && it.limit >= recommendedLimit }
                            }
                            log("recommended plan: {} -> {}", recommendedLimit, plan)
                        }
                        if (plan == null) {
                            defaultPlan.isActive = true
                            plans.add(0, defaultPlan)
                            plan = defaultPlan
                        }

                        if (plans.isNotEmpty()) {
                            availablePlansLiveData.postValue(plans)
                            selectedPlanLiveData.postValue(plan)
                        }

                        Completable.complete()
                    }
            }
            .subscribeBy("onFetchPlans", appState)
    }

    fun onSelectPlan(activity: FragmentActivity, plan: PlanDataModel, plans: List<PlanDataModel>) {
        appState.setLoadingState()
        val user = userState.user.requireValue()
        plan.skuDetails?.let { sku ->
            log("onSelectPlan: {}", sku)
            val prevToken = user.syncSubscriptionToken
            purchaseState.purchaseUpdate.clearValue()
            if (prevToken != null && hasActiveSubscribtion) {
                purchaseManager.startPaymentFlow(activity, sku, prevToken)
            } else {
                purchaseManager.startPaymentFlow(activity, sku)
            }
            purchaseStateDisposable.disposeSilently()
            purchaseStateDisposable = purchaseState.purchaseUpdate.getLiveChanges()
                .filter { it.isNotNull() }
                .map { it.value!!.purchases }
                .filter { purchaseList -> purchaseList.find { it.sku() == sku.sku } != null }
                .firstElement()
                .subscribeBy {
                    onConfirmPlan(plan, plans, it)
                }
        }
    }

    fun onChangeLimit(progress: Int, plans: List<PlanDataModel>) {
        val plan = plans.getOrElse(progress) { plans.last() }
        log("onChangeLimit: {} -> {}", progress, plan)
        selectedPlanLiveData.postValue(plan)
    }

    private fun onConfirmPlan(plan: PlanDataModel, plans: List<PlanDataModel>, purchaseList: List<Purchase>) {
        log("onConfirmPlan: {} -> {}", plan, purchaseList)
        val purchase = purchaseList.find { it.sku() == plan.skuDetails?.sku }
        if (purchase != null) {
            appState.setLoadingState()
            val user = userState.user.requireValue()
            getSkuLimit(purchase.sku())?.let { user.syncLimit = it }
            user.syncSubscriptionToken = purchase.purchaseToken
            user.license = LicenseType.SUBSCRIPTION
            user.syncSubscriptionId = purchase.sku()
            user.syncIsRestricted = false
            userRepository.update(user)
                .subscribeBy("onConfirmPlan") {
                    checkUserSessionAction.execute(force = true)
                }
            plans.forEach { it.isActive = false }
            plan.isActive = true
            availablePlansLiveData.postValue(plans)
            hasActiveSubscribtion = true
            appState.setLoadedState()
        }
    }

    private fun getSkuLimit(id: String?): Int? = id
        ?.substring(id.lastIndexOf("_") + 1)
        ?.toIntOrNull()

}