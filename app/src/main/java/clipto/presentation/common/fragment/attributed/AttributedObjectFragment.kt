package clipto.presentation.common.fragment.attributed

import android.animation.AnimatorSet
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.text.InputFilter
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.common.extensions.*
import clipto.common.misc.AnimationUtils
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.ActivityBackPressConsumer
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.common.presentation.state.ViewState
import clipto.common.presentation.text.InputFilterMinMax
import clipto.domain.AttributedObject
import clipto.domain.AttributedObjectScreenState
import clipto.domain.FocusMode
import clipto.extensions.getTextColorAccent
import clipto.extensions.getTextColorPrimary
import clipto.extensions.log
import clipto.presentation.blocks.ProgressBlock
import clipto.presentation.blocks.domain.SeparatorsBlock
import clipto.presentation.blocks.layout.RowBlock
import clipto.presentation.blocks.ux.SpaceBlock
import clipto.presentation.clip.view.blocks.AttachmentsBlock
import clipto.presentation.clip.view.blocks.TextBlock
import clipto.presentation.common.fragment.attributed.blocks.*
import clipto.presentation.common.fragment.attributed.config.ConfigAttributedObjectFragment
import clipto.presentation.common.fragment.attributed.numberpicker.NumberPicker
import clipto.presentation.common.fragment.attributed.numberpicker.addProgressChangedListener
import clipto.presentation.common.recyclerview.BlockListAdapter
import clipto.presentation.common.widget.AutoCompleteTextView
import clipto.presentation.file.view.blocks.PreviewBlock
import clipto.presentation.main.list.blocks.ClipItemFolderBlock
import com.google.android.material.button.MaterialButton
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.fragment_attributed_object.*
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate

abstract class AttributedObjectFragment<O : AttributedObject, S : AttributedObjectScreenState<O>, VM : AttributedObjectViewModel<O, S>> : MvvmFragment<VM>(), ActivityBackPressConsumer,
    FragmentBackButtonListener {

    override val layoutResId: Int = R.layout.fragment_attributed_object

    private var hideActionsState = false

    protected var activeField: EditText? = null
    protected var activeFieldSelectionStart = 0
    protected var activeFieldSelectionEnd = 0
    protected var activeFieldScrollY = 0

    private val initShowPreviewListener by lazy {
        var prevValue = viewModel.linkPreviewState.canShowPreview.requireValue()
        viewModel.showPreviewLive.observe(viewLifecycleOwner) { newValue ->
            if (prevValue != newValue) {
                prevValue = newValue
                if (viewModel.isViewMode()) {
                    viewModel.onUpdateState()
                }
            }
        }
    }
    protected val onContentDescriptionListener = View.OnLongClickListener {
        if (!it.contentDescription.isNullOrBlank()) {
            viewModel.showToast(it.contentDescription)
            true
        } else {
            false
        }
    }

    private val adapter by lazy {
        BlockListAdapter<Fragment>(this)
    }

    protected abstract fun getFitViewId(): Int
    protected abstract fun createViewState(): ViewState<S>
    protected open fun hasConfig(state: S): Boolean = true

    override fun onBackPressConsumed(): Boolean {
        return getTitleView().clearSelection()
                || getDescriptionView().clearSelection()
                || getAbbreviationView().clearSelection()
                || getFitView()?.takeIf { it is EditTextExt }?.let { it as EditTextExt }?.clearSelection() == true
    }

    override fun onFragmentBackPressed(): Boolean {
        return onBackPressConsumed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        showActions()
    }

    @CallSuper
    override fun bind(viewModel: VM) {
        initBlocks()
        initLiveData()
        initFastPager()
    }

    override fun onStop() {
        view.hideKeyboard()
        super.onStop()
    }

    fun getTitleView(): EditTextExt? = rvBlocks?.getTitleView()
    fun getAbbreviationView(): EditTextExt? = rvBlocks?.getAbbreviationView()
    fun getDescriptionView(): EditTextExt? = rvBlocks?.getDescriptionView()
    fun getTagsView(): AutoCompleteTextView? = rvBlocks?.getTagsView()
    fun getDescriptionBlock(): View? = rvBlocks?.getDescriptionBlock()
    fun getFitView(): View? = rvBlocks?.getFitView()
    fun getBlocksView(): AttributedObjectRecyclerView = rvBlocks

    fun getPreviewView(): MaterialButton = mbPreview
    fun getUndoView(): MaterialButton = mbUndo
    fun getRedoView(): MaterialButton = mbRedo

    fun getMinHeight(): Int = rvBlocks?.getMinHeight() ?: 0

    protected fun showActions() {
        if (hideActionsState && !viewModel.isPreviewMode()) {
            viewModel.onHideActions(false)
            hideActionsState = false
            val clTopBarRef = clTopBar
            val clBottomBarRef = clBottomBar
            if (clTopBarRef != null && clBottomBarRef != null) {
                clTopBarRef.animation?.cancel()
                clBottomBarRef.animation?.cancel()
                val anim1 = AnimationUtils.translationY(clTopBarRef, -Units.toolbarHeight, 0f, null)!!
                val anim2 = AnimationUtils.translationY(clBottomBarRef, Units.toolbarHeight, 0f, null)!!
                AnimatorSet().apply { playTogether(anim1, anim2) }.start()
            }
        }
    }

    protected fun hideActions() {
        if (!hideActionsState && !viewModel.isEditMode()) {
            viewModel.onHideActions(true)
            hideActionsState = true
            val clTopBarRef = clTopBar
            val clBottomBarRef = clBottomBar
            if (clTopBarRef != null && clBottomBarRef != null) {
                clTopBarRef.animation?.cancel()
                clBottomBarRef.animation?.cancel()
                val anim1 = AnimationUtils.translationY(clTopBarRef, 0f, -Units.toolbarHeight, null)!!
                val anim2 = AnimationUtils.translationY(clBottomBarRef, 0f, Units.toolbarHeight, null)!!
                AnimatorSet().apply { playTogether(anim1, anim2) }.start()
            }
        }
    }

    protected fun bindAction(
        view: ImageView,
        @DrawableRes iconRes: Int,
        @StringRes contentDescriptionRes: Int,
        listener: View.OnClickListener,
    ) {
        view.contentDescription = viewModel.string(contentDescriptionRes)
        view.setOnLongClickListener(onContentDescriptionListener)
        view.setOnClickListener(listener.debounce())
        view.setImageResource(iconRes)
    }

    private fun initBlocks() {
        val rvBlocksRef = rvBlocks ?: return
        rvBlocksRef.fitViewId = getFitViewId()
        rvBlocksRef.viewModel = viewModel
        rvBlocksRef.layoutManager = AttributedObjectLayoutManager(rvBlocksRef.context, viewModel::getScreenState)
        rvBlocksRef.adapter = adapter
        rvBlocksRef.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private var scrollState: Int = -1

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                scrollState = newState
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (scrollState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    if (dy < 0) {
                        showActions()
                    } else if (dy > 0) {
                        hideActions()
                    }
                }
            }
        })
        rvBlocksRef.addOnKeyboardStateListener { _, _, _ ->
            showActions()
        }

        if (viewModel.appConfig.noteSupportFastScroll()) {
            FastScrollerBuilder(rvBlocksRef)
                .setViewHelper(RecyclerViewHelper(rvBlocksRef))
                .useMd2Style()
                .build()
        }
    }

    private fun initFastPager() {
        val hapticFeedbackSupported = viewModel.appConfig.noteSupportFastPagerHapticFeedback()
        val hapticFeedbackMaxWhenScroll = viewModel.appConfig.noteSupportFastPagerHapticFeedbackMaxWhenScroll()
        var hapticFeedback = hapticFeedbackSupported
        var prevIndex = 0

        pagePicker.addProgressChangedListener(
            progressChanged = { _, progress, fromUser ->
                val nextIndex = progress - 1
                if (fromUser) {
                    if (prevIndex != nextIndex) {
                        viewModel.onNavigate(nextIndex)
                        if (hapticFeedback) {
                            pagePicker.hapticKeyRelease()
                        }
                        prevIndex = nextIndex
                    } else if (!pagePicker.isFromTracker()) {
                        val maxIndex = viewModel.getNavigatorMaxValue() - 1
                        if (nextIndex == 0) {
                            pagePicker.setProgress(maxIndex + 1, true)
                        } else if (nextIndex == maxIndex) {
                            pagePicker.setProgress(1, true)
                        }
                    }
                }
            },
            startTrackingTouch = {
                hapticFeedback = hapticFeedbackSupported && pagePicker.maxValue <= hapticFeedbackMaxWhenScroll
                pagePicker.hapticKey()
            },
            stopTrackingTouch = {
                hapticFeedback = hapticFeedbackSupported
            }
        )

        viewModel.navigatorProgress.observe(viewLifecycleOwner) { progress ->
            pagePicker?.progress = progress
            prevIndex = progress - 1
        }
    }

    protected fun rebuildNavigator() {
        val max = viewModel.getNavigatorMaxValue()
        val min = 1
        if (min >= max) {
            pagePicker?.gone()
        } else {
            pagePicker?.maxValue = max
            pagePicker?.minValue = min
            pagePicker?.editText?.filters = arrayOf(
                InputFilterMinMax(min, max),
                InputFilter.LengthFilter(max.toString().length)
            )
            viewModel.onInitNavigator {
                viewModel.navigatorProgress.postValue(it + 1)
            }
        }
    }

    private fun initLiveData() {
        val ctx = requireContext()
        val viewState = createViewState()
        viewState
            .addLayer(
                object : ViewState.Layer<S, NumberPicker>(pagePicker, "action_note_pager") {
                    override fun canApply(state: S): Boolean = viewModel.hasNavigator()
                    override fun doApply(state: S) = rebuildNavigator()
                }
            )
            .addLayer(
                object : ViewState.Layer<S, ImageView>(iv6, "action_config") {
                    override fun canApply(state: S): Boolean = hasConfig(state)
                    override fun doApply(state: S) {
                        bindAction(layerView, R.drawable.ic_tune, R.string.menu_display_options) {
                            storeActiveFieldState()
                            ConfigAttributedObjectFragment.show(requireActivity())
                        }
                    }
                }
            )
            .addLayer(
                object : ViewState.Layer<S, TextView>(tvTitle, "title") {
                    override fun canApply(state: S): Boolean = !state.title.isNullOrBlank()
                    override fun doApply(state: S) {
                        layerView.text = state.title
                    }
                }
            )
            .addLayer(
                object : ViewState.Layer<S, ImageView>(ivIcon, "title_icon") {
                    override fun canApply(state: S): Boolean = state.iconRes != null
                    override fun doApply(state: S) {
                        state.iconRes?.let { iconRes ->
                            layerView.setImageResource(iconRes)
                            val color = state.iconColor ?: ctx.getTextColorPrimary()
                            layerView.imageTintList = ColorStateList.valueOf(color)
                        }
                    }
                }
            )

        val textColorPrimary = ColorStateList.valueOf(ctx.getTextColorPrimary())
        val textColorAccent = ColorStateList.valueOf(ctx.getTextColorAccent())

        viewModel.dismissLive.observe(viewLifecycleOwner) {
            navigateUp()
        }

        viewModel.autoSaveState.getLiveData().observe(viewLifecycleOwner) {
            autoSaveIconView?.withRune(it.first, it.second)
        }

        viewModel.contentChangedLive.observe(viewLifecycleOwner) {
            iv5?.imageTintList = if (it) textColorAccent else textColorPrimary
        }

        viewModel.nextFocusLive.observe(viewLifecycleOwner) {
            when (it) {
                FocusMode.DESCRIPTION -> getDescriptionView()?.requestFocusFromTouch()
                FocusMode.TITLE -> getTitleView()?.requestFocusFromTouch()
                FocusMode.TEXT -> getFitView()?.requestFocusFromTouch()
                FocusMode.TAGS -> getTagsView()?.requestFocusFromTouch()
                else -> Unit
            }
        }

        viewModel.screenStateLive.observe(viewLifecycleOwner) { state ->
            if (state != null) {
                viewModel.onCreateBlocks(state) { blocks ->
                    adapter.submitList(blocks) {
                        if (state.focusMode == FocusMode.NONE) {
                            rvBlocks?.scrollToPosition(0)
                        }
                    }
                }
                if (!state.isPreviewMode()) {
                    viewState.rebuild(state)
                }
                initShowPreviewListener
                when {
                    state.isViewMode() -> view.hideKeyboard()
                    state.isPreviewMode() -> hideActions()
                    else -> showActions()
                }
                rvBlocks?.alpha = if (state.value.isDeleted()) 0.6f else 1f
            }
        }

        viewModel.getActiveViewLive().observe(viewLifecycleOwner) {
            if (this::class.java == it) {
                restoreActiveFieldState()
            }
        }
    }

    inner class RecyclerViewHelper(
        private val mView: RecyclerView,
        private val mPopupTextProvider: PopupTextProvider? = null
    ) : FastScroller.ViewHelper {
        private val mTempRect = Rect()
        override fun addOnPreDrawListener(onPreDraw: Runnable) {
            mView.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun onDraw(
                    canvas: Canvas, parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    onPreDraw.run()
                }
            })
        }

        override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
            mView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onScrollChanged.run()
                }
            })
        }

        override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent>) {
            mView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView,
                    event: MotionEvent
                ): Boolean {
                    return onTouchEvent.test(event)
                }

                override fun onTouchEvent(
                    recyclerView: RecyclerView,
                    event: MotionEvent
                ) {
                    onTouchEvent.test(event)
                }
            })
        }

        override fun getScrollRange(): Int {
            val itemCount = itemCount
            if (itemCount == 0) return 0
            val itemsHeight = adapter.currentList.mapIndexed { index, _ -> getItemHeight(index) }.sum()
            return mView.paddingTop + mView.paddingBottom + itemsHeight
        }

        override fun getScrollOffset(): Int {
            val firstItemPosition = firstItemPosition
            if (firstItemPosition == RecyclerView.NO_POSITION) return 0
            val firstItemTop = firstItemOffset
            return mView.paddingTop + getOffsetHeight(firstItemPosition) - firstItemTop
        }

        override fun scrollTo(offset: Int) {
            var newOffset = offset
            mView.stopScroll()
            newOffset -= mView.paddingTop
            val firstItemTop = getOffsetHeight(firstItemPosition) - newOffset
            scrollToPositionWithOffset(firstItemPosition, firstItemTop)
            if (offset == 0) {
                showActions()
            } else {
                hideActions()
            }
        }

        override fun getPopupText(): String? {
            var popupTextProvider = mPopupTextProvider
            if (popupTextProvider == null) {
                val adapter = mView.adapter
                if (adapter is PopupTextProvider) {
                    popupTextProvider = adapter
                }
            }
            if (popupTextProvider == null) {
                return null
            }
            val position = firstItemAdapterPosition
            return if (position == RecyclerView.NO_POSITION) {
                null
            } else popupTextProvider.getPopupText(position)
        }

        private val itemCount: Int
            get() {
                val linearLayoutManager = verticalLinearLayoutManager ?: return 0
                var itemCount = linearLayoutManager.itemCount
                if (itemCount == 0) {
                    return 0
                }
                if (linearLayoutManager is GridLayoutManager) {
                    itemCount = (itemCount - 1) / linearLayoutManager.spanCount + 1
                }
                return itemCount
            }

        private fun getOffsetHeight(position: Int): Int {
            var sum = 0
            for (i in 0 until position) {
                sum += getItemHeight(i)
            }
            return sum
        }

        private fun getItemHeight(index: Int): Int {
            val items = adapter.currentList
            val item = items.getOrNull(index) ?: return 0
            return when (item.javaClass) {
                TextBlock::class.java,
                PreviewBlock::class.java -> {
                    val view = item.getContentView() ?: getFitView()
                    if (view is TextView && view.lineCount >= viewModel.appConfig.getFastScrollBarMinTextLines()) {
                        mView.getDecoratedBoundsWithMargins(view, mTempRect)
                        mTempRect.height()
                    } else {
                        0
                    }
                }
                SeparatorsBlock::class.java -> {
                    val view = item.getContentView() ?: rvBlocks?.findViewById(R.id.clSeparators)
                    if (view != null) {
                        mView.getDecoratedBoundsWithMargins(view, mTempRect)
                        mTempRect.height()
                    } else {
                        0
                    }
                }
                DescriptionBlock::class.java -> {
                    val view = item.getContentView() ?: getDescriptionBlock()
                    if (view != null) {
                        mView.getDecoratedBoundsWithMargins(view, mTempRect)
                        mTempRect.height()
                    } else {
                        0
                    }
                }
                AbbreviationBlock::class.java -> Units.toolbarHeight.toInt()
                TitleBlock::class.java -> Units.toolbarHeight.toInt()
                AttachmentsBlock::class.java -> Units.toolbarHeightSm.toInt()
                AddTagBlock::class.java -> Units.toolbarHeight.toInt()
                TagsBlock::class.java -> Units.toolbarHeightSm.toInt()
                SpaceBlock::class.java -> (item as SpaceBlock).heightInDp
                RowBlock::class.java -> Units.toolbarHeight.toInt()
                ProgressBlock::class.java -> Units.toolbarHeight.toInt()
                else -> {
                    val view = item.getContentView()
                    log("calc height by using default logic :: {} -> {}", item, view)
                    if (view != null) {
                        mView.getDecoratedBoundsWithMargins(view, mTempRect)
                        mTempRect.height()
                    } else {
                        0
                    }
                }
            }
        }

        private val firstItemPosition: Int
            get() {
                var position = firstItemAdapterPosition
                val linearLayoutManager = verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
                if (linearLayoutManager is GridLayoutManager) {
                    position /= linearLayoutManager.spanCount
                }
                return position
            }
        private val firstItemAdapterPosition: Int
            get() {
                if (mView.childCount == 0) {
                    return RecyclerView.NO_POSITION
                }
                val itemView = mView.getChildAt(0)
                val linearLayoutManager = verticalLinearLayoutManager ?: return RecyclerView.NO_POSITION
                return linearLayoutManager.getPosition(itemView)
            }
        private val firstItemOffset: Int
            get() {
                if (mView.childCount == 0) {
                    return RecyclerView.NO_POSITION
                }
                val itemView = mView.getChildAt(0)
                mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
                return mTempRect.top
            }

        private fun scrollToPositionWithOffset(position: Int, offset: Int) {
            var newPosition = position
            var newOffset = offset
            val linearLayoutManager = verticalLinearLayoutManager ?: return
            if (linearLayoutManager is GridLayoutManager) {
                newPosition *= linearLayoutManager.spanCount
            }
            newOffset -= mView.paddingTop
            linearLayoutManager.scrollToPositionWithOffset(newPosition, newOffset)
        }

        private val verticalLinearLayoutManager: LinearLayoutManager?
            get() {
                val layoutManager = mView.layoutManager as? LinearLayoutManager ?: return null
                return if (layoutManager.orientation != RecyclerView.VERTICAL) {
                    null
                } else layoutManager
            }

    }

    fun storeActiveFieldState(): FocusMode? {
        try {
            val focusedField = rvBlocks.findFocus()
            activeField = if (focusedField is EditText) focusedField else null
            activeFieldSelectionStart = activeField?.selectionStart ?: 0
            activeFieldSelectionEnd = activeField?.selectionEnd ?: 0
            activeFieldScrollY = activeField?.scrollY ?: 0
            val isEditMode = viewModel.isEditMode()
            return if (isEditMode && activeField != null) {
                when {
                    activeField === getFitView() -> FocusMode.TEXT
                    activeField === getTitleView() -> FocusMode.TITLE
                    activeField === getTagsView() -> FocusMode.TAGS
                    activeField === getDescriptionView() -> FocusMode.DESCRIPTION
                    activeField === getAbbreviationView() -> FocusMode.ABBREVIATION
                    else -> null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            return null
        }
    }

    fun restoreActiveFieldState() {
        activeField?.let {
            runCatching {
                it.setSelection(activeFieldSelectionStart, activeFieldSelectionEnd)
//                it.scrollY = activeFieldScrollY
                if (!viewModel.isViewMode()) {
                    it.showKeyboard()
                } else {
//                    it.requestFocus()
                }
            }
        }
    }

}