package clipto.presentation.runes.keyboard_companion

import android.accessibilityservice.AccessibilityService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import clipto.common.misc.AndroidUtils
import clipto.common.misc.IntentUtils
import clipto.extensions.isEditable
import clipto.extensions.isViewClicked
import clipto.presentation.runes.keyboard_companion.panel.CompanionPanel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class CompanionService : AccessibilityService() {

    @Inject
    lateinit var viewModel: CompanionViewModel

    @Inject
    lateinit var companionState: CompanionState

    private var lastClickTime = 0L
    private var lastClickedNodeId = 0
    private var lastOpenedTime = System.currentTimeMillis()
    private var panelProvider: CompanionPanelProvider = CompanionPanelProvider()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        panelProvider.panel.log("onStartCommand: {}", intent)
        runCatching {
            if (intent != null) {

                when (intent.action) {
                    ACTION_SHOW -> {
                        companionState.ifAvailable {
                            if (viewModel.isEnabled()) {
                                viewModel.withDelay { panelProvider.panel.modeAutoDetected() }
                            } else {
                                viewModel.withDelay { panelProvider.hide() }
                            }
                        }
                    }
                    ACTION_HIDE -> {
                        companionState.ifAvailable {
                            if (viewModel.isEnabled()) {
                                viewModel.withDelay { panelProvider.panel.modeUserHidden() }
                            } else {
                                viewModel.withDelay { panelProvider.hide() }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onInterrupt() {
        viewModel.log("onInterrupt")
        viewModel.onShowNotification()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        viewModel.log("onUnbind")
        panelProvider.panel.unbind()
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val panel = panelProvider.panel

        // CHECK IF DISABLED
        if (!viewModel.isEnabled()) {
            panelProvider.hide()
            return
        }

        // CHECK IF FOCUSED
        if (panel.isUserFocused()) {
            panel.log("prevent event due to focused state :: {}", event)
            return
        }

        companionState.ifAvailable {
            // BIND
            panel.log("onAccessibilityEvent :: {}", event)
            if (viewModel.isUserHidden()) {
                viewModel.onShowNotification()
            } else {
                panel.bind(event)
            }

            if (viewModel.canDoubleClickToShow() && panel.isHidden() && event.isViewClicked() && event.isEditable()) {
                // DOUBLE CLICK TO SHOW
                val currentClickTime = System.currentTimeMillis()
                val currentNodeId = event?.source?.hashCode() ?: 0
                val prevClickTime = lastClickTime
                if (currentNodeId == lastClickedNodeId && currentClickTime - prevClickTime <= viewModel.getDoubleClickToShowThreshold()) {
                    panel.log("double click threshold: {}", currentNodeId)
                    panel.modeAutoDetected()
                    lastClickedNodeId = 0
                    lastClickTime = 0L
                } else {
                    lastClickedNodeId = currentNodeId
                    lastClickTime = currentClickTime
                }
            } else if (!panel.isHidden()) {
                lastOpenedTime = System.currentTimeMillis()
            } else {
                val interval = viewModel.getPanelDestroyOnInactivityIntervalInSeconds()
                if (System.currentTimeMillis() - lastOpenedTime > TimeUnit.SECONDS.toMillis(interval.toLong())) {
                    lastOpenedTime = System.currentTimeMillis()
                    panelProvider.recreateIfNeed()
                }
            }

            viewModel.companionState.isAvailable.setValue(true)
        }
    }

    companion object {
        private const val ACTION_SHOW = "action_show"
        private const val ACTION_HIDE = "action_hide"

        fun createShowIntent(context: Context): PendingIntent {
            return Intent(context, CompanionService::class.java).let {
                it.action = ACTION_SHOW
                val id = AndroidUtils.nextId()
                PendingIntent.getService(context, id, it, IntentUtils.getPendingIntentFlags())
            }
        }

        fun createHideIntent(context: Context): PendingIntent {
            return Intent(context, CompanionService::class.java).let {
                it.action = ACTION_HIDE
                val id = AndroidUtils.nextId()
                PendingIntent.getService(context, id, it, IntentUtils.getPendingIntentFlags())
            }
        }
    }

    private inner class CompanionPanelProvider : LifecycleOwner {

        private val service = this@CompanionService
        private val owner = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle = owner

        val panel: CompanionPanel by lazy {
            val panelRef = CompanionPanel(this@CompanionService, viewModel)
            val theme = viewModel.getTheme()
            viewModel.themeLive.observe(this) {
                companionState.ifAvailable {
                    if (theme != it) recreateIfNeed().panel.modeAutoDetected()
                }
            }
            owner.currentState = Lifecycle.State.STARTED
            panelRef
        }

        fun recreateIfNeed(): CompanionPanelProvider {
            return if (panel.isInitialized()) {
                panel.log("recreate :: continue")
                panel.unbind()
                owner.currentState = Lifecycle.State.CREATED
                val newProvider = CompanionPanelProvider()
                service.panelProvider = newProvider
                newProvider
            } else {
                panel.log("recreate :: skip")
                this
            }
        }

        fun hide() {
            recreateIfNeed()
            viewModel.onHideNotification()
        }

    }

}