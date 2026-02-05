package clipto.common.presentation.mvvm

import android.os.Bundle
import androidx.annotation.CallSuper
import clipto.common.presentation.mvvm.base.BaseNavigationActivity

abstract class MvvmNavigationActivity<VM : ViewModel> : BaseNavigationActivity() {

    protected abstract val viewModel: VM

    protected abstract fun bind(viewModel: VM)

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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