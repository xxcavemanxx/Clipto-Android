package clipto.presentation.clip.details.pages.general

import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class PublicLinkEditAccessTimeDialogFragment : PublicLinkEditTimeDialogFragment() {

    override fun getIconRes(): Int = R.drawable.public_link_access_time
    override fun getTitleRes(): Int = R.string.public_note_link_access_time
    override fun getDescriptionRes(): Int = R.string.public_note_link_access_time_description
    override fun getTimeLabelRes(): Int = R.string.public_note_link_access_time_label
    override fun getInitialTimeAsDate(): Date? = viewModel.getPublicLink().postponeAtDate
    override fun getInitialTimeInMillis(): Long? = viewModel.getPublicLink().postponeInMillis
    override fun onApply(timeInMillis: Long?, timeAsDate: Date?) = viewModel.onPublicLinkAccessTimeChanged(timeInMillis, timeAsDate) {
        dismissAllowingStateLoss()
    }

}