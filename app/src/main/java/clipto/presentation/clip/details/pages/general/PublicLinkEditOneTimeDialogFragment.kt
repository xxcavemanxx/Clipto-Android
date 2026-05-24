package clipto.presentation.clip.details.pages.general
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentClipPublicLinkEditOneTimeBinding
import clipto.common.extensions.animateScale
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PublicLinkEditOneTimeDialogFragment : PublicLinkEditDialogFragment() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClipPublicLinkEditOneTimeBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentClipPublicLinkEditOneTimeBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_clip_public_link_edit_one_time

    override fun bind(viewModel: GeneralPageViewModel) {
        // one time
        binding.oneTimeToggle.isChecked = viewModel.getPublicLink().isOneTime() == true

        // cancel action
        binding.cancelAction.setOnClickListener { dismissAllowingStateLoss() }

        // apply action
        binding.applyAction.setOnClickListener {
            viewModel.onPublicLinkOneTimeChanged(binding.oneTimeToggle.isChecked) {
                dismissAllowingStateLoss()
            }
        }

        binding.iconView.animateScale(true)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
