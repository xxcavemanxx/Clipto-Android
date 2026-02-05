package clipto.presentation.settings.doubleclickactions

import androidx.fragment.app.viewModels
import clipto.analytics.Analytics
import clipto.common.presentation.mvvm.MvvmFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_settings_double_click_actions.*

@AndroidEntryPoint
class DoubleClickActionsFragment : MvvmFragment<DoubleClickActionsViewModel>() {

    override val layoutResId: Int = R.layout.fragment_settings_double_click_actions
    override val viewModel: DoubleClickActionsViewModel by viewModels()

    override fun bind(viewModel: DoubleClickActionsViewModel) {
        withDefaults(toolbar, R.string.settings_double_click_title)

        val settings = viewModel.settings

        exitTheAppSwitch.isChecked = settings.doubleClickToExit
        exitTheAppAction.setOnClickListener { exitTheAppSwitch.isChecked = !exitTheAppSwitch.isChecked }
        exitTheAppSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleClickToExit = isChecked
        }

        deleteNoteSwitch.isChecked = settings.doubleClickToDelete
        deleteNoteAction.setOnClickListener { deleteNoteSwitch.isChecked = !deleteNoteSwitch.isChecked }
        deleteNoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleClickToDelete = isChecked
        }

        editNoteSwitch.isChecked = settings.doubleClickToEdit
        editNoteAction.setOnClickListener { editNoteSwitch.isChecked = !editNoteSwitch.isChecked }
        editNoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleClickToEdit = isChecked
        }

        Analytics.screenDoubleClickActions()
    }

}
