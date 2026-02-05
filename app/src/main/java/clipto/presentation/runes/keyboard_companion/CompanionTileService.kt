package clipto.presentation.runes.keyboard_companion

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import clipto.common.extensions.closeSystemDialogs
import clipto.common.logging.L
import clipto.common.presentation.mvvm.base.LifecycleTileService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@RequiresApi(Build.VERSION_CODES.N)
class CompanionTileService : LifecycleTileService() {

    @Inject
    lateinit var state: CompanionState

    override fun onCreate() {
        super.onCreate()
        state.mode.getLiveData().observe(this) {
            updateMode()
        }
        state.tileActivated.getLiveData().observe(this) {
            state.requestShowNotification()
        }
    }

    override fun onTileAdded() {
        log("onTileAdded")
        state.tileActivated.setValue(true)
        updateMode()
    }

    override fun onTileRemoved() {
        log("onTileRemoved")
        state.tileActivated.setValue(false)
        state.requestShowNotification()
        updateMode()
    }

    override fun onStartListening() {
        log("onStartListening")
        state.tileActivated.setValue(true)
        updateMode()
    }

    override fun onStopListening() {
        log("onStopListening")
        state.tileActivated.setValue(true)
        updateMode()
    }

    override fun onClick() {
        val tileState = qsTile?.state ?: Tile.STATE_UNAVAILABLE
        log("onClick: qsState={}", tileState)
        when (tileState) {
            Tile.STATE_ACTIVE -> {
                CompanionService.createHideIntent(this).send()
            }
            Tile.STATE_UNAVAILABLE,
            Tile.STATE_INACTIVE -> {
                state.ifAvailable(requestActivationIfDisabled = true) {
                    CompanionService.createShowIntent(this).send()
                }
            }
            else -> Unit
        }
        closeSystemDialogs()
    }

    private fun updateMode() {
        log("update mode: isHidden={}", state.isHidden())
        when {
            state.isHidden() -> updateTile(Tile.STATE_INACTIVE)
            else -> updateTile(Tile.STATE_ACTIVE)
        }
    }

    private fun updateTile(state: Int) {
        log("updateTile :: {}", state)
        qsTile?.state = state
        qsTile?.updateTile()
    }

    private fun log(text: String, vararg params: Any?) {
        L.log(this, "keyboard_companion :: $text", *params)
    }

}