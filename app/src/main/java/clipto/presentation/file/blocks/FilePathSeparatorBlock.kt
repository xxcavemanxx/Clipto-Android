package clipto.presentation.file.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class FilePathSeparatorBlock<C> : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_file_path_separator

    override fun onBind(context: C, block: View) = Unit

}