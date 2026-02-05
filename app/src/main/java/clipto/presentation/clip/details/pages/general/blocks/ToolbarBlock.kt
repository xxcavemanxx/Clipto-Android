package clipto.presentation.clip.details.pages.general.blocks

import android.view.View
import clipto.common.presentation.state.MenuState
import clipto.domain.Clip
import clipto.extensions.getTextColorPrimary
import clipto.extensions.getTextColorSecondary
import clipto.presentation.clip.details.pages.general.GeneralPageFragment
import clipto.presentation.clip.details.pages.general.GeneralPageViewModel
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.common.text.KeyValueString
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_general_toolbar.view.*
import java.util.*

class ToolbarBlock(
        private val viewModel: GeneralPageViewModel,
        private val clip: Clip,
        private val fav: Boolean = clip.fav,
        private val editMode: Boolean = viewModel.isEditMode(),
        private val isDeleted: Boolean = clip.isDeleted(),
        private val createDate: Date? = clip.createDate
) : BlockItem<GeneralPageFragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_general_toolbar

    override fun areItemsTheSame(item: BlockItem<GeneralPageFragment>): Boolean =
            item is ToolbarBlock &&
                    item.fav == fav &&
                    item.editMode == editMode &&
                    item.isDeleted == isDeleted &&
                    item.createDate == createDate

    override fun onBind(fragment: GeneralPageFragment, block: View) {
        val ctx = block.context
        MenuState<Clip>()
                .withContext(ctx)
                .withMenuItem(
                        MenuState.StatefulMenuItem<Clip>()
                                .withShowAsActionAlways()
                                .withTitle(R.string.menu_fav)
                                .withIcon {
                                    if (it.fav) {
                                        R.drawable.ic_fav_true
                                    } else {
                                        R.drawable.ic_fav_false_inverse
                                    }
                                }
                                .withListener { _, _ -> viewModel.onToggleFav() }
                )
                .withMenuItem(
                        MenuState.StatefulMenuItem<Clip>()
                                .withShowAsActionAlways()
                                .withIconColor(ctx.getTextColorPrimary())
                                .withTitle(R.string.menu_delete)
                                .withIcon {
                                    if (isDeleted) {
                                        R.drawable.ic_delete_forever
                                    } else {
                                        R.drawable.action_delete
                                    }
                                }
                                .withListener { _, _ -> viewModel.onDelete() }
                                .withStateAcceptor { !editMode }
                )
                .withMenuItem(
                        MenuState.StatefulMenuItem<Clip>()
                                .withShowAsActionAlways()
                                .withTitle(R.string.menu_share)
                                .withIcon(R.drawable.action_share)
                                .withListener { _, _ -> viewModel.onShare() }
                )
                .apply(clip, block.actionMenu.menu)

        KeyValueString(
                block.textView,
                "\n",
                ctx.getTextColorPrimary(),
                ctx.getTextColorSecondary()).apply {
            setKey(StyleHelper.getDateValue(createDate))
            setValue(ctx.getString(R.string.clip_attr_created))
        }
    }

}