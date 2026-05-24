package clipto.presentation.settings.swipeactions
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSettingsSwipeActionsBinding
import android.animation.LayoutTransition
import androidx.fragment.app.viewModels
import clipto.AppContext
import clipto.analytics.Analytics
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.ThemeUtils
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.domain.SwipeAction
import clipto.extensions.toColor
import clipto.extensions.toIcon
import clipto.extensions.toTitle
import clipto.presentation.common.text.KeyValueString
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SwipeActionsFragment : MvvmFragment<SwipeActionsViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsSwipeActionsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSettingsSwipeActionsBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_settings_swipe_actions
    override val viewModel: SwipeActionsViewModel by viewModels()

    override fun bind(viewModel: SwipeActionsViewModel) {
        val ctx = requireContext()
        val settings = viewModel.settings
        val colorKey = ThemeUtils.getColor(ctx, android.R.attr.textColorPrimary)
        val colorValue = ThemeUtils.getColor(ctx, android.R.attr.textColorSecondary)
        val swipeActions = SwipeAction.values()
        val swipeActionOptions = swipeActions.map { ctx.getString(it.toTitle()) }.toTypedArray()

        withDefaults(binding.toolbar, R.string.settings_swipe_actions_title)
        updateStubIcon()

        val rightTitle = KeyValueString(
                binding.rightActionTitleView,
                "\n",
                colorKey,
                colorValue)
        rightTitle.setKey(R.string.main_swipe_actions_caption_right)
        rightTitle.setValue(settings.swipeActionRight.toTitle())
        binding.rightActionIcon.setImageResource(settings.swipeActionRight.toIcon())
        binding.rightActionBackground.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
        binding.rightActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.rightActionCard.setVisibleOrGone(settings.swipeActionRight != SwipeAction.NONE)
        binding.rightSwipe.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(ctx)
            builder.setTitle(R.string.main_swipe_actions_caption_right)
            val selectedIndex = swipeActions.indexOfFirst { it == settings.swipeActionRight }
            builder.setSingleChoiceItems(swipeActionOptions, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val selected = swipeActions[which]
                if (selected != settings.swipeActionRight) {
                    settings.swipeActionRight = selected
                    binding.rightActionCard?.setVisibleOrGone(selected != SwipeAction.NONE)
                    rightTitle.setValue(settings.swipeActionRight.toTitle())
                    binding.rightActionIcon?.setImageResource(settings.swipeActionRight.toIcon())
                    binding.rightActionBackground?.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
                    updateStubIcon()
                }
            }
            builder.show()
        }

        val leftTitle = KeyValueString(
                binding.leftActionTitleView,
                "\n",
                colorKey,
                colorValue)
        leftTitle.setKey(R.string.main_swipe_actions_caption_left)
        leftTitle.setValue(settings.swipeActionLeft.toTitle())
        binding.leftActionIcon.setImageResource(settings.swipeActionLeft.toIcon())
        binding.leftActionBackground.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
        binding.leftActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.leftActionCard.setVisibleOrGone(settings.swipeActionLeft != SwipeAction.NONE)
        binding.leftSwipe.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(ctx)
            builder.setTitle(R.string.main_swipe_actions_caption_left)
            val selectedIndex = swipeActions.indexOfFirst { it == settings.swipeActionLeft }
            builder.setSingleChoiceItems(swipeActionOptions, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val selected = swipeActions[which]
                if (selected != settings.swipeActionLeft) {
                    settings.swipeActionLeft = selected
                    binding.leftActionCard?.setVisibleOrGone(selected != SwipeAction.NONE)
                    leftTitle.setValue(settings.swipeActionLeft.toTitle())
                    binding.leftActionIcon?.setImageResource(settings.swipeActionLeft.toIcon())
                    binding.leftActionBackground?.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
                    updateStubIcon()
                }
            }
            builder.show()
        }

        Analytics.screenSwipeActions()
    }

    private fun updateStubIcon() {
        val settings = AppContext.get().getSettings()
        binding.leftActionStubIcon?.setVisibleOrGone(settings.swipeActionLeft != SwipeAction.COPY
                && settings.swipeActionRight != SwipeAction.COPY)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
