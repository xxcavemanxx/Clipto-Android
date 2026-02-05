package clipto.common.presentation.mvvm.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.StringRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import clipto.common.R
import clipto.common.extensions.getNavController
import clipto.common.presentation.mvvm.ViewModel

abstract class BaseFragment : Fragment(), AppFragment {

    protected abstract val layoutResId: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(layoutResId, container, false)

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

    protected fun withDefaults(
        toolbar: Toolbar,
        @StringRes titleRes: Int = 0,
        onBackPressed: () -> Unit = { navigateUp() }
    ): Toolbar {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { onBackPressed.invoke() }
        if (titleRes != 0) {
            toolbar.setTitle(titleRes)
        }
        return toolbar
    }

    protected open fun navigateUp(): Boolean = runCatching { getNavController().navigateUp() }.getOrDefault(true)

}