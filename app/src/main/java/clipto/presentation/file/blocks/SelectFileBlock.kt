package clipto.presentation.file.blocks

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.common.extensions.setBold
import clipto.common.extensions.setVisibleOrGone
import clipto.domain.FileRef
import clipto.domain.ListConfig
import clipto.presentation.file.FileScreenHelper
import clipto.presentation.main.list.blocks.FileItemBlock
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_select_file.view.*

class SelectFileBlock<C>(
    file: FileRef,
    listConfigGetter: () -> ListConfig,
    fileScreenHelper: FileScreenHelper,
    isSelectedGetter: (file: FileRef) -> Boolean,
    onFileClicked: (file: FileRef) -> Unit,
    onFileIconClicked: (file: FileRef) -> Unit,
    onFileChanged: (callback: (file: FileRef) -> Unit) -> Unit,
    highlight: String? = null
) : FileItemBlock<C>(
    file = file,
    listConfigGetter = listConfigGetter,
    fileScreenHelper = fileScreenHelper,
    isSelectedGetter = isSelectedGetter,
    onFileClicked = onFileClicked,
    onFileIconClicked = onFileIconClicked,
    onFileChanged = onFileChanged,
    highlight = highlight
) {

    override val layoutRes: Int = R.layout.block_select_file

    override fun getTitleView(block: View): TextView = block.tvName
    override fun getIconView(block: View): ImageView = block.ivIcon
    override fun getIconTextView(block: View): TextView = block.tvIcon
    override fun getAttrsView(block: View): TextView? = block.tvAttrs
    override fun getProgressImageView(block: View): ImageView = block.ivProgress
    override fun getProgressView(block: View): LinearProgressIndicator = block.lpProgress

    override fun doOnChecked(block: View, file: FileRef, checked: Boolean) {
        super.doOnChecked(block, file, checked)
        block.ivSelected.setVisibleOrGone(checked)
        getTitleView(block).setBold(checked)
    }

}