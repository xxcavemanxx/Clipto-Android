package clipto.presentation.clip.details.pages.general

import clipto.common.extensions.animateScale
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_clip_public_link_edit_one_time.*

@AndroidEntryPoint
class PublicLinkEditOneTimeDialogFragment : PublicLinkEditDialogFragment() {

    override val layoutResId: Int = R.layout.fragment_clip_public_link_edit_one_time

    override fun bind(viewModel: GeneralPageViewModel) {
        // one time
        oneTimeToggle.isChecked = viewModel.getPublicLink().isOneTime() == true

        // cancel action
        cancelAction.setOnClickListener { dismissAllowingStateLoss() }

        // apply action
        applyAction.setOnClickListener {
            viewModel.onPublicLinkOneTimeChanged(oneTimeToggle.isChecked) {
                dismissAllowingStateLoss()
            }
        }

        iconView.animateScale(true)
    }

}