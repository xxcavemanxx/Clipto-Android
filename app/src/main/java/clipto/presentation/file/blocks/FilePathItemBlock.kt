package clipto.presentation.file.blocks

import android.content.res.ColorStateList
import android.view.View
import clipto.domain.FileRef
import clipto.domain.getBgColor
import clipto.domain.getFavIcon
import clipto.presentation.common.recyclerview.BlockItem
import com.google.android.material.chip.Chip
import com.wb.clipboard.R

data class FilePathItemBlock<C>(
    val fileRef: FileRef,
    val flat: Boolean = false,
    val onClicked: (fileRef: FileRef) -> Unit,
    val onLongClicked: (fileRef: FileRef) -> Unit = {}
) : BlockItem<C>(), View.OnClickListener, View.OnLongClickListener {

    val fav: Boolean = fileRef.fav
    val name: String? = fileRef.title
    val color: String? = fileRef.color

    override val layoutRes: Int = R.layout.block_file_path_item

    override fun onClick(v: View?) {
        val blockRef = v?.tag
        if (blockRef is FilePathItemBlock<*>) {
            blockRef.onClicked.invoke(blockRef.fileRef)
        }
    }

    override fun onLongClick(v: View?): Boolean {
        val blockRef = v?.tag
        if (blockRef is FilePathItemBlock<*>) {
            blockRef.onLongClicked.invoke(blockRef.fileRef)
        }
        return true
    }

    override fun areItemsTheSame(item: BlockItem<C>): Boolean =
        super.areItemsTheSame(item) && item is FilePathItemBlock<*>
                && item.fileRef == fileRef

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        super.areContentsTheSame(item) && item is FilePathItemBlock<*>
                && item.color == color
                && item.flat == flat
                && item.name == name
                && item.fav == fav

    override fun onInit(context: C, block: View) {
        block.setOnLongClickListener(this)
        block.setOnClickListener(this)
    }

    override fun onBind(context: C, block: View) {
        block as Chip
        block.text = name
        val color = fileRef.getBgColor(block.context, 80)
        block.chipBackgroundColor = ColorStateList.valueOf(color)
        if (fileRef.fav) {
            block.setChipIconResource(fileRef.getFavIcon())
            block.isChipIconVisible = true
        } else {
            block.isChipIconVisible = false
            block.chipIcon = null
        }
        if (flat) {
            block.setCloseIconResource(R.drawable.ic_folder_search)
        } else {
            block.closeIcon = null
        }
        block.isCloseIconVisible = flat
        block.tag = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilePathItemBlock<*>

        if (fileRef != other.fileRef) return false
        if (fav != other.fav) return false
        if (name != other.name) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileRef.hashCode()
        result = 31 * result + fav.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (color?.hashCode() ?: 0)
        return result
    }

}