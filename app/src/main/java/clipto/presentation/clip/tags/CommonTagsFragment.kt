package clipto.presentation.clip.tags

import androidx.fragment.app.viewModels
import clipto.analytics.Analytics
import clipto.common.extensions.inBrackets
import clipto.common.presentation.state.MenuState
import clipto.presentation.common.fragment.blocks.BlocksFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_blocks.*

@AndroidEntryPoint
class CommonTagsFragment : BlocksFragment<CommonTagsViewModel>() {

    override val viewModel: CommonTagsViewModel by viewModels()
    override fun getBackConfirmTitle(): Int = R.string.clip_multiple_exit_without_save_title
    override fun getBackConfirmMessage(): Int = R.string.clip_multiple_exit_without_save_description
    override fun getTitle(): String {
        val clips = viewModel.selectedClips
        return if (clips.size > 1) {
            val subtitle = viewModel.quantityString(R.plurals.main_toolbar_notes, clips.size, clips.size)
            "${viewModel.string(R.string.clip_multiple_edit_attributes_tags)} ${subtitle.inBrackets()}"
        } else {
            viewModel.string(R.string.clip_info_label_tags_edit)
        }
    }

    override fun bind(viewModel: CommonTagsViewModel) {
        super.bind(viewModel)

        MenuState<Unit>()
            .withMenuItem(MenuState.StatefulMenuItem<Unit>()
                .withIcon(R.drawable.ic_save)
                .withShowAsActionAlways()
                .withTitle(R.string.button_save)
                .withListener { _, _ -> viewModel.oAssignTags() })
            .apply(Unit, toolbar.menu)

        Analytics.screenEditClipAttributes()
    }
}