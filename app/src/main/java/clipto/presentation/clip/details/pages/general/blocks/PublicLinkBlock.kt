package clipto.presentation.clip.details.pages.general.blocks

import android.view.View
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.withSafeChildFragmentManager
import clipto.common.misc.IntentUtils
import clipto.domain.PublicLink
import clipto.presentation.clip.details.pages.general.*
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_clip_details_general_public_link.view.*

class PublicLinkBlock(
        private val viewModel: GeneralPageViewModel,
        private val publicLink: PublicLink
) : BlockItem<GeneralPageFragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_general_public_link

    override fun areContentsTheSame(item: BlockItem<GeneralPageFragment>): Boolean =
            item is PublicLinkBlock &&
                    item.publicLink == publicLink

    override fun onBind(fragment: GeneralPageFragment, block: View) {
        // common
        val hasLink = !publicLink.link.isNullOrBlank()

        // access time
        block.accessTimeChip?.isSelected = publicLink.isPostponed()

        // access password
        block.accessPasswordChip?.isSelected = publicLink.isLocked()

        // time to expire
        block.timeToExpireChip?.isSelected = publicLink.canBeExpired()

        // one time
        block.oneTimeChip?.isSelected = publicLink.isOneTime()

        // link
        block.linkButton?.setVisibleOrGone(hasLink)

        block.linkButton.text = publicLink.link
        block.copyLinkAction?.setVisibleOrGone(hasLink)

        // create link
        block.createButton?.setVisibleOrGone(!hasLink)

        // remove link
        block.removeButton?.setVisibleOrGone(hasLink)

        block.accessTimeChip?.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditAccessTimeDialogFragment().show(fm, "PublicLinkEditAccessTimeDialogFragment")
            }
        }
        block.accessPasswordChip.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditPasswordDialogFragment().show(fm, "PublicLinkEditPasswordDialogFragment")
            }
        }
        block.timeToExpireChip.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditTimeToExpireDialogFragment().show(fm, "PublicLinkEditTimeToExpireDialogFragment")
            }
        }
        block.oneTimeChip.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditOneTimeDialogFragment().show(fm, "PublicLinkEditOneTimeDialogFragment")
            }
        }
        block.linkButton.setOnClickListener {
            publicLink.link?.let { IntentUtils.open(viewModel.app, it) }
        }
        block.copyLinkAction.setOnClickListener {
            viewModel.onCopyLink(publicLink)
        }
        block.createButton.setOnClickListener {
            viewModel.onCreateLink(publicLink)
        }
        block.createButton.isSelected = true
        block.removeButton.setOnClickListener { viewModel.onRemoveLink() }
    }

}