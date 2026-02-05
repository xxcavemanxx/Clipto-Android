package clipto.presentation.runes

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.config.IAppConfig
import clipto.domain.IRune
import clipto.presentation.blocks.DescriptionSecondaryBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.app.AppState
import clipto.store.main.MainState
import clipto.store.user.UserState
import kotlinx.android.synthetic.main.fragment_rune_settings.*
import javax.inject.Inject

abstract class RuneSettingsProvider(
    protected val uid: String,
    @DrawableRes protected val iconRes: Int,
    @StringRes protected val titleRes: Int,
    @StringRes protected val descriptionRes: Int
) : IRune {

    @Inject
    lateinit var app: Application

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var mainState: MainState

    @Inject
    lateinit var userState: UserState

    @Inject
    lateinit var appConfig: IAppConfig

    private var expand: Boolean = false

    override fun isExpanded(): Boolean = expand
    override fun setExpanded(expanded: Boolean) {
        expand = expanded
    }

    override fun getId(): String = uid

    override fun getIcon(): Int = iconRes

    override fun hasWarning(): Boolean = false

    override fun isAvailable(): Boolean = true

    override fun getTitle(): String = app.getString(titleRes)

    override fun getDescription(): String = app.getString(descriptionRes)

    override fun getColor(): String = appConfig.getRuneConfigs().find { it.id == getId() }
        ?.let { configRef -> if (appState.getTheme().dark) configRef.colorDark else configRef.colorLight }
        ?: getDefaultColor()


    abstract fun getDefaultColor(): String

    open fun createSettings(fragment: Fragment, flat: Boolean = false): List<BlockItem<Fragment>> = emptyList()

    open fun bind(recyclerView: RecyclerView, fragment: RunesFragment) {
        val ctx = recyclerView.context
        val adapter = recyclerView.adapter
        val adapterRef: BlockListAdapter<Fragment>
        if (adapter !is BlockListAdapter<*>) {
            adapterRef = BlockListAdapter(fragment)
            recyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
            recyclerView.adapter = adapterRef
        } else {
            adapterRef = adapter as BlockListAdapter<Fragment>
        }
        val items = createSettings(fragment, true).toMutableList()
        adapterRef.submitList(items)
    }

    open fun bind(fragment: RuneSettingsFragment) {
        val ctx = fragment.requireContext()
        val settingsAdapter = BlockListAdapter<Fragment>(fragment)
        fragment.rvBlocks.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.VERTICAL, false)
        fragment.rvBlocks.adapter = settingsAdapter
        appState.settings.getLiveData().observe(fragment) {
            val items = createSettings(fragment).toMutableList()
            items.addAll(
                0, listOf(
//                    SpaceBlock(heightInDp = 4),
                    DescriptionSecondaryBlock(
                        description = getDescription(),
                        textFont = mainState.getListConfig().textFont,
                        textSize = mainState.getListConfig().textSize
                    ),
                    SpaceBlock(heightInDp = 16),
                )
            )
            items.add(SpaceBlock(heightInDp = 96))
            settingsAdapter.submitList(items)
            fragment.viewModel.onRefreshRune()
        }
    }

}