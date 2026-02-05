package clipto.presentation.common.dialog.hint

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
import kotlinx.android.synthetic.main.dialog_hint.*

class HintDialogFragment : BaseDialogFragment() {

    override var withNoTitle: Boolean = true
    override val layoutResId: Int = R.layout.dialog_hint
    override var withSizeLimits: SizeLimits? = SizeLimits(widthMultiplier = 0.85f, onSizeChanged = { scrollView?.requestLayout() })

    private val dialogData by lazy { arguments?.getSerializable(ATTR_DATA) as HintDialogData? }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val data = dialogData
        if (data == null) {
            dismissAllowingStateLoss()
            return
        }
        titleView.text = data.title
        if (data.withDefaultIconColor) {
            iconView.imageTintList = ColorStateList.valueOf(requireContext().getTextColorPrimary())
        }
        iconView.setImageResource(data.iconRes)
        if (data.descriptionIsMarkdown) {
            TextTypeExt.MARKDOWN.apply(descriptionView, data.description, skipDynamicFieldsRendering = true)
        } else {
            descriptionView.text = data.description.trimSpaces()
        }
        descriptionView.setDebounceClickListener { AppContext.get().onCopy(data.description) }
        okAction.setOnClickListener { dismissAllowingStateLoss() }
        okAction.setText(data.actionRes)
        iconView.animateScale(true)
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

}