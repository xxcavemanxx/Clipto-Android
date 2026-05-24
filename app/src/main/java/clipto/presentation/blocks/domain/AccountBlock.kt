package clipto.presentation.blocks.domain

import com.wb.clipboard.databinding.BlockAccountBinding
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import clipto.common.extensions.load
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.user.UserState
import com.wb.clipboard.R

class AccountBlock<C : Fragment>(
    private val userState: UserState,
    private val onSignIn: (webAuth: Boolean, withWarning: Boolean, callback: () -> Unit) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_account

    override fun onBind(context: C, block: View) {
        val binding = BlockAccountBinding.bind(block)
        userState.user.getLiveData().observe(context) { user ->
            if (user.isAuthorized()) {
                user.photoUrl?.let { binding.icon.load(it) }
                binding.titleTextView.text = user.getTitle()
                    ?: context.getString(R.string.account_title_authorized)
                binding.actionButton.setVisibleOrGone(false)
                binding.actionIcon.setVisibleOrGone(true)
            } else {
                binding.icon.setImageDrawable(null)
                binding.titleTextView.text = context.getString(R.string.account_title_not_authorized)
                binding.actionButton.setVisibleOrGone(true)
                binding.actionIcon.setVisibleOrGone(false)
            }
        }

        userState.syncLimit.getLiveData().observe(context) { title ->
            binding.descriptionTextView.text = title
        }

        binding.accountPanel.setDebounceClickListener {
            if (userState.isAuthorized()) {
                context.findNavController().navigate(R.id.action_account)
            } else {
                onSignIn.invoke(false, true) {}
            }
        }

        binding.actionButton.setOnLongClickListener {
            onSignIn.invoke(true, true) {}
            true
        }

        binding.actionButton.setDebounceClickListener {
            onSignIn.invoke(false, true) {}
        }
    }
}
