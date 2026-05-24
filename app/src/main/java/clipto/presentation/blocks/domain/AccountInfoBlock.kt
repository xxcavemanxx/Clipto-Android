package clipto.presentation.blocks.domain

import com.wb.clipboard.databinding.BlockAccountInfoBinding
import android.view.View
import clipto.common.extensions.load
import clipto.common.extensions.setVisibleOrGone
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R

class AccountInfoBlock<C>(
    private val photoUrl: String?,
    private val title: String,
    private val description: String,
    private val showUpgradeButton: Boolean = true,
    private val onUpgradePlan: View.OnClickListener
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_account_info

    override fun areContentsTheSame(item: BlockItem<C>): Boolean =
        item is AccountInfoBlock
                && photoUrl == item.photoUrl
                && title == item.title
                && description == item.description
                && showUpgradeButton == item.showUpgradeButton

    override fun onBind(context: C, block: View) {
        val binding = BlockAccountInfoBinding.bind(block)
        photoUrl?.let { binding.icon?.load(it) }
        binding.titleTextView.text = title
        binding.descriptionTextView.text = description
        binding.accountPanel.setOnClickListener(onUpgradePlan)
        binding.upgradeButton.setOnClickListener(onUpgradePlan)
        binding.upgradeButton.setVisibleOrGone(showUpgradeButton)
        binding.iconView.setVisibleOrGone(!showUpgradeButton)
    }

}
