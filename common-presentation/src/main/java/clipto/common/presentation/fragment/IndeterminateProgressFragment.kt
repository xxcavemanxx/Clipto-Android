package clipto.common.presentation.fragment

import clipto.common.databinding.FragmentIndeterminateProgressBinding
import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentManager
import clipto.common.R
import clipto.common.presentation.mvvm.base.BaseDialogFragment

class IndeterminateProgressFragment : BaseDialogFragment() {

    
    private var _binding: FragmentIndeterminateProgressBinding? = null
    private val binding get() = _binding!!
override var withNoTitle: Boolean = true
    override val layoutResId: Int = R.layout.fragment_indeterminate_progress
    override var withSizeLimits: SizeLimits? = SizeLimits(widthMultiplier = 1f, onSizeChanged = { binding.contentView?.requestLayout() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            isCancelable = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentIndeterminateProgressBinding.bind(view)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
