package clipto.common.presentation.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import clipto.common.R
import clipto.common.presentation.mvvm.base.BaseDialogFragment
import kotlinx.android.synthetic.main.fragment_indeterminate_progress.*

class IndeterminateProgressFragment : BaseDialogFragment() {

    override var withNoTitle: Boolean = true
    override val layoutResId: Int = R.layout.fragment_indeterminate_progress
    override var withSizeLimits: SizeLimits? = SizeLimits(widthMultiplier = 1f, onSizeChanged = { contentView?.requestLayout() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            isCancelable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val timeout = requireArguments().getLong(ATTR_TIMEOUT)
        view.postDelayed({ dialog?.dismiss() }, timeout)
    }

    override fun onResume() {
        super.onResume()
        if (!isActive) {
            dismiss()
        }
    }

    companion object {
        private const val ATTR_TIMEOUT = "attr_timeout"

        private const val TAG = "IndeterminateProgressFragment"

        @Volatile
        private var isActive = false

        fun show(
            fragmentManager: FragmentManager,
            timeout: Long = 5000
        ) {
            if (fragmentManager.isDestroyed || fragmentManager.isStateSaved) {
                return
            }
            isActive = true
            IndeterminateProgressFragment()
                .apply {
                    arguments = Bundle().apply {
                        putLong(ATTR_TIMEOUT, timeout)
                    }
                }
                .show(fragmentManager, TAG)
        }

        fun hide(fragmentManager: FragmentManager) {
            if (fragmentManager.isDestroyed || fragmentManager.isStateSaved) {
                return
            }
            isActive = false
            fragmentManager.findFragmentByTag(TAG)?.let {
                it as IndeterminateProgressFragment
                it.dismissAllowingStateLoss()
            }
        }
    }
}