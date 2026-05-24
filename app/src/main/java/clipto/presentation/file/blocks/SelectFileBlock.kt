package clipto.presentation.file.blocks

import com.wb.clipboard.databinding.BlockSelectFileBinding
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

    override fun getTitleView(block: View): TextView = BlockSelectFileBinding.bind(block).tvName
    override fun getIconView(block: View): ImageView = BlockSelectFileBinding.bind(block).ivIcon
    override fun getIconTextView(block: View): TextView = BlockSelectFileBinding.bind(block).tvIcon
    override fun getAttrsView(block: View): TextView? = BlockSelectFileBinding.bind(block).tvAttrs
    override fun getProgressImageView(block: View): ImageView = BlockSelectFileBinding.bind(block).ivProgress
    override fun getProgressView(block: View): LinearProgressIndicator = BlockSelectFileBinding.bind(block).lpProgress

    override fun doOnChecked(block: View, file: FileRef, checked: Boolean) {
        val binding = BlockSelectFileBinding.bind(block)
        super.doOnChecked(block, file, checked)
        binding.ivSelected.setVisibleOrGone(checked)
        getTitleView(block).setBold(checked)
    }

}
