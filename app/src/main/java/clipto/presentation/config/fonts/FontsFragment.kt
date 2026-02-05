package clipto.presentation.config.fonts

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import clipto.analytics.Analytics
import clipto.common.extensions.hideKeyboard
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import clipto.domain.Font
import clipto.domain.FontLanguage
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_fonts.*
import java.util.*

@AndroidEntryPoint
class FontsFragment : MvvmBottomSheetDialogFragment<FontsViewModel>() {

    override val layoutResId: Int = R.layout.fragment_fonts
    override val viewModel: FontsViewModel by viewModels()

    override fun bind(viewModel: FontsViewModel) {
        contentView.setBottomSheetHeight(noBackground = true)

        val context = requireContext()

        var language: FontLanguage? = null

        val fonts = Font.getMoreFonts()
        val adapter = FontsAdapter(context, fonts, viewModel) {
            if (viewModel.getListConfig().textFont != it.id) {
                viewModel.onApplyConfig { cfg -> cfg.copy(textFont = it.id) }
            } else {
                var indexOfNextLanguage = it.languages.indexOfFirst { it == language } + 1
                if (indexOfNextLanguage >= it.languages.size) {
                    indexOfNextLanguage = 0
                }
                it.languages.getOrNull(indexOfNextLanguage)?.let {
                    exampleEditTextView?.setText(it.exampleRes)
                    language = it
                }
            }
        }

        var prevFont: Font? = null
        viewModel.settingsLive.observe(viewLifecycleOwner) {
            val next = Font.valueOf(it)
            val prev = prevFont
            exampleTextView?.hint = viewModel.string(next.titleRes)
            exampleEditTextView?.let { textView ->
                val lang = language
                if (prev == null ||
                    lang == null ||
                    !next.languages.contains(lang) ||
                    (prev.languages.first() != next.languages.first()
                            && textView.text?.toString() == viewModel.string(prev.languages.first().exampleRes))
                ) {
                    language = next.languages.first()
                    language?.let { textView.setText(it.exampleRes) }
                }
                next.apply(textView)
            }
            prevFont = next
            adapter.notifyDataSetChanged()
        }

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                dragged: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val draggedPosition = dragged.adapterPosition
                val targetPosition = target.adapterPosition
                fonts[draggedPosition].order = targetPosition
                fonts[targetPosition].order = draggedPosition
                Collections.swap(fonts, draggedPosition, targetPosition)
                adapter.notifyItemMoved(draggedPosition, targetPosition)
                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                    if (viewHolder is FontsAdapter.ViewHolder) {
                        viewHolder.onItemSelected()
                    }
                }
                super.onSelectedChanged(viewHolder, actionState)
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (viewHolder is FontsAdapter.ViewHolder) {
                    viewHolder.onItemClear()
                }
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int = 0
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })

        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter
        touchHelper.attachToRecyclerView(recyclerView)

        Analytics.screenFonts()
    }

    override fun onStop() {
        exampleTextView?.hideKeyboard()
        super.onStop()
    }

    companion object {
        fun show(fragment: Fragment) {
            fragment.withSafeFragmentManager()?.let { fm ->
                FontsFragment().show(fm, "FontsFragment")
            }
        }
    }

}
