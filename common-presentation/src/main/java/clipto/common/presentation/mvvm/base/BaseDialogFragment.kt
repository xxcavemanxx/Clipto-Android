package clipto.common.presentation.mvvm.base

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import clipto.common.extensions.getNavController
import clipto.common.misc.Units
import clipto.common.presentation.mvvm.ViewModel
import kotlin.math.min

abstract class BaseDialogFragment : DialogFragment(), AppFragment {

    protected open var withSizeLimits: SizeLimits? = null
    protected open var withNoTitle: Boolean = false

    protected abstract val layoutResId: Int

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (withNoTitle) {
            setStyle(STYLE_NO_TITLE, 0)
        }
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (withNoTitle) {
            dialog?.window?.run {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                requestFeature(Window.FEATURE_NO_TITLE)
            }
        }
        return inflater.inflate(layoutResId, container, false)
    }

    @CallSuper
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySizeLimits()
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        applySizeLimits()
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        applySizeLimits()
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewModel.addViewToQueue(this)
        super.onViewCreated(view, savedInstanceState)
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        ViewModel.removeViewFromQueue(this)
    }

    private fun applySizeLimits() {
        withSizeLimits?.let { limits ->
            dialog?.window?.let {
                when {
                    limits.maxWidth != null && limits.widthMultiplier != null -> {
                        val maxWidth = Units.DP.toPx(limits.maxWidth).toInt()
                        val width = min((resources.displayMetrics.widthPixels * limits.widthMultiplier).toInt(), maxWidth)
                        val height = WindowManager.LayoutParams.WRAP_CONTENT
                        it.setLayout(width, height)
                    }
                    limits.widthMultiplier != null -> {
                        val width = (resources.displayMetrics.widthPixels * limits.widthMultiplier).toInt()
                        val height = WindowManager.LayoutParams.WRAP_CONTENT
                        it.setLayout(width, height)
                    }
                    else -> throw IllegalArgumentException("unhandled condition")
                }
                limits.onSizeChanged
            }
        }
    }

    protected fun navigateUp(): Boolean = getNavController().navigateUp()

    data class SizeLimits(
        val widthMultiplier: Float? = null,
        val maxWidth: Float? = null,
        val onSizeChanged: () -> Unit = {}
    )

}