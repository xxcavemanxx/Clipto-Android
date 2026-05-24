package clipto.presentation.common.dialog.confirm

import com.wb.clipboard.databinding.DialogConfirmBinding
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import clipto.common.extensions.animateScale
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.presentation.mvvm.base.BaseDialogFragment
import clipto.extensions.TextTypeExt
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ConfirmDialogFragment : BaseDialogFragment() {

    
    private var _binding: DialogConfirmBinding? = null
    private val binding get() = _binding!!
val viewModel: ConfirmDialogViewModel by activityViewModels()
    private val confirmData: ConfirmDialogData? by lazy {
        val id = arguments?.getInt(ATTR_DATA_ID) ?: 0
        val data = viewModel.dataMap.get(id)
        viewModel.dataMap.remove(id)
        data
    }

    override var withSizeLimits: SizeLimits? = SizeLimits(widthMultiplier = 0.85f, onSizeChanged = { binding.scrollView?.requestLayout() })
    override val layoutResId: Int = R.layout.dialog_confirm
    override var withNoTitle: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = DialogConfirmBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        val data = confirmData
        if (data == null) {
            dismissAllowingStateLoss()
            return
        }
        binding.titleView.text = data.title
        binding.iconView.setImageResource(data.iconRes)
        if (data.descriptionIsMarkdown) {
            TextTypeExt.MARKDOWN.apply(binding.descriptionView, data.description, skipDynamicFieldsRendering = true)
        } else {
            binding.descriptionView.text = data.description
        }
        binding.okAction.setText(data.confirmActionTextRes)
        binding.okAction.setOnClickListener {
            data.proceeded = true
            dismissAllowingStateLoss()
            data.onConfirmed.invoke()
        }
        binding.cancelAction.setText(data.cancelActionTextRes)
        binding.cancelAction?.setOnClickListener {
            data.proceeded = true
            dismissAllowingStateLoss()
            data.onCanceled.invoke()
        }
        binding.iconView.animateScale(true)
    }

    override fun onDestroy() {
        confirmData?.let { data ->
            data.onClosed.invoke(data.proceeded)
        }
        super.onDestroy()
    }

    companion object {

        private const val ATTR_DATA_ID = "attr_data_id"

        fun show(activity: FragmentActivity, data: ConfirmDialogData) {
            activity.withSafeFragmentManager()?.let { fm ->
                if (data.autoConfirm.invoke()) {
                    data.onConfirmed.invoke()
                    return
                }
                val viewModel = activity.viewModels<ConfirmDialogViewModel>().value
                viewModel.dataMap.put(data.id, data)
                ConfirmDialogFragment()
                    .apply {
                        arguments = Bundle().apply {
                            putInt(ATTR_DATA_ID, data.id)
                        }
                    }
                    .show(fm, "ConfirmDialogFragment")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
