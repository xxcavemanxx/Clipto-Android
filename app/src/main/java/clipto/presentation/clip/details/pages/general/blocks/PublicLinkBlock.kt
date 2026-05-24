package clipto.presentation.clip.details.pages.general.blocks

import com.wb.clipboard.databinding.BlockClipDetailsGeneralPublicLinkBinding
import android.view.View
import clipto.common.extensions.setVisibleOrGone
import clipto.common.extensions.withSafeChildFragmentManager
import clipto.common.misc.IntentUtils
import clipto.domain.PublicLink
import clipto.presentation.clip.details.pages.general.*
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class PublicLinkBlock(
        private val viewModel: GeneralPageViewModel,
        private val publicLink: PublicLink
) : BlockItem<GeneralPageFragment>() {

    override val layoutRes: Int = R.layout.block_clip_details_general_public_link

    override fun areContentsTheSame(item: BlockItem<GeneralPageFragment>): Boolean =
            item is PublicLinkBlock &&
                    item.publicLink == publicLink

    override fun onBind(fragment: GeneralPageFragment, block: View) {
        val binding = BlockClipDetailsGeneralPublicLinkBinding.bind(block)
        // common
        val hasLink = !publicLink.link.isNullOrBlank()

        // access time
        binding.accessTimeChip?.isSelected = publicLink.isPostponed()

        // access password
        binding.accessPasswordChip?.isSelected = publicLink.isLocked()

        // time to expire
        binding.timeToExpireChip?.isSelected = publicLink.canBeExpired()

        // one time
        binding.oneTimeChip?.isSelected = publicLink.isOneTime()

        // link
        binding.linkButton?.setVisibleOrGone(hasLink)

        binding.linkButton.text = publicLink.link
        binding.copyLinkAction?.setVisibleOrGone(hasLink)

        // create link
        binding.createButton?.setVisibleOrGone(!hasLink)

        // remove link
        binding.removeButton?.setVisibleOrGone(hasLink)

        binding.accessTimeChip?.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditAccessTimeDialogFragment().show(fm, "PublicLinkEditAccessTimeDialogFragment")
            }
        }
        binding.accessPasswordChip.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditPasswordDialogFragment().show(fm, "PublicLinkEditPasswordDialogFragment")
            }
        }
        binding.timeToExpireChip.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditTimeToExpireDialogFragment().show(fm, "PublicLinkEditTimeToExpireDialogFragment")
            }
        }
        binding.oneTimeChip.setOnClickListener {
            fragment.withSafeChildFragmentManager()?.let { fm ->
                PublicLinkEditOneTimeDialogFragment().show(fm, "PublicLinkEditOneTimeDialogFragment")
            }
        }
        binding.linkButton.setOnClickListener {
            publicLink.link?.let { IntentUtils.open(viewModel.app, it) }
        }
        binding.copyLinkAction.setOnClickListener {
            viewModel.onCopyLink(publicLink)
        }
        binding.createButton.setOnClickListener {
            viewModel.onCreateLink(publicLink)
        }
        binding.createButton.isSelected = true
        binding.removeButton.setOnClickListener { viewModel.onRemoveLink() }
    }

}
