package clipto.presentation.settings.swipeactions

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
import kotlinx.android.synthetic.main.fragment_settings_swipe_actions.*

@AndroidEntryPoint
class SwipeActionsFragment : MvvmFragment<SwipeActionsViewModel>() {

    override val layoutResId: Int = R.layout.fragment_settings_swipe_actions
    override val viewModel: SwipeActionsViewModel by viewModels()

    override fun bind(viewModel: SwipeActionsViewModel) {
        val ctx = requireContext()
        val settings = viewModel.settings
        val colorKey = ThemeUtils.getColor(ctx, android.R.attr.textColorPrimary)
        val colorValue = ThemeUtils.getColor(ctx, android.R.attr.textColorSecondary)
        val swipeActions = SwipeAction.values()
        val swipeActionOptions = swipeActions.map { ctx.getString(it.toTitle()) }.toTypedArray()

        withDefaults(toolbar, R.string.settings_swipe_actions_title)
        updateStubIcon()

        val rightTitle = KeyValueString(
                rightActionTitleView,
                "\n",
                colorKey,
                colorValue)
        rightTitle.setKey(R.string.main_swipe_actions_caption_right)
        rightTitle.setValue(settings.swipeActionRight.toTitle())
        rightActionIcon.setImageResource(settings.swipeActionRight.toIcon())
        rightActionBackground.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
        rightActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        rightActionCard.setVisibleOrGone(settings.swipeActionRight != SwipeAction.NONE)
        rightSwipe.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(ctx)
            builder.setTitle(R.string.main_swipe_actions_caption_right)
            val selectedIndex = swipeActions.indexOfFirst { it == settings.swipeActionRight }
            builder.setSingleChoiceItems(swipeActionOptions, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val selected = swipeActions[which]
                if (selected != settings.swipeActionRight) {
                    settings.swipeActionRight = selected
                    rightActionCard?.setVisibleOrGone(selected != SwipeAction.NONE)
                    rightTitle.setValue(settings.swipeActionRight.toTitle())
                    rightActionIcon?.setImageResource(settings.swipeActionRight.toIcon())
                    rightActionBackground?.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
                    updateStubIcon()
                }
            }
            builder.show()
        }

        val leftTitle = KeyValueString(
                leftActionTitleView,
                "\n",
                colorKey,
                colorValue)
        leftTitle.setKey(R.string.main_swipe_actions_caption_left)
        leftTitle.setValue(settings.swipeActionLeft.toTitle())
        leftActionIcon.setImageResource(settings.swipeActionLeft.toIcon())
        leftActionBackground.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
        leftActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        leftActionCard.setVisibleOrGone(settings.swipeActionLeft != SwipeAction.NONE)
        leftSwipe.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(ctx)
            builder.setTitle(R.string.main_swipe_actions_caption_left)
            val selectedIndex = swipeActions.indexOfFirst { it == settings.swipeActionLeft }
            builder.setSingleChoiceItems(swipeActionOptions, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val selected = swipeActions[which]
                if (selected != settings.swipeActionLeft) {
                    settings.swipeActionLeft = selected
                    leftActionCard?.setVisibleOrGone(selected != SwipeAction.NONE)
                    leftTitle.setValue(settings.swipeActionLeft.toTitle())
                    leftActionIcon?.setImageResource(settings.swipeActionLeft.toIcon())
                    leftActionBackground?.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
                    updateStubIcon()
                }
            }
            builder.show()
        }

        Analytics.screenSwipeActions()
    }

    private fun updateStubIcon() {
        val settings = AppContext.get().getSettings()
        leftActionStubIcon?.setVisibleOrGone(settings.swipeActionLeft != SwipeAction.COPY
                && settings.swipeActionRight != SwipeAction.COPY)
    }

}
