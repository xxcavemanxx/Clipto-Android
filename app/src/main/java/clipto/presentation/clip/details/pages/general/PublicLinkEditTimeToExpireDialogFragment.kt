package clipto.presentation.clip.details.pages.general

import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PublicLinkEditTimeToExpireDialogFragment : PublicLinkEditTimeDialogFragment() {

    override fun getIconRes(): Int = R.drawable.public_link_time_to_expire
    override fun getTitleRes(): Int = R.string.public_note_link_time_to_expire
    override fun getDescriptionRes(): Int = R.string.public_note_link_time_to_expire_description
    override fun getTimeLabelRes(): Int = R.string.public_note_link_time_to_expire_label
    override fun getInitialTimeAsDate(): Date? = viewModel.getPublicLink().expiresAtDate
    override fun getInitialTimeInMillis(): Long? = viewModel.getPublicLink().expiresInMillis
    override fun onApply(timeInMillis: Long?, timeAsDate: Date?) = viewModel.onPublicLinkTimeToExpireChanged(timeInMillis, timeAsDate) {
        dismissAllowingStateLoss()
    }

}