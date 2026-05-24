package clipto.presentation.lockscreen.fingerprint
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSetTouchIdBinding
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.viewModels
import androidx.transition.Slide
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import clipto.common.extensions.animateScale
import clipto.common.extensions.doOnFirstLayout
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.visible
import clipto.common.presentation.mvvm.MvvmFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetFingerprintFragment : MvvmFragment<SetFingerprintViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSetTouchIdBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSetTouchIdBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_set_touch_id
    override val viewModel: SetFingerprintViewModel by viewModels()

    override fun bind(viewModel: SetFingerprintViewModel) {
        withDefaults(binding.toolbar)
        binding.toolbar.setNavigationIcon(R.drawable.action_arrow_back)
        binding.ivStatus.animateScale(true)
        binding.clParent.doOnFirstLayout { animateButtons() }
        binding.btnSkipForNow.setDebounceClickListener { viewModel.onSkipForNowClick() }
        binding.btnUseFingerprint.setDebounceClickListener { viewModel.onUseFingerprintClick() }
        viewModel.doneLive.observe(viewLifecycleOwner) { navigateUp() }
    }

    private fun animateButtons() {
        val buttonsSet = TransitionSet()
                .addTransition(Slide(Gravity.BOTTOM).addTarget(binding.btnUseFingerprint)
                        .setInterpolator(DecelerateInterpolator()).setDuration(500))
                .addTransition(Slide(Gravity.BOTTOM).addTarget(binding.btnSkipForNow)
                        .setInterpolator(DecelerateInterpolator()).setDuration(500).setStartDelay(500))
        TransitionManager.beginDelayedTransition(binding.clParent, buttonsSet)
        binding.btnUseFingerprint.visible()
        binding.btnSkipForNow.visible()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
