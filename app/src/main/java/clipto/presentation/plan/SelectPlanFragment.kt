package clipto.presentation.plan
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSelectPlanBinding
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

@AndroidEntryPoint
class SelectPlanFragment : MvvmFragment<SelectPlanViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelectPlanBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSelectPlanBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_select_plan
    override val viewModel: SelectPlanViewModel by activityViewModels()

    override fun bind(viewModel: SelectPlanViewModel) {
        withDefaults(binding.toolbar, R.string.account_sync_plan_toolbar)

        // policy
        binding.privacyPolicy.setOnClickListener {
            Analytics.onPrivacyPolicy()
            IntentUtils.open(requireContext(), BuildConfig.privacyPolicyUrl)
        }

        // terms
        binding.termsOfService.setOnClickListener {
            Analytics.onTermsOfService()
            IntentUtils.open(requireContext(), BuildConfig.tosUrl)
        }

        // plans
        binding.planView?.layoutTransition?.enableTransitionType(LayoutTransition.CHANGING)

        viewModel.availablePlansLiveData.observe(viewLifecycleOwner) { plans ->
            // warning
            val showWarning = viewModel.canShowWarning()
            if (showWarning) {
                val current = viewModel.getSyncedNotesCount()
                val allowed = viewModel.getSyncLimit()
                binding.warningSubTitle?.text = viewModel.string(R.string.account_sync_plan_warning_limit_reached_sub_title, current, allowed)
            }
            binding.warningTitle?.setVisibleOrGone(showWarning)
            binding.warningSubTitle?.setVisibleOrGone(showWarning)
            binding.warningDescription?.setVisibleOrGone(showWarning)

            // contributor info
            binding.contributorLabel?.setVisibleOrGone(viewModel.isContributorProgramEnabled())

            // plans
            if (plans.size > 1) {
                binding.planChooserSeekBar?.valueTo = (plans.size - 1).toFloat()
            }
            binding.planChooserSeekBar?.addOnChangeListener { _, value, _ ->
                viewModel.onChangeLimit(value.toInt(), plans)
            }

            viewModel.selectedPlanLiveData.removeObservers(viewLifecycleOwner)
            viewModel.selectedPlanLiveData.observe(viewLifecycleOwner) { plan ->
                // progress
                binding.planChooserTitleView?.text = viewModel.string(R.string.account_sync_plan_hint, plan.totalLimit)
                runCatching { plans.indexOf(plan).takeIf { it >= 0 }?.let { binding.planChooserSeekBar?.value = it.toFloat() } }

                // benefits
                binding.planTitleView?.text = SimpleSpanBuilder()
                    .append(viewModel.string(R.string.account_sync_plan_benefit_offline))
                    .append("\n")
                    .append(viewModel.string(R.string.account_sync_plan_benefit_sync, plan.limitTitle))
                    .build()

                // price
                if (plan.skuDetails == null) {
                    binding.planPriceView?.text = plan.debugTitle
                        ?: viewModel.string(R.string.account_sync_plan_free)
                } else {
                    val sku = plan.skuDetails
                    val period = when (sku.subscriptionPeriod) {
                        "P1M" -> viewModel.string(R.string.contribute_monthly_period)
                        else -> viewModel.string(R.string.contribute_annual_period)
                    }
                    binding.planPriceView?.text = viewModel.string(R.string.account_sync_plan_price, sku.price, period)
                }

                // warning
                when {
                    plan.skuDetails == null -> {
                        binding.cancelPlanCaption?.setVisibleOrGone(false)
                    }
                    plan.canBeSelected -> {
                        binding.cancelPlanCaption?.setVisibleOrGone(true)
                    }
                    else -> {
                        binding.cancelPlanCaption?.setVisibleOrGone(false)
                    }
                }
                binding.warningCaption?.text = plan.warning

                // action
                if (plan.isActive) {
                    binding.selectPlanButton?.setText(R.string.account_sync_plan_button_active)
                    binding.selectPlanButton?.isEnabled = false
                    binding.selectPlanButton?.alpha = 0.5f
                } else {
                    binding.selectPlanButton?.setText(R.string.account_sync_plan_button_select)
                    binding.selectPlanButton?.isEnabled = plan.canBeSelected
                    binding.selectPlanButton?.alpha = if (plan.canBeSelected) 1f else 0.5f
                    binding.selectPlanButton?.setDebounceClickListener {
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
