package clipto.presentation.lockscreen.fingerprint

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
import kotlinx.android.synthetic.main.fragment_set_touch_id.*

@AndroidEntryPoint
class SetFingerprintFragment : MvvmFragment<SetFingerprintViewModel>() {

    override val layoutResId: Int = R.layout.fragment_set_touch_id
    override val viewModel: SetFingerprintViewModel by viewModels()

    override fun bind(viewModel: SetFingerprintViewModel) {
        withDefaults(toolbar)
        toolbar.setNavigationIcon(R.drawable.action_arrow_back)
        ivStatus.animateScale(true)
        clParent.doOnFirstLayout { animateButtons() }
        btnSkipForNow.setDebounceClickListener { viewModel.onSkipForNowClick() }
        btnUseFingerprint.setDebounceClickListener { viewModel.onUseFingerprintClick() }
        viewModel.doneLive.observe(viewLifecycleOwner) { navigateUp() }
    }

    private fun animateButtons() {
        val buttonsSet = TransitionSet()
                .addTransition(Slide(Gravity.BOTTOM).addTarget(btnUseFingerprint)
                        .setInterpolator(DecelerateInterpolator()).setDuration(500))
                .addTransition(Slide(Gravity.BOTTOM).addTarget(btnSkipForNow)
                        .setInterpolator(DecelerateInterpolator()).setDuration(500).setStartDelay(500))
        TransitionManager.beginDelayedTransition(clParent, buttonsSet)
        btnUseFingerprint.visible()
        btnSkipForNow.visible()
    }

}
