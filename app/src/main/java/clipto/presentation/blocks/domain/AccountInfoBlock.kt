package clipto.presentation.blocks.domain

import android.view.View
import clipto.common.extensions.load
import clipto.common.extensions.setVisibleOrGone
import clipto.presentation.common.recyclerview.BlockItem
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_account_info.view.*

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
        photoUrl?.let { block.icon?.load(it) }
        block.titleTextView.text = title
        block.descriptionTextView.text = description
        block.accountPanel.setOnClickListener(onUpgradePlan)
        block.upgradeButton.setOnClickListener(onUpgradePlan)
        block.upgradeButton.setVisibleOrGone(showUpgradeButton)
        block.iconView.setVisibleOrGone(!showUpgradeButton)
    }

}