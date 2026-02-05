package clipto.presentation.runes.keyboard_companion.panel

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.*
import clipto.common.misc.Units
import clipto.extensions.*
import clipto.presentation.clip.list.ClipListAdapter
import clipto.presentation.common.recyclerview.FlowLayoutManagerExt
import clipto.presentation.runes.keyboard_companion.*
import com.google.android.material.button.MaterialButton
import com.wb.clipboard.R
import com.xiaofeng.flowlayoutmanager.Alignment
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ClickableViewAccessibility")
class CompanionPanel(
        val service: CompanionService,
        val viewModel: CompanionViewModel) : LifecycleOwner {

    companion object {
        const val FLAGS_FLOATING = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        const val FLAGS_FOCUSABLE = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    private val state = viewModel.companionState
    private val lastEventStateCheckDelay = viewModel.getLastEventStateCheckDelay()
    private val lastEventStateCheckHandler = Handler(Looper.getMainLooper())
    private val lastEventStateCheckTask = Runnable {
        bind(event = null, cleanup = true)
    }

    var lastEventInfo: CompanionEventInfo? = null
    var lastPanelState: IPanelState? = null
    var lastEventTime: Long = 0

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val owner = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = owner

    val showClipsActionView: MaterialButton by lazy { view.findViewById(R.id.showClipsActionView) }
    val filterOpenStateIcon: ImageView by lazy { view.findViewById(R.id.filterOpenState) }
    val filterCounterView: TextView by lazy { view.findViewById(R.id.filterCounterView) }
    val minimizeActionView: View by lazy { view.findViewById(R.id.minimizeActionView) }
    val filterIconView: ImageView by lazy { view.findViewById(R.id.filterIconView) }
    val recyclerView: RecyclerView by lazy { view.findViewById(R.id.recycleView) }
    val undoView: MaterialButton by lazy { view.findViewById(R.id.undoView) }
    val redoView: MaterialButton by lazy { view.findViewById(R.id.redoView) }
    val counterView: TextView by lazy { view.findViewById(R.id.counterView) }
    val detailsPanel: View by lazy { view.findViewById(R.id.detailsPanel) }
    val searchView: EditText by lazy { view.findViewById(R.id.searchView) }
    val titleView: TextView by lazy { view.findViewById(R.id.titleView) }
    val resizeView: View by lazy { view.findViewById(R.id.resizeView) }
    val filterView: View by lazy { view.findViewById(R.id.filterView) }
    val emptyView: View by lazy { view.findViewById(R.id.emptyView) }
    val toolbar: View by lazy { view.findViewById(R.id.toolbar) }

    init {
        val mode = if (viewModel.isUserHidden()) CompanionMode.HIDDEN else CompanionMode.AUTO_DETECTED
        state.mode.setValue(mode)
    }

    fun isInitialized(): Boolean = contentView.isInitialized()
    fun isUserFocused(): Boolean = state.isUserSearch()
    fun isHidden(): Boolean = state.isHidden()

    private val filtersAdapter = FilterListAdapter {
        viewModel.onSearch()
    }

    private val clipsAdapter = ClipListAdapter(
            context = service,
            withMultiSelect = false,
            withMainState = viewModel.mainState,
            withCopyActionSize = Units.DP.toPx(40f).toInt(),
            withTextConstraint = { viewModel.getTextLike() },
            withCopyHandler = { clip, _ -> viewModel.onCopy(clip) },
            withLinkPreviewWidth = viewModel.getPanelWidth() - Units.DP.toPx(104f).toInt(),
            withClickHandler = { clip, _ -> lastPanelState?.onClick(clip, lastEventInfo ?: CompanionEventInfo()) }
    )

    private val states = listOf<IPanelState>(
            EditTextContextPanelState(this),
            EditTextPanelState(this),
            DetachedPanelState(this)
    )

    private val viewParams by lazy {
        val type =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_PHONE
                } else {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
        val params = WindowManager.LayoutParams(type, FLAGS_FLOATING, PixelFormat.TRANSLUCENT)
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.x = viewModel.getX()
        params.y = viewModel.getY()
        params
    }

    private val view: View by lazy {
        val themeId = viewModel.getTheme().themeId
        viewModel.app.setTheme(themeId)
        val theme = viewModel.app.theme
        theme.applyStyle(themeId, true)
        val contextThemeWrapper = ContextThemeWrapper(viewModel.app, theme)
        val layoutInflater = service.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val localInflater = layoutInflater.cloneInContext(contextThemeWrapper)
        val viewToAdd = localInflater.inflate(R.layout.view_keyboard_companion, null)
        windowManager.addView(viewToAdd, viewParams)
        viewToAdd
    }

    private val contentView = lazy {
        val panelWidth = viewModel.getPanelWidth()
        val panelHeight = viewModel.getPanelHeight()

        toolbar.layoutParams?.width = panelWidth

        detailsPanel.layoutParams?.apply {
            height = panelHeight
            width = panelWidth
        }

        toolbar.registerDraggableTouchListener(
                { Point(viewParams.x, viewParams.y) },
                { x, y ->
                    viewParams.x = x
                    viewParams.y = y
                    withFloatingLayout()
                }
        )

        resizeView.registerDraggableTouchListener(
                {
                    resizeView.hapticKeyRelease()
                    Point(detailsPanel.layoutParams?.width ?: viewModel.getPanelWidth(), detailsPanel.layoutParams?.height ?: viewModel.getPanelHeight())
                },
                { x, y ->
                    detailsPanel.layoutParams?.apply {
                        height = min(viewModel.panelMaxHeight, max(viewModel.panelMinHeight, y))
                        width = min(viewModel.panelMaxWidth, max(viewModel.panelMinWidth, x))
                        toolbar.layoutParams?.width = width
                        withFloatingLayout()
                    }
                }
        )

        minimizeActionView.setOnLongClickListener {
            Toast.makeText(it.context, R.string.runes_texpander_hint_hide, Toast.LENGTH_LONG).show()
            true
        }

        minimizeActionView.setOnClickListener {
            modeUserHidden()
        }

        searchView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                modeUserSearchEnter()
            }
            false
        }

        searchView.doOnTextChanged { text, _, _, _ ->
            val textStr = text.toNullIfEmpty()
            if (viewModel.getTextLike() != textStr) {
                viewModel.onSearch(textStr)
            }
        }

        filterView.setOnClickListener {
            if (recyclerView.adapter is FilterListAdapter) {
                bindClipsAdapter()
            } else {
                bindFiltersAdapter()
            }
        }

        showClipsActionView.setOnClickListener {
            val visible = !detailsPanel.isVisible()
            showHideDetails(visible)
        }

        undoView.setOnClickListener {
            lastPanelState?.onUndo()
        }

        redoView.setOnClickListener {
            lastPanelState?.onRedo()
        }

        (view as CompanionContentLayout).setListener {
            log("enter focusable state: {}", it)
            if (!it) {
                modeUserSearchExit()
            }
        }

        viewModel.filtersLive.observe(this) { filters ->
            filtersAdapter.submitList(filters)
        }

        viewModel.clipsLive.observe(this) { list ->
            filterIconView.imageTintList = ColorStateList.valueOf(viewModel.getFilterColor())
            if (searchView.text.toNullIfEmpty() != viewModel.getTextLike()) {
                searchView.setText(viewModel.getTextLike())
            }
            filterCounterView.text = list.size.toString().inBrackets()
            clipsAdapter.submitList(list) {
                if (recyclerView.adapter == clipsAdapter) {
                    clipsAdapter.reconfigure(recyclerView, viewModel.getListConfig())
                    emptyView.setVisibleOrGone(list.isEmpty())
                } else {
                    emptyView.gone()
                }
            }
        }

        viewModel.clipboardState.clip.getLiveData().observe(this) {
            clipsAdapter.updateActive(it)
        }

        view
    }

    fun withFloatingLayout(withCallback: Boolean = false, callback: () -> Unit = {}) {
        if (isInitialized()) {
            viewParams.flags = FLAGS_FLOATING
            runCatching { windowManager.updateViewLayout(contentView.value, viewParams) }
            if (withCallback) {
                view.postDelayed({ callback.invoke() }, viewModel.getLayoutDelay())
            }
        }
    }

    fun withFocusableLayout() {
        if (isInitialized()) {
            viewParams.flags = FLAGS_FOCUSABLE
            runCatching { windowManager.updateViewLayout(contentView.value, viewParams) }
        }
    }

    private fun bindFiltersAdapter() {
        filterOpenStateIcon.setImageResource(R.drawable.texpander_collapse)
        if (recyclerView.adapter != filtersAdapter) {
            val padding = Units.DP.toPx(12f).toInt()
            emptyView.gone()
            recyclerView.updatePadding(top = padding, left = padding, right = padding)
            recyclerView.layoutManager = FlowLayoutManagerExt().apply {
                setAlignment(Alignment.LEFT)
            }
            recyclerView.removeDecorations()
            recyclerView.adapter = filtersAdapter
            recyclerView.scrollToPosition(0)
            modeUserSearchEnter(true)
        }
    }

    fun bindClipsAdapter() {
        filterOpenStateIcon.setImageResource(R.drawable.texpander_expand)
        if (recyclerView.adapter != clipsAdapter) {
            val padding = 0
            emptyView.setVisibleOrGone(!viewModel.hasFilteredNotes())
            recyclerView.updatePadding(top = padding, left = padding, right = padding)
            clipsAdapter.reconfigure(recyclerView, viewModel.getListConfig(), force = true)
            recyclerView.scrollToPosition(0)
            modeUserSearchExit { it.floating }
        }
    }

    private fun modeUserSearchEnter(floating: Boolean = false) = lastPanelState
            ?.takeIf { it !is UserSearchPanelState || it.floating }
            ?.let { it.unwrap() }
            ?.let { prevState ->
                log("modeUserSearchEnter: {}", state.mode.requireValue())
                val event = lastEventInfo ?: CompanionEventInfo()
                val newState = UserSearchPanelState(this, prevState, state.mode.requireValue(), event, floating)
                lastPanelState = newState
                newState.apply(event)
            }

    private fun modeUserSearchExit(canExit: (state: UserSearchPanelState) -> Boolean = { true }) = lastPanelState
            ?.takeIf { it is UserSearchPanelState && canExit.invoke(it) }
            ?.let {
                log("modeUserSearchExit: {}", state.mode.requireValue())
                it.onUndo()
            }

    fun modeUserHidden() {
        log("modeUserHidden")
        applyState(null, null)
        state.mode.setValue(CompanionMode.HIDDEN)
        owner.currentState = Lifecycle.State.CREATED
        onSaveState(true)
    }

    fun modeAutoDetected() {
        log("modeAutoDetected")
        state.mode.setValue(CompanionMode.AUTO_DETECTED)
        bind(null)
        if (detailsPanel.isVisible()) {
            owner.currentState = Lifecycle.State.STARTED
        }
        onSaveState(false)
    }

    fun bind(event: AccessibilityEvent?, cleanup: Boolean = false) {
        runCatching {
            if (viewModel.appState.isLastActivityContextActions()) {
                log("skip state due to context actions")
                applyState(null, null)
                return
            }

            if (event != null) {
                if (event.isInternal(service) && !event.isEditText()) {
                    log("skip state due to internal window state changed")
                    applyState()
                    return
                }
//                if (event.isWindowStateChanged() && event.isInputService()) {
//                    log("skip state due to inputservice")
//                    applyState(recheck = true)
//                    return
//                }
                if (event.isWindowStateChanged() && event.packageName == lastEventInfo?.nodeInfo?.packageName) {
                    log("skip state due to the same package")
                    applyState()
                    return
                }
                if (event.isWindowStateChanged() && !event.isSystemPackage() && findFocus() == null) {
                    log("skip state due to not system package")
                    applyState()
                    return
                }
                val prevEventInfo = lastEventInfo
                if (prevEventInfo != null
                        && event.isViewClicked()
                        && event.className == prevEventInfo.getClassName()
                        && event.packageName == prevEventInfo.getPackageName()) {
                    log("found similar? :: {} - {}", event.text, prevEventInfo)
                    if (event.source == null) {
                        log("skip state due to the same event sent twice")
                        applyState()
                        return
                    }
                    if (event.text == prevEventInfo.getEventText()) {
                        log("skip state due to the same field clicked")
                        applyState()
                        return
                    }
                    if (prevEventInfo.isViewFocused() && System.currentTimeMillis() - lastEventTime <= viewModel.getEventThreshold()) {
                        log("skip state due to threshold")
                        applyState()
                        return
                    }
                }
            }
            if (state.isDetached()) {
                log("start auto detecting")
                state.mode.setValue(CompanionMode.AUTO_DETECTED)
            }
            lastEventStateCheckHandler.removeCallbacks(lastEventStateCheckTask)
            var newEventInfo = CompanionEventInfo(event)
            var newState = states.find { it.test(newEventInfo) }
            log("found state: newState={}", newState)
            if (newState != null) {
                applyState(newState, newEventInfo)
                if (!cleanup && newState is DetachedPanelState) {
                    log("schedule recheck")
                    lastEventStateCheckHandler.postDelayed(lastEventStateCheckTask, lastEventStateCheckDelay)
                }
            } else {
                if ((event == null && !cleanup) || !viewModel.canAutoHide()) {
                    log("set detached state")
                    state.mode.setValue(CompanionMode.DETACHED)
                }
                val focusedNode = findFocus()
                newEventInfo = CompanionEventInfo(event, focusedNode)
                newState = states.find { it.test(newEventInfo) }
                log("found focus (1): {} - {}", focusedNode, newState)
                val lastEventInfoRef = lastEventInfo
                if (newState == null
                        && lastEventInfoRef != null
                        && lastEventInfoRef.freshNodeInfo()?.isFocused == true
                        && newEventInfo.getPackageName() == lastEventInfoRef.getPackageName()) {
                    newEventInfo = lastEventInfoRef
                    newState = states.find { it.test(newEventInfo) }
                    log("found focus (2): {} - {}", focusedNode, newState)
                }
                if (newState != null) {
                    applyState(newState, newEventInfo)
                    if (!cleanup && newState !is DetachedPanelState) {
                        log("schedule cleanup")
                        lastEventStateCheckHandler.postDelayed(lastEventStateCheckTask, lastEventStateCheckDelay)
                    }
                } else {
                    applyState(null, null)
                }
            }
        }
    }

    private fun findFocus() = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

    fun unbind() {
        owner.currentState = Lifecycle.State.CREATED
        if (isInitialized()) {
            runCatching { windowManager.removeViewImmediate(contentView.value) }
            onSaveState()
        }
        if (viewModel.isEnabled()) {
            viewModel.onShowNotification()
        } else {
            viewModel.onHideNotification()
        }
    }

    fun showHideDetails(visible: Boolean) {
        if (visible) {
            bindClipsAdapter()
            detailsPanel.visible()
            owner.currentState = Lifecycle.State.STARTED
            showClipsActionView.setIconResource(R.drawable.texpander_collapse)
            lastPanelState?.onShowDetails()
        } else {
            detailsPanel.gone()
            owner.currentState = Lifecycle.State.CREATED
            showClipsActionView.setIconResource(R.drawable.texpander_expand)
            lastPanelState?.onHideDetails()
            lastEventInfo?.let { lastPanelState?.apply(it) }
        }
        withFloatingLayout()
    }

    private fun applyState(newPanelState: IPanelState? = lastPanelState, newEventInfo: CompanionEventInfo? = lastEventInfo) {
        lastEventInfo = newEventInfo
        lastPanelState = newPanelState
        lastEventTime = System.currentTimeMillis()
        log("apply state :: {}, eventClass={}", newPanelState, newEventInfo?.getClassName())
        if (newPanelState != null && newEventInfo != null) {
            newPanelState.apply(newEventInfo)
            contentView.value.visible()
        } else {
            applyStateAutoHidden()
        }
        withFloatingLayout()
    }

    private fun applyStateAutoHidden() {
        log("applyStateAutoHidden")
        state.mode.setValue(CompanionMode.HIDDEN)
        this.lastEventInfo = null
        this.lastPanelState = null
        states.forEach { it.clear() }
        if (isInitialized()) {
            contentView.value.gone()
            onSaveState(false)
        }
        viewModel.onShowNotification()
    }

    private fun onSaveState(userHidden: Boolean? = null) {
        log("onSaveState: x={}, y={}, widht={}, height={}", viewParams.x, viewParams.y, detailsPanel.layoutParams?.width, detailsPanel.layoutParams?.height)
        viewModel.onSaveState(
                newX = viewParams.x,
                newY = viewParams.y,
                newWidth = detailsPanel.layoutParams?.width,
                newHeight = detailsPanel.layoutParams?.height,
                userHidden = userHidden
        )
    }

    internal fun log(text: String, vararg params: Any?) = viewModel.log(text, *params)

}