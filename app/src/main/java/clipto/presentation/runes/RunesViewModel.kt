package clipto.presentation.runes

import androidx.lifecycle.MutableLiveData
import clipto.action.SaveFilterAction
import clipto.action.SaveSettingsAction
import clipto.common.extensions.inBrackets
import clipto.common.extensions.toEmoji
import clipto.common.presentation.mvvm.RxViewModel
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import clipto.common.presentation.text.SimpleSpanBuilder
import clipto.domain.IRune
import clipto.presentation.common.dialog.DialogState
import clipto.presentation.common.dialog.hint.HintDialogData
import clipto.presentation.common.dialog.select.value.SelectValueDialogRequest
import clipto.repository.IRunesRepository
import clipto.store.app.AppState
import clipto.store.user.UserState
import com.wb.clipboard.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.*
import javax.inject.Inject

@HiltViewModel
class RunesViewModel @Inject constructor(
    userState: UserState,
    private val appState: AppState,
    private val dialogState: DialogState,
    private val repository: IRunesRepository,
    private val saveFilterAction: SaveFilterAction,
    private val saveSettingsAction: SaveSettingsAction,
) : RxViewModel(appState.app) {

    val languageLive = appState.language.getLiveData()
    val settingsLive = appState.settings.getLiveData()
    val syncLimitLive = userState.syncLimit.getLiveData()
    val runesFlatLive = MutableLiveData<List<RuneFlatItem>>()
    val runesLive = MutableLiveData<List<RuneItem>>()
    val runeLive = SingleLiveData<IRune>()

    fun isFlatMode(): Boolean = appState.getSettings().settingsFlatMode

    fun onSelectRune(rune: IRune) = runeLive.postValue(rune)

    override fun doClear() {
        super.doClear()
        saveSettingsAction.execute()
        saveFilterAction.execute(appState.getFilterByClipboard())
    }

    fun onShowHint() {
        val title = string(R.string.runes_toolbar_title)
        val description = string(R.string.runes_hint_description)
        dialogState.showHint(
            HintDialogData(
                title = title,
                description = description,
                iconRes = R.drawable.rune_hint
            )
        )
    }

    fun onShowHint(rune: IRune) {
        dialogState.showHint(
            HintDialogData(
                title = rune.getTitle(),
                description = rune.getDescription(),
                descriptionIsMarkdown = true,
                iconRes = R.drawable.rune_hint
            )
        )
    }

    fun onChangeLanguage() {
        val selected = appState.getSelectedLocale()

        val available = arrayListOf(
            Locale("en", "US"),
            Locale("ar", "AE"),
            Locale("be", "BY"),
            Locale("bg", "BG"),
            Locale("bn", "BD"),
            Locale("cs", "CZ"),
            Locale("da", "DK"),
            Locale("de", "DE"),
            Locale("es", "ES"),
            Locale("es", "US"),
            Locale("ca", "ES"),
            Locale("et", "EE"),
            Locale("fil", "PH"),
            Locale("fr", "FR"),
            Locale("hi", "IN"),
            Locale("hu", "HU"),
            Locale("in", "ID"),
            Locale("it", "IT"),
            Locale("ja", "JP"),
            Locale("ko", "KR"),
            Locale("nl", "NL"),
            Locale("pl", "PL"),
            Locale("pt", "BR"),
            Locale("ru", "RU"),
            Locale("sl", "SI"),
            Locale("sv", "SE"),
            Locale("tr", "TR"),
            Locale("uk", "UA"),
            Locale("vi", "VN"),
            Locale("zh", "CN"),
            Locale("zh", "TW"),
        )

        val request = SelectValueDialogRequest(
            title = string(R.string.settings_language_title),
            single = true,
            withClearAll = false,
            withImmediateNotify = true,
            options = available.map { locale ->
                val title = SimpleSpanBuilder()
                    .append(locale.toEmoji() ?: locale.language.inBrackets())
                    .append("    ")
                    .append(locale.getDisplayLanguage(locale))
                    .append(" ")
                    .append(locale.getDisplayCountry(locale).inBrackets())
                    .build()
                SelectValueDialogRequest.Option(
                    model = locale,
                    checked = selected == locale,
                    title = title
                )
            },
            onSelected = {
                it.firstOrNull()?.let { appState.language.setValue(it) }
            }
        )
        dialogState.requestSelectValueDialog(request)
    }

    fun onChangeMode() {
        val settings = appState.getSettings()
        val mode = !settings.settingsFlatMode
        settings.settingsFlatMode = mode
        onRefresh()
    }

    fun onRefresh() {
        repository.getAll()
            .subscribeBy("onRefreshRunes") { all ->
                if (isFlatMode()) {
                    val items = all
                        .filter { it.isAvailable() }
                        .map {
                            val isActive = it.isActive()
                            RuneFlatItem(
                                rune = it,
                                isActive = isActive,
                                hasWarning = isActive && it.hasWarning(),
                                expanded = it.isExpanded()
                            )
                        }
                    runesFlatLive.postValue(items)
                } else {
                    val items = all
                        .filter { it.isAvailable() }
                        .map {
                            val isActive = it.isActive()
                            RuneItem(
                                rune = it,
                                isActive = isActive,
                                hasWarning = isActive && it.hasWarning()
                            )
                        }
                    runesLive.postValue(items)
                }
            }
    }

}