package clipto.presentation.common.dialog.hint

import com.wb.clipboard.databinding.DialogHintBinding
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import clipto.AppContext
import clipto.common.extensions.animateScale
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.trimSpaces
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.base.BaseDialogFragment
import clipto.extensions.TextTypeExt
import clipto.extensions.getTextColorPrimary
import com.wb.clipboard.R

class HintDialogFragment : BaseDialogFragment() {

    
    private var _binding: DialogHintBinding? = null
    private val binding get() = _binding!!
override var withNoTitle: Boolean = true
    override val layoutResId: Int = R.layout.dialog_hint
    override var withSizeLimits: SizeLimits? = SizeLimits(widthMultiplier = 0.85f, onSizeChanged = { binding.scrollView?.requestLayout() })

    private val dialogData by lazy { arguments?.getSerializable(ATTR_DATA) as HintDialogData? }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = DialogHintBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        val data = dialogData
        if (data == null) {
            dismissAllowingStateLoss()
            return
        }
        binding.titleView.text = data.title
        if (data.withDefaultIconColor) {
            binding.iconView.imageTintList = ColorStateList.valueOf(requireContext().getTextColorPrimary())
        }
        binding.iconView.setImageResource(data.iconRes)
        if (data.descriptionIsMarkdown) {
            TextTypeExt.MARKDOWN.apply(binding.descriptionView, data.description, skipDynamicFieldsRendering = true)
        } else {
            binding.descriptionView.text = data.description.trimSpaces()
        }
        binding.descriptionView.setDebounceClickListener { AppContext.get().onCopy(data.description) }
        binding.okAction.setOnClickListener { dismissAllowingStateLoss() }
        binding.okAction.setText(data.actionRes)
        binding.iconView.animateScale(true)
    }

    companion object {

        private const val ATTR_DATA = "attr_data"

        fun show(
            activity: FragmentActivity,
            data: HintDialogData
        ) {
            activity.withSafeFragmentManager()?.let { fm ->
                HintDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putSerializable(ATTR_DATA, data)
                        }
                    }
                    .show(fm, "HintDialogFragment")
            }
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
