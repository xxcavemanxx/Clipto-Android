package clipto.common.presentation.mvvm

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.fragment.app.FragmentManager
import clipto.common.extensions.onBackPressDeclined
import clipto.common.presentation.mvvm.base.BaseActivity
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener

abstract class MvvmActivity<VM : ViewModel> : BaseActivity() {

    abstract val viewModel: VM

    protected abstract fun bind(viewModel: VM)

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

    override fun onBackPressed() {
        val fm = supportFragmentManager
        if (!onBackPressDeclined() && !onBackPressed(fm)) {
            super.onBackPressed()
        }
    }

    private fun onBackPressed(fm: FragmentManager): Boolean {
        val fragments = fm.fragments
        var result: Boolean
        if (fragments.isNotEmpty()) {
            for (frag in fragments) {
                if (frag != null && frag.isAdded) {
                    if (frag is FragmentBackButtonListener && frag.onFragmentBackPressed()) {
                        return true
                    }
                    result = onBackPressed(frag.childFragmentManager)
                    if (result) return true
                }
            }
        }
        if (fm.backStackEntryCount > 0) {
            fm.popBackStack()
            fm.executePendingTransactions()
            return true
        }
        return false
    }

}