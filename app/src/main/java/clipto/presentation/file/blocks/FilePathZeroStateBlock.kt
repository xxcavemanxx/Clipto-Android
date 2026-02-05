package clipto.presentation.file.blocks

import android.view.View
import androidx.fragment.app.Fragment
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class FilePathZeroStateBlock<F : Fragment> : BlockItem<F>() {

    override val layoutRes: Int = R.layout.block_file_path_zero_state

    override fun onBind(fragment: F, block: View) = Unit

}