package clipto.common.presentation.mvvm

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import clipto.common.presentation.mvvm.base.BaseDialogFragment

abstract class MvvmDialogFragment<VM : ViewModel> : BaseDialogFragment() {

    protected abstract val viewModel: VM

    protected abstract fun bind(viewModel: VM)

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init()
        viewModel.onSubscribed(this)
        bind(viewModel)
    }

    @CallSuper
    override fun onDestroy() {
        viewModel.onUnsubscribed(this)
        super.onDestroy()
    }

}