package clipto.presentation.blocks.domain

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
import kotlinx.android.synthetic.main.block_swipe_actions.view.*

class SwipeActionsBlock<C>(
    private val appState: AppState,
    private val mainState: MainState
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_swipe_actions

    override fun onBind(context: C, block: View) {
        val ctx = block.context
        val settings = appState.getSettings()

        val colorKey = ThemeUtils.getColor(ctx, android.R.attr.textColorPrimary)
        val colorValue = ThemeUtils.getColor(ctx, android.R.attr.textColorSecondary)
        val swipeActions = SwipeAction.values()
        val swipeActionOptions = swipeActions.map { ctx.getString(it.toTitle()) }.toTypedArray()

        val updateStubIcon: (updateSettings: Boolean) -> Unit = {
            block.leftActionStubIcon?.setVisibleOrGone(
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
            block.rightActionTextStub?.setBackgroundResource(if (visible) R.drawable.bg_stub_rounded_left else R.drawable.bg_stub_rounded)
            block.rightActionBackground?.setVisibleOrGone(visible)
            block.rightActionIcon?.setVisibleOrGone(visible)
            block.rightActionEnd?.setVisibleOrGone(!visible)
        }

        val updateLeftState: () -> Unit = {
            val visible = settings.swipeActionLeft != SwipeAction.NONE
            val backgroundStubRes = if (visible) R.drawable.bg_stub_rounded_right else R.drawable.bg_stub_rounded
            block.leftActionTextStub?.setBackgroundResource(backgroundStubRes)
            block.leftActionLeftStub?.setBackgroundResource(backgroundStubRes)
            block.leftActionBackground?.setVisibleOrGone(visible)
            block.leftActionIcon?.setVisibleOrGone(visible)
            block.leftActionStart?.setVisibleOrGone(!visible)
        }

        val rightTitle = KeyValueString(
            block.rightActionTitleView,
            "\n",
            colorKey,
            colorValue
        )
        rightTitle.setKey(R.string.main_swipe_actions_caption_right)
        rightTitle.setValue(settings.swipeActionRight.toTitle())
        block.rightActionIcon.setImageResource(settings.swipeActionRight.toIcon())
        block.rightActionBackground.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
        block.rightActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        updateRightState()
        block.rightSwipe.setOnClickListener {
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
                    block.rightActionIcon?.setImageResource(settings.swipeActionRight.toIcon())
                    block.rightActionBackground?.setBackgroundColor(settings.swipeActionRight.toColor(ctx))
                    updateStubIcon(true)
                    updateRightState()
                }
            }
            builder.show()
        }

        val leftTitle = KeyValueString(
            block.leftActionTitleView,
            "\n",
            colorKey,
            colorValue
        )
        leftTitle.setKey(R.string.main_swipe_actions_caption_left)
        leftTitle.setValue(settings.swipeActionLeft.toTitle())
        block.leftActionIcon.setImageResource(settings.swipeActionLeft.toIcon())
        block.leftActionBackground.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
        block.leftActionSettings.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        updateLeftState()
        block.leftSwipe.setOnClickListener {
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
                    block.leftActionBackground?.setVisibleOrGone(visible)
                    block.leftActionIcon?.setVisibleOrGone(visible)
                    leftTitle.setValue(settings.swipeActionLeft.toTitle())
                    block.leftActionIcon?.setImageResource(settings.swipeActionLeft.toIcon())
                    block.leftActionBackground?.setBackgroundColor(settings.swipeActionLeft.toColor(ctx))
                    updateStubIcon(true)
                    updateLeftState()
                }
            }
            builder.show()
        }
    }

}