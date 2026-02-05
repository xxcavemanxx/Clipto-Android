package clipto.presentation.runes.loyalty

import androidx.fragment.app.Fragment
import clipto.domain.IRune
import clipto.presentation.blocks.domain.AboutBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class LoyaltyRuneProvider @Inject constructor() : RuneSettingsProvider(
    IRune.RUNE_LOYALTY,
    R.drawable.rune_loyalty,
    R.string.runes_loyalty_title,
    R.string.runes_loyalty_description
) {
    override fun getDefaultColor(): String = "#FF1744"

    override fun isActive(): Boolean = userState.isAuthorized()

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        list.add(
            AboutBlock(
                withTitle = false,
                userState = userState
            )
        )
        return list
    }
}