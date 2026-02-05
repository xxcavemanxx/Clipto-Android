package clipto.presentation.runes.theme

import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import clipto.domain.IRune
import clipto.domain.Theme
import clipto.presentation.blocks.DescriptionSecondaryBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.blocks.domain.ThemeBlock
import clipto.presentation.common.recyclerview.FlowLayoutManagerExt
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsFragment
import clipto.presentation.runes.RuneSettingsProvider
import clipto.presentation.runes.RunesFragment
import clipto.store.clipboard.ClipboardState
import com.wb.clipboard.R
import com.xiaofeng.flowlayoutmanager.Alignment
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.android.synthetic.main.fragment_rune_settings.*
import javax.inject.Inject

@ViewModelScoped
class ThemeRuneProvider @Inject constructor(
    val clipboardState: ClipboardState
) : RuneSettingsProvider(
    IRune.RUNE_THEME,
    R.drawable.rune_theme,
    R.string.runes_theme_title,
    R.string.runes_theme_description
) {

    override fun getDefaultColor(): String = "#76FF03"

    override fun isActive(): Boolean = appState.getTheme() != Theme.DEFAULT

    override fun bind(fragment: RuneSettingsFragment) {
        bind(fragment, fragment.rvBlocks, false)
    }

    override fun bind(recyclerView: RecyclerView, fragment: RunesFragment) {
        bind(fragment, recyclerView, true)
    }

    private fun bind(fragment: Fragment, recyclerView: RecyclerView, flat: Boolean) {
        val onClickListener: (theme: Theme) -> Unit = {
            if (appState.getTheme() != it) {
                appState.getSettings().theme = it.id
                appState.refreshSettings()
                clipboardState.refreshNotification()
                appState.requestRestart()
            }
        }

        val blocks = mutableListOf<BlockItem<Fragment>>()
//        blocks.add(SpaceBlock(8))
        blocks.add(DescriptionSecondaryBlock(getDescription()))
        blocks.add(SpaceBlock(16))

        val activeTheme = appState.getTheme()
        Theme.values().forEach {
            blocks.add(
                ThemeBlock(
                    theme = it,
                    selected = it == activeTheme,
                    colorListItemSelected = Color.parseColor(it.colorListItemSelected),
                    colorPrimaryInverse = Color.parseColor(it.colorPrimaryInverse),
                    colorAccent = Color.parseColor(it.colorAccent),
                    dense = flat,
                    onClickListener = onClickListener
                )
            )
        }

        recyclerView.layoutManager = FlowLayoutManagerExt().also {
            it.setAlignment(Alignment.CENTER)
            it.isAutoMeasureEnabled = true
        }
        val adapter = BlockListAdapter(fragment)
        recyclerView.adapter = adapter
        adapter.submitList(blocks)
    }

}