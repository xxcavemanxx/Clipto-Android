package clipto.presentation.plan

import android.animation.LayoutTransition
import androidx.fragment.app.activityViewModels
import clipto.analytics.Analytics
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.text.SimpleSpanBuilder
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_select_plan.*

@AndroidEntryPoint
class SelectPlanFragment : MvvmFragment<SelectPlanViewModel>() {

    override val layoutResId: Int = R.layout.fragment_select_plan
    override val viewModel: SelectPlanViewModel by activityViewModels()

    override fun bind(viewModel: SelectPlanViewModel) {
        withDefaults(toolbar, R.string.account_sync_plan_toolbar)

        // policy
        privacyPolicy.setOnClickListener {
            Analytics.onPrivacyPolicy()
            IntentUtils.open(requireContext(), BuildConfig.privacyPolicyUrl)
        }

        // terms
        termsOfService.setOnClickListener {
            Analytics.onTermsOfService()
            IntentUtils.open(requireContext(), BuildConfig.tosUrl)
        }

        // plans
        planView?.layoutTransition?.enableTransitionType(LayoutTransition.CHANGING)

        viewModel.availablePlansLiveData.observe(viewLifecycleOwner) { plans ->
            // warning
            val showWarning = viewModel.canShowWarning()
            if (showWarning) {
                val current = viewModel.getSyncedNotesCount()
                val allowed = viewModel.getSyncLimit()
                warningSubTitle?.text = viewModel.string(R.string.account_sync_plan_warning_limit_reached_sub_title, current, allowed)
            }
            warningTitle?.setVisibleOrGone(showWarning)
            warningSubTitle?.setVisibleOrGone(showWarning)
            warningDescription?.setVisibleOrGone(showWarning)

            // contributor info
            contributorLabel?.setVisibleOrGone(viewModel.isContributorProgramEnabled())

            // plans
            if (plans.size > 1) {
                planChooserSeekBar?.valueTo = (plans.size - 1).toFloat()
            }
            planChooserSeekBar?.addOnChangeListener { _, value, _ ->
                viewModel.onChangeLimit(value.toInt(), plans)
            }

            viewModel.selectedPlanLiveData.removeObservers(viewLifecycleOwner)
            viewModel.selectedPlanLiveData.observe(viewLifecycleOwner) { plan ->
                // progress
                planChooserTitleView?.text = viewModel.string(R.string.account_sync_plan_hint, plan.totalLimit)
                runCatching { plans.indexOf(plan).takeIf { it >= 0 }?.let { planChooserSeekBar?.value = it.toFloat() } }

                // benefits
                planTitleView?.text = SimpleSpanBuilder()
                    .append(viewModel.string(R.string.account_sync_plan_benefit_offline))
                    .append("\n")
                    .append(viewModel.string(R.string.account_sync_plan_benefit_sync, plan.limitTitle))
                    .build()

                // price
                if (plan.skuDetails == null) {
                    planPriceView?.text = plan.debugTitle
                        ?: viewModel.string(R.string.account_sync_plan_free)
                } else {
                    val sku = plan.skuDetails
                    val period = when (sku.subscriptionPeriod) {
                        "P1M" -> viewModel.string(R.string.contribute_monthly_period)
                        else -> viewModel.string(R.string.contribute_annual_period)
                    }
                    planPriceView?.text = viewModel.string(R.string.account_sync_plan_price, sku.price, period)
                }

                // warning
                when {
                    plan.skuDetails == null -> {
                        cancelPlanCaption?.setVisibleOrGone(false)
                    }
                    plan.canBeSelected -> {
                        cancelPlanCaption?.setVisibleOrGone(true)
                    }
                    else -> {
                        cancelPlanCaption?.setVisibleOrGone(false)
                    }
                }
                warningCaption?.text = plan.warning

                // action
                if (plan.isActive) {
                    selectPlanButton?.setText(R.string.account_sync_plan_button_active)
                    selectPlanButton?.isEnabled = false
                    selectPlanButton?.alpha = 0.5f
                } else {
                    selectPlanButton?.setText(R.string.account_sync_plan_button_select)
                    selectPlanButton?.isEnabled = plan.canBeSelected
                    selectPlanButton?.alpha = if (plan.canBeSelected) 1f else 0.5f
                    selectPlanButton?.setDebounceClickListener {
                        activity?.let { act ->
                            viewModel.onSelectPlan(act, plan, plans)
                        }
                    }
                }
            }
        }

        viewModel.onFetchPlans()
        Analytics.screenSelectPlan()
    }

}
