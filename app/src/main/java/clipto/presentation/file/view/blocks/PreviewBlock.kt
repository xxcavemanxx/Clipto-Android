package clipto.presentation.file.view.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.common.extensions.animateScale
import clipto.common.extensions.setDebounceClickListener
import clipto.common.misc.Units
import clipto.domain.FileRef
import clipto.domain.getPreviewBackground
import clipto.extensions.log
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.preview.link.LinkPreview
import clipto.store.files.FileScreenState
import clipto.utils.GlideUtils
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_file_details_preview.view.*
import java.util.*

class PreviewBlock<F : Fragment>(
    private val screenState: FileScreenState,
    private val file: FileRef = screenState.value,
    private val updateDate: Date? = file.updateDate,
    private val fileScreenHelper: FileScreenHelper,
    private val minHeightProvider: (fragment: F) -> Int,
    private val preview: LinkPreview? = fileScreenHelper.getPreview(file),
    private val showHideClickableLayer: (callback: (show: Boolean) -> Unit) -> Unit = {}
) : BlockItem<F>() {

    override val layoutRes: Int = R.layout.block_file_details_preview

    private val size = Units.displayMetrics.widthPixels - Units.DP.toPx(32f).toInt()

    override fun areContentsTheSame(item: BlockItem<F>): Boolean =
        item is PreviewBlock
                && item.preview == preview
                && item.updateDate == updateDate

    override fun onInit(context: F, block: View) {
        block.tvFilePreview.minHeight = minHeightProvider.invoke(context)
        block.ivIcon.setDebounceClickListener {
            val ref = block.tag
            if (ref is PreviewBlock<*>) {
                fileScreenHelper.onPreview(ref.file, ref.preview)
            }
        }
        showHideClickableLayer {
            block.ivPreview?.animateScale(it)
        }
    }

    override fun onBind(context: F, block: View) {
        log("onBind :: preview :: {}", preview)

        block.tag = this
        val previewType = preview?.getType()
        val previewWidth = size
        val previewHeight = size

        // IMAGE
        val iconView = block.ivIcon
        iconView.minimumWidth = previewWidth
        iconView.minimumHeight = previewHeight / 3
        GlideUtils.preview(
            preview = preview,
            view = iconView,
            width = previewWidth,
            height = previewHeight,
            placeholder = file.getPreviewBackground()
        ) {
            iconView.minimumWidth = 0
            iconView.minimumHeight = 0
        }

        // ACTION
        val previewView = block.ivPreview
        if (previewType != null) {
            previewView.setImageResource(previewType.imageRes)
        } else {
            previewView.setImageResource(R.drawable.ic_link_preview_open_in_padded)
        }
    }

}