package clipto.presentation.common.fragment.attributed

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditTextExt
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import clipto.common.misc.Units
import clipto.presentation.common.widget.AutoCompleteTextView
import com.wb.clipboard.R

class AttributedObjectRecyclerView : RecyclerView {

    lateinit var viewModel: AttributedObjectViewModel<*, *>

    private var tagsView: AutoCompleteTextView? = null
    private var titleView: EditTextExt? = null
    private var minHeight: Int = -1

    var fitViewId: Int = 0

    init {
        setItemViewCacheSize(10)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun hasOverlappingRendering(): Boolean = false

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        val newHeight = b - t
        if (newHeight != minHeight) {
            minHeight = newHeight
            getFitView()
        }
    }

    fun getMinHeight(): Int {
        val h = minHeight.takeIf { it > 0 } ?: height
        return if (viewModel.isEditMode()) {
            maxOf(0, h - Units.toolbarHeight.toInt() * 2)
        } else {
            maxOf(0, h - Units.toolbarHeight.toInt())
        }
    }

    fun getTagsView(): AutoCompleteTextView? {
        var ref = tagsView
        if (ref == null) {
            ref = findViewById(R.id.tagsEditText)
        }
        return ref
    }

    fun getTitleView(): EditTextExt? {
        var ref = titleView
        if (ref == null) {
            ref = findViewById(R.id.etClipTitle)
        }
        return ref
    }

    fun getDescriptionBlock(): View? = findViewById(R.id.flClipDescriptionBlock)

    fun getDescriptionView(): EditTextExt? = findViewById(R.id.etClipDescription)

    fun getAbbreviationView(): EditTextExt? = findViewById(R.id.etClipAbbreviation)

    fun getFitView(): View? {
        if (fitViewId == 0) return null
        val fitView: View = findViewById(fitViewId) ?: return null
        val newHeight = getMinHeight()
        if (fitView is TextView) {
            fitView.takeIf { it.minHeight != newHeight }?.minHeight = newHeight
        } else {
            fitView.takeIf { it.minimumHeight != newHeight }?.minimumHeight = newHeight
        }
        return fitView
    }

}