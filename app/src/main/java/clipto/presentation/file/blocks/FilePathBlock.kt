package clipto.presentation.file.blocks

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.FileRef
import clipto.domain.factory.FileRefFactory
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R

class FilePathBlock<C>(
    private val flat: Boolean = false,
    private val folders: List<FileRef>,
    private val withNewFolder: Boolean = false,
    private val onChanged: (fileRef: FileRef?) -> Unit,
    private val onLongClicked: (fileRef: FileRef) -> Unit,
    private val onNewFolder: () -> Unit = {},
    private val foldersBlocks: List<FilePathItemBlock<C>> = folders.map {
        FilePathItemBlock(
            fileRef = it,
            onClicked = onChanged,
            onLongClicked = onLongClicked
        )
    }
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_file_path

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is FilePathBlock<C>
                && item.flat == flat
                && item.withNewFolder == withNewFolder
                && item.foldersBlocks == foldersBlocks

    override fun onInit(context: C, block: View) {
        block as RecyclerView
        val ctx = block.context
        block.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
        val adapter = BlockListAdapter(context)
        block.adapter = adapter
    }

    override fun onBind(context: C, block: View) {
        block as RecyclerView
        val adapter = block.adapter as BlockListAdapter<C>

        val items = mutableListOf<BlockItem<C>>()
        items.add(
            FilePathItemBlock(
                FileRefFactory.root(),
                onClicked = onChanged,
                onLongClicked = onLongClicked,
                flat = flat && foldersBlocks.isEmpty()
            )
        )
        foldersBlocks.forEachIndexed { index, folderBlock ->
            val file = folderBlock.fileRef
            items.add(FilePathSeparatorBlock())
            items.add(
                FilePathItemBlock(
                    fileRef = file,
                    onClicked = onChanged,
                    onLongClicked = onLongClicked,
                    flat = flat && index == foldersBlocks.size - 1
                )
            )
        }
        if (withNewFolder) {
            items.add(FilePathSeparatorBlock())
            items.add(
                FilePathItemBlock(
                    FileRefFactory.create(),
                    onClicked = { onNewFolder() },
                    onLongClicked = { onNewFolder() }
                )
            )
        }
        adapter.submitList(items)
    }

}