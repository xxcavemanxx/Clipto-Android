package clipto.presentation.snippets.details

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.common.extensions.gone
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.visible
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.StatefulFragment
import clipto.domain.SnippetKit
import clipto.extensions.getTextColorSecondary
import clipto.extensions.getTitleRes
import clipto.extensions.getUserNameLabel
import clipto.presentation.common.recyclerview.BlockListAdapter
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_snippet_kit_details.*

@AndroidEntryPoint
class SnippetKitDetailsFragment : MvvmFragment<SnippetKitDetailsViewModel>(), StatefulFragment {

    override val layoutResId: Int = R.layout.fragment_snippet_kit_details

    override val viewModel: SnippetKitDetailsViewModel by viewModels()

    override fun bind(viewModel: SnippetKitDetailsViewModel) {
        val ctx = requireContext()

        ivBack.setDebounceClickListener { navigateUp() }
        ivShare.setDebounceClickListener { viewModel.onShare() }

        val adapter = BlockListAdapter(this)
        rvBlocks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rvBlocks.adapter = adapter

        viewModel.kitLive.observe(viewLifecycleOwner) { kit ->
            if (kit != null) {
                tvName?.text = kit.name
                tvNameSingleLine?.text = kit.name
                tvAuthor?.text = kit.getUserNameLabel()
                tvStatus?.setText(kit.publicStatus.getTitleRes())

                val iconColor = kit.color?.let { Color.parseColor(it) } ?: ctx.getTextColorSecondary()
                ivIcon.imageTintList = ColorStateList.valueOf(iconColor)
                ivIcon.refreshDrawableState()

                val bgColor = ColorUtils.setAlphaComponent(iconColor, 20)
                ivIconBg.imageTintList = ColorStateList.valueOf(bgColor)
                ivIconBg.refreshDrawableState()

                if (kit === SnippetKit.NOT_FOUND) {
                    ivShare?.setImageResource(R.drawable.ic_bug_report)
                } else {
                    ivShare?.setImageResource(R.drawable.ic_share)
                }

                clLoading?.gone()
            } else {
                clLoading?.visible()
            }
        }

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

}