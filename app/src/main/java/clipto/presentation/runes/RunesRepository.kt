package clipto.presentation.runes

import clipto.config.IAppConfig
import clipto.domain.IRune
import clipto.presentation.runes.autosave.AutoSaveRuneProvider
import clipto.presentation.runes.backup_restore.BackupRestoreRuneProvider
import clipto.presentation.runes.clipboard.ClipboardRuneProvider
import clipto.presentation.runes.doubleclickactions.DoubleClickActionsRuneProvider
import clipto.presentation.runes.focusontitle.FocusOnTitleRuneProvider
import clipto.presentation.runes.hideoncopy.HideOnCopyRuneProvider
import clipto.presentation.runes.instantsync.InstantSyncRuneProvider
import clipto.presentation.runes.keyboard_companion.CompanionRuneProvider
import clipto.presentation.runes.linkpreview.LinkPreviewRuneProvider
import clipto.presentation.runes.loyalty.LoyaltyRuneProvider
import clipto.presentation.runes.pincode.PincodeRuneProvider
import clipto.presentation.runes.rememberlastfilter.RememberLastFilterRuneProvider
import clipto.presentation.runes.swipeactions.SwipeActionsRuneProvider
import clipto.presentation.runes.theme.ThemeRuneProvider
import clipto.repository.IRunesRepository
import dagger.hilt.android.scopes.ViewModelScoped
import io.reactivex.Single
import javax.inject.Inject

@ViewModelScoped
class RunesRepository @Inject constructor(appConfig: IAppConfig) : IRunesRepository {

    @Inject
    lateinit var backupRestoreRuneProvider: BackupRestoreRuneProvider

    @Inject
    lateinit var instantSyncRuneProvider: InstantSyncRuneProvider

    @Inject
    lateinit var clipboardRuneProvider: ClipboardRuneProvider

    @Inject
    lateinit var pincodeRuneProvider: PincodeRuneProvider

    @Inject
    lateinit var themeRuneProvider: ThemeRuneProvider

    @Inject
    lateinit var linkPreviewRuneProvider: LinkPreviewRuneProvider

    @Inject
    lateinit var companionRuneProvider: CompanionRuneProvider

    @Inject
    lateinit var hideOnCopyRuneProvider: HideOnCopyRuneProvider

    @Inject
    lateinit var swipeActionsRuneProvider: SwipeActionsRuneProvider

    @Inject
    lateinit var autoSaveRuneProvider: AutoSaveRuneProvider

    @Inject
    lateinit var focusOnTitleRuneProvider: FocusOnTitleRuneProvider

    @Inject
    lateinit var doubleClickActionsRuneProvider: DoubleClickActionsRuneProvider

    @Inject
    lateinit var rememberLastFilterRuneProvider: RememberLastFilterRuneProvider

    @Inject
    lateinit var loyaltyRuneProvider: LoyaltyRuneProvider

    private val runes by lazy {
        val runes = listOf(
            instantSyncRuneProvider,
            pincodeRuneProvider,
            themeRuneProvider,
            clipboardRuneProvider,
//            UniversalClipboardRuneProvider(),
//            NotificationRuneProvider(),
            linkPreviewRuneProvider,
            companionRuneProvider,
            hideOnCopyRuneProvider,
            swipeActionsRuneProvider,
            autoSaveRuneProvider,
            focusOnTitleRuneProvider,
            doubleClickActionsRuneProvider,
            rememberLastFilterRuneProvider,
            backupRestoreRuneProvider,
            loyaltyRuneProvider,
        )
        runes
    }

    override fun getById(id: String): Single<IRune> = Single.fromCallable { runes.find { it.getId() == id } }

    override fun getAll(): Single<List<IRune>> = Single.fromCallable { runes }

}