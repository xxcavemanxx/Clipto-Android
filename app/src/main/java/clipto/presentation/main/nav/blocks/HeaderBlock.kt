package clipto.presentation.main.nav.blocks

import android.view.View
import clipto.common.extensions.navigateTo
import clipto.common.extensions.setDebounceClickListener
import clipto.presentation.common.StyleHelper
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.main.nav.MainNavFragment
import clipto.presentation.main.nav.MainNavViewModel
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_main_nav_header.view.*

class HeaderBlock(val viewModel: MainNavViewModel) : BlockItem<MainNavFragment>() {

    override val layoutRes: Int = R.layout.block_main_nav_header

    override fun onInit(fragment: MainNavFragment, block: View) {
        val appConfig = viewModel.appConfig
        block.appNameTextView.text = StyleHelper.getAppTitle(block.context)
        block.setOnClickListener {
            if (appConfig.isRunesEnabled()) {
                fragment.navigateTo(R.id.action_runes)
            } else {
                fragment.navigateTo(R.id.action_settings)
            }
        }
        block.actionSettings.setDebounceClickListener {
            if (appConfig.isRunesEnabled()) {
                fragment.navigateTo(R.id.action_runes)
            } else {
                fragment.navigateTo(R.id.action_settings)
            }
        }
        block.actionSettings.setOnLongClickListener {
            if (appConfig.isRunesEnabled()) {
                fragment.navigateTo(R.id.action_settings)
            } else {
                fragment.navigateTo(R.id.action_runes)
            }
            true
        }
    }

    override fun onBind(fragment: MainNavFragment, block: View) = Unit
}