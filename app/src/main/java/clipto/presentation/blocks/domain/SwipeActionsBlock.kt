package clipto.presentation.blocks.domain

import com.wb.clipboard.databinding.BlockSwipeActionsBinding
import android.animation.LayoutTransition
import android.view.View
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.ThemeUtils
import clipto.domain.SwipeAction
import clipto.extensions.toColor
import clipto.extensions.toIcon
import clipto.extensions.toTitle
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueString
import clipto.store.app.AppState
import clipto.store.main.MainState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wb.clipboard.R

class SwipeActionsBlock<C>(
    private val appState: AppState,
    private val mainState: MainState
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_swipe_actions

    override fun onBind(context: C, block: View) {
        val binding = BlockSwipeActionsBinding.bind(block)
        val ctx = block.context
        val settings = appState.getSettings()

        val colorKey = ThemeUtils.getColor(ctx, android.R.attr.textColorPrimary)
        val colorValue = ThemeUtils.getColor(ctx, android.R.attr.textColorSecondary)
        val swipeActions = SwipeAction.values()
        val swipeActionOptions = swipeActions.map { ctx.getString(it.toTitle()) }.toTypedArray()

        val updateStubIcon: (updateSettings: Boolean) -> Unit = {
            binding.leftActionStubIcon?.setVisibleOrGone(
                settings.swipeActionLeft != SwipeAction.COPY
                        && settings.swipeActionRight != SwipeAction.COPY
            )
            if (it) {
                appState.refreshSettings()
            }
        }
        updateStubIcon(false)

        val updateRightState: () -> Unit = {
            val visible = settings.swipeActionRight != SwipeAction.NONE
            binding.rightActionTextStub?.setBackgroundResource(if (visible) R.drawable.bg_stub_rounded_left else R.drawable.bg_stub_rounded)
            binding.rightActionBackground?.setVisibleOrGone(visible)
            binding.rightActionIcon?.setVisibleOrGone(visible)
            binding.rightActionEnd?.setVisibleOrGone(!visible)
        }

        val updateLeftState: () -> Unit = {
            val visible = settings.swipeActionLeft != SwipeAction.NONE
            val backgroundStubRes = if (visible) R.drawable.bg_stub_rounded_right else R.drawable.bg_stub_rounded
            binding.leftActionTextStub?.setBackgroundResource(backgroundStubRes)
            binding.leftActionLeftStub?.setBackgroundResource(backgroundStubRes)
            binding.leftActionBackground?.setVisibleOrGone(visible)
            binding.leftActionIcon?.setVisibleOrGone(visible)
            binding.leftActionStart?.setVisibleOrGone(!visible)
        }

        val rightTitle = KeyValueString(
            binding.rightActionTitleView,
            "\n",
            colorKey,
            colorValue
        )
        rightTitle.setKey(R.string.main_swipe_actions_caption_right)
        rightTitle.setValue(settings.swipeActionRight.toTitle())
        binding.rightActionIcon.setImageResource(settings.swipeActionRight.toIcon())
        binding.rightActionBackground.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
        binding.rightActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        updateRightState()
        binding.rightSwipe.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(ctx)
            builder.setTitle(R.string.main_swipe_actions_caption_right)
            val selectedIndex = swipeActions.indexOfFirst { it == settings.swipeActionRight }
            builder.setSingleChoiceItems(swipeActionOptions, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val selected = swipeActions[which]
                if (selected != settings.swipeActionRight) {
                    settings.swipeActionRight = selected
                    mainState.requestUpdateSwipeActions(settings)
                    rightTitle.setValue(settings.swipeActionRight.toTitle())
                    binding.rightActionIcon?.setImageResource(settings.swipeActionRight.toIcon())
                    binding.rightActionBackground?.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
                    updateStubIcon(true)
                    updateRightState()
                }
            }
            builder.show()
        }

        val leftTitle = KeyValueString(
            binding.leftActionTitleView,
            "\n",
            colorKey,
            colorValue
        )
        leftTitle.setKey(R.string.main_swipe_actions_caption_left)
        leftTitle.setValue(settings.swipeActionLeft.toTitle())
        binding.leftActionIcon.setImageResource(settings.swipeActionLeft.toIcon())
        binding.leftActionBackground.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
        binding.leftActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        updateLeftState()
        binding.leftSwipe.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(ctx)
            builder.setTitle(R.string.main_swipe_actions_caption_left)
            val selectedIndex = swipeActions.indexOfFirst { it == settings.swipeActionLeft }
            builder.setSingleChoiceItems(swipeActionOptions, selectedIndex) { dialog, which ->
                dialog.dismiss()
                val selected = swipeActions[which]
                if (selected != settings.swipeActionLeft) {
                    settings.swipeActionLeft = selected
                    mainState.requestUpdateSwipeActions(settings)
                    val visible = selected != SwipeAction.NONE
                    binding.leftActionBackground?.setVisibleOrGone(visible)
                    binding.leftActionIcon?.setVisibleOrGone(visible)
                    leftTitle.setValue(settings.swipeActionLeft.toTitle())
                    binding.leftActionIcon?.setImageResource(settings.swipeActionLeft.toIcon())
                    binding.leftActionBackground?.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
                    updateStubIcon(true)
                    updateLeftState()
                }
            }
            builder.show()
        }
    }

}
