package clipto.presentation.clip.details.pages.general
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentClipPublicLinkEditAccessPasswordBinding
import clipto.common.extensions.animateScale
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PublicLinkEditPasswordDialogFragment : PublicLinkEditDialogFragment() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentClipPublicLinkEditAccessPasswordBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentClipPublicLinkEditAccessPasswordBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_clip_public_link_edit_access_password

    override fun bind(viewModel: GeneralPageViewModel) {
        // password
        binding.passwordClueView.setText(viewModel.getPublicLink().passwordClue)

        // cancel action
        binding.cancelAction.setOnClickListener { dismissAllowingStateLoss() }

        // apply action
        binding.applyAction.setOnClickListener {
            val password = binding.passwordView.text.toString()
            val passwordClue = binding.passwordClueView.text.toString()
            viewModel.onPublicLinPasswordChanged(password, passwordClue) {
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
