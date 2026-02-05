package clipto.common.presentation.mvvm

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import clipto.common.extensions.doOnFirstLayout
import clipto.common.presentation.mvvm.base.BaseFragment

abstract class MvvmFragment<VM : ViewModel> : BaseFragment() {

    abstract val viewModel: VM

    protected abstract fun bind(viewModel: VM)

    open fun bindOnFirstLayout(): Boolean = false

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (bindOnFirstLayout()) {
            view.doOnFirstLayout {
                viewModel.init()
                viewModel.onSubscribed(this)
                bind(viewModel)
            }
        } else {
            viewModel.init()
            viewModel.onSubscribed(this)
            bind(viewModel)
        }
    }

    @CallSuper
    override fun onDestroy() {
        viewModel.onUnsubscribed(this)
        super.onDestroy()
    }

}