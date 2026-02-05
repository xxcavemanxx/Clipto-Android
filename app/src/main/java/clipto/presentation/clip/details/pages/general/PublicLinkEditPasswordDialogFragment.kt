package clipto.presentation.clip.details.pages.general

import clipto.common.extensions.animateScale
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_clip_public_link_edit_access_password.*

@AndroidEntryPoint
class PublicLinkEditPasswordDialogFragment : PublicLinkEditDialogFragment() {

    override val layoutResId: Int = R.layout.fragment_clip_public_link_edit_access_password

    override fun bind(viewModel: GeneralPageViewModel) {
        // password
        passwordClueView.setText(viewModel.getPublicLink().passwordClue)

        // cancel action
        cancelAction.setOnClickListener { dismissAllowingStateLoss() }

        // apply action
        applyAction.setOnClickListener {
            val password = passwordView.text.toString()
            val passwordClue = passwordClueView.text.toString()
            viewModel.onPublicLinPasswordChanged(password, passwordClue) {
                dismissAllowingStateLoss()
            }
        }

        iconView.animateScale(true)
    }

}