package clipto.presentation.clip.details.pages

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.ViewModel
import clipto.common.presentation.state.ViewPagerState
import clipto.domain.ClipDetailsTab
import clipto.extensions.getLabel
import clipto.presentation.clip.details.ClipDetailsFragment
import com.wb.clipboard.R

abstract class PageFragment<VM : ViewModel> : MvvmFragment<VM>() {

    override val layoutResId: Int = R.layout.fragment_clip_details_page

    private val rv by lazy {
        val recyclerView = getRecyclerView()
        bind(recyclerView, viewModel)
        recyclerView
    }

    open fun getRecyclerView(): RecyclerView = view as RecyclerView

    private fun rebind() {
        isNestedScrollingEnabled(true)
        if (rv.adapter == null) {
            bind(rv, viewModel)
        }
    }

    private fun unbind() {
        isNestedScrollingEnabled(false)
        rv.adapter = null
    }

    private fun isNestedScrollingEnabled(enabled: Boolean) {
        rv.isNestedScrollingEnabled = enabled
    }

    final override fun bind(viewModel: VM) = Unit

    protected abstract fun bind(recyclerView: RecyclerView, viewModel: VM)

    companion object {
        fun <VM : ViewModel, F : PageFragment<VM>> page(
            parent: ClipDetailsFragment,
            tab: ClipDetailsTab,
            parentView: View,
            fragmentInstance: () -> F
        ): ViewPagerState.FragmentPageProvider<F> {
            return object : ViewPagerState.FragmentPageProvider<F>() {
                override fun getTitle(context: Context): CharSequence = tab.getLabel(context)
                override fun getPage(context: Context): F = fragmentInstance.invoke()
                override fun selectPage(page: F) {
                    parent.viewModel.onSelectTab(tab)
                    page.rebind()
                    parentView.requestLayout()
                }

                override fun unselectPage(page: F) {
                    page.unbind()
                }
            }
        }
    }

}