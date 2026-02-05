package clipto.presentation.blocks

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import clipto.AppContext
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.common.misc.IntentUtils
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.internet.InternetState
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_copied_link.view.*

class CopiedLinkBlock<C>(
    private val link: String,
    private val label: CharSequence = link,
    private val centered: Boolean = false,
    private val canBeOpened: Boolean = true,
    private val canBeCopied: Boolean = true,
    private val internetState: InternetState,
    private val onOpened: (link: String) -> Unit = {}
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_copied_link

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is CopiedLinkBlock
                && link == item.link
                && label == item.label
                && centered == item.centered
                && canBeOpened == item.canBeOpened
                && canBeCopied == item.canBeCopied

    override fun onInit(context: C, block: View) {
        block.copyLinkAction.setDebounceClickListener {
            val ref = block.tag
            if (ref is CopiedLinkBlock<*>) {
                ref.onCopy()
            }
        }
        block.linkButton.setDebounceClickListener {
            val ref = block.tag
            if (ref is CopiedLinkBlock<*>) {
                ref.onOpen()
            }
        }
    }

    override fun onBind(context: C, block: View) {
        block.tag = this
        block.linkButton.text = label
        block.linkButton.isClickable = canBeOpened
        block.copyLinkAction.setVisibleOrGone(canBeCopied)
        val params = block.linkButton.layoutParams
        if (params is ConstraintLayout.LayoutParams) {
            params.horizontalBias = if (centered) 0.5f else 0.0f
            block.linkButton.layoutParams = params
        }
    }

    private fun onCopy() {
        AppContext.get().onCopy(link)
    }

    private fun onOpen() {
        internetState.withInternet(
            success = {
                onOpened.invoke(link)
                IntentUtils.open(AppContext.get().app, link)
            }
        )
    }

}