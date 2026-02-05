package clipto.presentation.common.fragment.blocks

import androidx.fragment.app.Fragment
import clipto.presentation.common.recyclerview.BlockItem

data class BlocksData(
    val blocks:List<BlockItem<Fragment>>,
    val scrollToTop:Boolean = false
)