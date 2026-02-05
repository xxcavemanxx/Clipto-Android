package clipto.presentation.clip.details.pages.general

import androidx.fragment.app.viewModels
import clipto.common.presentation.mvvm.MvvmDialogFragment
import clipto.presentation.clip.details.pages.general.GeneralPageViewModel

abstract class PublicLinkEditDialogFragment : MvvmDialogFragment<GeneralPageViewModel>() {

    override var withSizeLimits: SizeLimits? = SizeLimits(widthMultiplier = 0.85f)
    override val viewModel: GeneralPageViewModel by viewModels()
    override var withNoTitle: Boolean = true

}