package clipto.presentation.contextactions

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import clipto.common.extensions.setBottomSheetHeight
import clipto.common.extensions.withSafeFragmentManager
import clipto.common.misc.AndroidUtils
import clipto.common.presentation.mvvm.MvvmBottomSheetDialogFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_context_actions.*
import java.util.*

@AndroidEntryPoint
class ContextActionsFragment : MvvmBottomSheetDialogFragment<ContextActionsViewModel>() {

    override val viewModel: ContextActionsViewModel by activityViewModels()
    override val layoutResId: Int = R.layout.fragment_context_actions

    val isForced:Boolean by lazy { arguments?.getBoolean(ATTR_FORCED, false) ?: false }

    override fun bind(viewModel: ContextActionsViewModel) {
        val id = arguments?.get(ATTR_ID)?.toString()
        val action = viewModel.actions.remove(id)
        if (action != null) {
            contentView.setBottomSheetHeight(0.001f) { _, _, _ ->
                action.invoke(this)
            }
        } else {
            onClose()
        }
    }

    fun getTextViewRef(): TextView? = textView

    fun onClose() {
        runCatching { dismissAllowingStateLoss() }
    }

    companion object {
        private const val TAG = "ContextActionsFragment"
        private const val ATTR_FORCED = "__action_forced__"
        private const val ATTR_ID = "__action_id__"

        fun process(activity: FragmentActivity, force: Boolean = false, action: (fragment: ContextActionsFragment?) -> Unit) {
            if (AndroidUtils.isPreQ() && !force) {
                action.invoke(null)
            } else {
                activity.withSafeFragmentManager()?.let { fm ->
                    val viewModel = activity.viewModels<ContextActionsViewModel>().value
                    val id = UUID.randomUUID().toString()
                    viewModel.actions[id] = action
                    ContextActionsFragment()
                        .apply {
                            arguments = Bundle().apply {
                                putBoolean(ATTR_FORCED, force)
                                putString(ATTR_ID, id)
                            }
                        }
                        .show(fm, TAG)
                }
            }
        }
    }
}