package clipto.presentation.blocks.domain

import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import clipto.common.extensions.load
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.setVisibleOrGone
import clipto.presentation.common.recyclerview.BlockItem
import clipto.store.user.UserState
import com.wb.clipboard.R
import kotlinx.android.synthetic.main.block_account.view.*

class AccountBlock<C : Fragment>(
    private val userState: UserState,
    private val onSignIn: (webAuth: Boolean, withWarning: Boolean, callback: () -> Unit) -> Unit
) : BlockItem<C>() {

    override val layoutRes: Int = R.layout.block_account

    override fun onBind(context: C, block: View) {
        userState.user.getLiveData().observe(context) { user ->
            if (user.isAuthorized()) {
                user.photoUrl?.let { block.icon.load(it) }
                block.titleTextView.text = user.getTitle()
                    ?: context.getString(R.string.account_title_authorized)
                block.actionButton.setVisibleOrGone(false)
                block.actionIcon.setVisibleOrGone(true)
            } else {
                block.icon.setImageDrawable(null)
                block.titleTextView.text = context.getString(R.string.account_title_not_authorized)
                block.actionButton.setVisibleOrGone(true)
                block.actionIcon.setVisibleOrGone(false)
            }
        }

        userState.syncLimit.getLiveData().observe(context) { title ->
            block.descriptionTextView.text = title
        }

        block.accountPanel.setDebounceClickListener {
            if (userState.isAuthorized()) {
                context.findNavController().navigate(R.id.action_account)
            } else {
                onSignIn.invoke(false, true) {}
            }
        }

        block.actionButton.setOnLongClickListener {
            onSignIn.invoke(true, true) {}
            true
        }

        block.actionButton.setDebounceClickListener {
            onSignIn.invoke(false, true) {}
        }
    }
}