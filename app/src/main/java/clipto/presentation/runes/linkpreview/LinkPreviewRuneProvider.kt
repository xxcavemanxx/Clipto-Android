package clipto.presentation.runes.linkpreview

import androidx.fragment.app.Fragment
import clipto.domain.IRune
import clipto.presentation.blocks.LinkPreviewExampleBlock
import clipto.presentation.blocks.SwitchBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.preview.link.LinkPreviewState
import clipto.presentation.runes.RuneSettingsFragment
import clipto.presentation.runes.RuneSettingsProvider
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class LinkPreviewRuneProvider @Inject constructor(
    private val linkPreviewState: LinkPreviewState
) : RuneSettingsProvider(
    IRune.RUNE_LINK_PREVIEW,
    R.drawable.rune_link_preview,
    R.string.runes_link_preview_title,
    R.string.runes_link_preview_description
) {
    override fun getDefaultColor(): String = "#00B0FF"

    override fun isActive(): Boolean = linkPreviewState.isPreviewEnabled()

    override fun bind(fragment: RuneSettingsFragment) {
        super.bind(fragment)
        val settings = appState.getSettings()
        var showPreview = !settings.hideLinkPreviews
        linkPreviewState.canShowPreview.getLiveData().observe(fragment) {
            if (it != showPreview) {
                showPreview = it
                appState.refreshSettings()
            }
        }
    }

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        val settings = appState.getSettings()
        list.add(SwitchBlock(
            titleRes = R.string.settings_link_preview_title,
            checked = !appState.getSettings().doNotPreviewLinks,
            clickListener = { _, isChecked ->
                if (settings.doNotPreviewLinks != !isChecked) {
                    settings.doNotPreviewLinks = !isChecked
                    appState.refreshSettings()
                }
            }
        ))
        list.add(
            LinkPreviewExampleBlock(
                appConfig.getLinkPreviewExampleUrl(),
                !settings.doNotPreviewLinks,
                settings.hideLinkPreviews
            )
        )
        return list
    }
}