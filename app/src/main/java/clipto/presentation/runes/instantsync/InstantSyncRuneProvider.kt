package clipto.presentation.runes.instantsync

import android.text.style.TypefaceSpan
import androidx.fragment.app.Fragment
import clipto.AppUtils
import clipto.action.DeleteAccountAction
import clipto.action.RebuildIndexAction
import clipto.analytics.Analytics
import clipto.common.extensions.navigateTo
import clipto.common.misc.IntentUtils
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.IRune
import clipto.domain.LicenseType
import clipto.extensions.getColorNegative
import clipto.extensions.getTextColorSecondary
import clipto.presentation.blocks.*
import clipto.presentation.blocks.domain.AccountInfoBlock
import clipto.presentation.blocks.ux.SeparatorVerticalBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.common.recyclerview.BlockItem
import clipto.presentation.runes.RuneSettingsFragment
import clipto.presentation.runes.RuneSettingsProvider
import clipto.repository.IClipRepository
import clipto.store.internet.InternetState
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class InstantSyncRuneProvider @Inject constructor(
    private val internetState: InternetState,
    private val clipRepository: IClipRepository,
    private val rebuildIndexAction: RebuildIndexAction,
    private val deleteAccountAction: DeleteAccountAction
) : RuneSettingsProvider(
    IRune.RUNE_INSTANT_SYNC,
    R.drawable.rune_instant_sync,
    R.string.runes_instant_sync_title,
    R.string.runes_instant_sync_description
) {
    override fun getDefaultColor(): String = "#00E5FF"

    override fun isActive(): Boolean = userState.isAuthorized()

    override fun hasWarning(): Boolean = appState.getSettings().disableSync

    override fun bind(fragment: RuneSettingsFragment) {
        super.bind(fragment)
        userState.user.getLiveData().observe(fragment) {
            appState.refreshSettings()
        }
        userState.syncLimit.getLiveData().observe(fragment) {
            appState.refreshSettings()
        }
    }

    override fun createSettings(fragment: Fragment, flat: Boolean): List<BlockItem<Fragment>> {
        val list = mutableListOf<BlockItem<Fragment>>()
        if (!isActive()) {
            if (flat) {
                list.add(SpaceBlock(12))
            }
            list.add(PrimaryButtonBlock(
                titleRes = R.string.account_button_sign_in,
                clickListener = {
                    userState.requestSignIn(webAuth = false, withWarning = true)
                },
                longClickListener = {
                    userState.requestSignIn(webAuth = true, withWarning = true)
                    true
                }
            ))
            if (flat) {
                list.add(SpaceBlock(12))
            } else {
                withCommonButtons(list)
            }
        } else {
            val user = userState.user.requireValue()
            list.add(AccountInfoBlock(
                photoUrl = user.photoUrl,
                title = user.getDetailedName() ?: app.getString(R.string.account_title_authorized),
                description = userState.getSyncLimit().toString(),
                showUpgradeButton = user.license == LicenseType.NONE,
                onUpgradePlan = {
                    fragment.navigateTo(R.id.action_select_plan)
                }
            ))
            list.add(SeparatorVerticalBlock())
            list.add(SwitchBlock(
                titleRes = R.string.account_sync_title,
                iconRes = R.drawable.ic_sync,
                checked = !appState.getSettings().disableSync,
                clickListener = { _, isChecked ->
                    if (appState.getSettings().disableSync != !isChecked) {
                        appState.getSettings().disableSync = !isChecked
                        appState.refreshSettings()
                        clipRepository.syncAll()
                    }
                }
            ))
            list.add(SeparatorVerticalBlock())
            list.add(SeparateScreenBlock(
                titleRes = R.string.account_button_sign_out,
                iconRes = R.drawable.ic_exit_to_app,
                clickListener = {
                    internetState.withInternet({
                        userState.requestSignOut()
                    })
                }
            ))

            if (!flat) {
                withCommonButtons(list)
            }

            user.firebaseId?.let {
                list.add(SpaceBlock(heightInDp = 12))
                list.add(
                    CopiedLinkBlock(
                        link = it,
                        label = SimpleSpanBuilder()
                            .append("ID:", TypefaceSpan("sans-serif-medium"))
                            .append(" ")
                            .append(it)
                            .build(),
                        canBeOpened = false,
                        centered = true,
                        internetState = internetState
                    )
                )
            }

            list.add(SpaceBlock(heightInDp = 12))
            list.add(TextButtonBlock(
                textColor = app.getColorNegative(),
                titleRes = R.string.account_delete_title,
                clickListener = { deleteAccountAction.execute(user) }
            ))
            if (flat) {
                list.add(SpaceBlock(12))
            }
        }
        return list
    }

    private fun <F : Fragment> withCommonButtons(list: MutableList<BlockItem<F>>) {
        val linux = app.getString(R.string.desktop_main_menu_download_linux)
        val win = app.getString(R.string.desktop_main_menu_download_windows)
        val mac = app.getString(R.string.desktop_main_menu_download_mac)
        list.add(SpaceBlock(heightInDp = 12))
        list.add(OutlinedButtonBlock(
            title = app.getString(R.string.desktop_main_menu_download_for, "${win}, ${mac}, $linux"),
            clickListener = {
                Analytics.onDownloadDesktopApp()
                IntentUtils.open(app, BuildConfig.appDownloadLink)
            }
        ))
        list.add(SpaceBlock(heightInDp = 12))
        list.add(OutlinedButtonBlock(
            title = app.getString(R.string.desktop_main_menu_open),
            clickListener = {
                Analytics.onOpenBrowserApp()
                IntentUtils.open(app, BuildConfig.appSiteLink)
            }
        ))

        list.add(SpaceBlock(heightInDp = 12))
        list.add(TextButtonBlock(
            titleRes = R.string.about_label_issue,
            clickListener = {
                AppUtils.sendRequest(app.getString(R.string.runes_instant_sync_title))
            }
        ))

        if (appConfig.canRequestRebuildIndex()) {
            list.add(TextButtonBlock(
                textColor = app.getTextColorSecondary(),
                titleRes = R.string.account_rebuild_index_title,
                clickListener = {
                    val user = userState.user.requireValue()
                    rebuildIndexAction.execute(user)
                }
            ))
        }
    }
}