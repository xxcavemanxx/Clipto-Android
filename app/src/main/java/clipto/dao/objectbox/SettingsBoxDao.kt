package clipto.dao.objectbox

import clipto.dao.objectbox.model.SettingsBox
import clipto.domain.Settings
import clipto.store.app.AppState
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsBoxDao @Inject constructor(
    private val appState: Lazy<AppState>
) : AbstractBoxDao<SettingsBox>() {

    private val _settings by lazy {
        box.all.lastOrNull()
            ?: run {
                val box = SettingsBox()
                save(box)
                box
            }
    }

    override fun getType(): Class<SettingsBox> = SettingsBox::class.java

    override fun clear() {
        val settings = get()
        if (settings.universalClipboard) {
            settings.universalClipboard = false
            save(settings)
            appState.get().refreshSettings()
        }
    }

    fun get(): SettingsBox = _settings

    fun update(updater: (settings: Settings) -> Boolean) {
        val settings = get()
        if (updater.invoke(settings)) {
            save(settings)
            appState.get().refreshSettings()
        }
    }

    fun save(settings: SettingsBox) = box.put(settings)

}