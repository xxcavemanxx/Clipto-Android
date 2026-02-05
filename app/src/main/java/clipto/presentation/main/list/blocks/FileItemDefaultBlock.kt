package clipto.presentation.main.list.blocks

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import clipto.common.extensions.setVisibleOrGone
import clipto.domain.FileRef
import clipto.domain.ListConfig
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.file.FileScreenHelper
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_main_list_file_default.view.*

class FileItemDefaultBlock<V>(
    file: FileRef,
    listConfigGetter: () -> ListConfig,
    fileScreenHelper: FileScreenHelper,
    isSelectedGetter: (file: FileRef) -> Boolean,
    onFileClicked: (file: FileRef) -> Unit,
    onFileIconClicked: (file: FileRef) -> Unit,
    onFileChanged: (callback: (file: FileRef) -> Unit) -> Unit,
    onLongClick: ((file: FileRef) -> Boolean)? = null,
    highlight: String? = null,
    private val folderId: String? = null,
    private val flatMode: Boolean = false,
    private val relativePathGetter: ((folderId: String?, fileRef: FileRef, callback: (path: String) -> Unit) -> Unit)? = null
) : FileItemBlock<V>(
    file = file,
    listConfigGetter = listConfigGetter,
    fileScreenHelper = fileScreenHelper,
    isSelectedGetter = isSelectedGetter,
    onFileIconClicked = onFileIconClicked,
    onFileClicked = onFileClicked,
    onFileChanged = onFileChanged,
    onLongClick = onLongClick,
    highlight = highlight
) {

    override val layoutRes: Int = R.layout.block_main_list_file_default

    override fun areContentsTheSame(item: BlockItem<V>): Boolean {
        return super.areContentsTheSame(item)
                && item is FileItemDefaultBlock<*>
                && item.flatMode == flatMode
                && item.folderId == folderId
    }

    override fun getTitleView(block: View): TextView = block.tvName
    override fun getIconView(block: View): ImageView = block.ivIcon
    override fun getIconTextView(block: View): TextView = block.tvIcon
    override fun getAttrsView(block: View): TextView? = block.tvAttrs
    override fun getProgressImageView(block: View): ImageView = block.ivProgress
    override fun getProgressView(block: View): LinearProgressIndicator = block.lpProgress
    override fun getBgView(block: View): View? = block.bgView

    override fun onBind(context: V, block: View) {
        super.onBind(context, block)
        block.tvPath?.apply {
            setVisibleOrGone(flatMode)
            if (flatMode) {
                relativePathGetter?.invoke(folderId, file) { path ->
                    text = path
                }
            }
        }
    }

}