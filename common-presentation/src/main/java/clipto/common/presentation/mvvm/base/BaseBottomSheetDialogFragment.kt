package clipto.common.presentation.mvvm.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.view.ContextThemeWrapper
import clipto.common.R
import clipto.common.presentation.mvvm.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment(), AppFragment {

    protected abstract val layoutResId: Int

    override fun getTheme(): Int = R.style.ThemeOverlay_MyTheme_BottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val act = requireActivity()
        val contextThemeWrapper = ContextThemeWrapper(act, act.theme)
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        return localInflater.inflate(layoutResId, container, false)
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

}