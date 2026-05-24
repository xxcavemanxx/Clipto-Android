package clipto.presentation.snippets.details
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSnippetKitDetailsBinding
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

@AndroidEntryPoint
class SnippetKitDetailsFragment : MvvmFragment<SnippetKitDetailsViewModel>(), StatefulFragment {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSnippetKitDetailsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSnippetKitDetailsBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_snippet_kit_details

    override val viewModel: SnippetKitDetailsViewModel by viewModels()

    override fun bind(viewModel: SnippetKitDetailsViewModel) {
        val ctx = requireContext()

        binding.ivBack.setDebounceClickListener { navigateUp() }
        binding.ivShare.setDebounceClickListener { viewModel.onShare() }

        val adapter = BlockListAdapter(this)
        binding.rvBlocks.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvBlocks.adapter = adapter

        viewModel.kitLive.observe(viewLifecycleOwner) { kit ->
            if (kit != null) {
                binding.tvName?.text = kit.name
                binding.tvNameSingleLine?.text = kit.name
                binding.tvAuthor?.text = kit.getUserNameLabel()
                binding.tvStatus?.setText(kit.publicStatus.getTitleRes())

                val iconColor = kit.color?.let { Color.parseColor(it) } ?: ctx.getTextColorSecondary()
                binding.ivIcon.imageTintList = ColorStateList.valueOf(iconColor)
                binding.ivIcon.refreshDrawableState()

                val bgColor = ColorUtils.setAlphaComponent(iconColor, 20)
                binding.ivIconBg.imageTintList = ColorStateList.valueOf(bgColor)
                binding.ivIconBg.refreshDrawableState()

                if (kit === SnippetKit.NOT_FOUND) {
                    binding.ivShare?.setImageResource(R.drawable.ic_bug_report)
                } else {
                    binding.ivShare?.setImageResource(R.drawable.ic_share)
                }

                binding.clLoading?.gone()
            } else {
                binding.clLoading?.visible()
            }
        }

        viewModel.blocksLive.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
