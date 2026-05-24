package clipto.presentation.settings.doubleclickactions
import android.view.View
import android.os.Bundle

import com.wb.clipboard.databinding.FragmentSettingsDoubleClickActionsBinding
import androidx.fragment.app.viewModels
import clipto.analytics.Analytics
import clipto.common.presentation.mvvm.MvvmFragment
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DoubleClickActionsFragment : MvvmFragment<DoubleClickActionsViewModel>() {

    
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsDoubleClickActionsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
    }
private var _binding: FragmentSettingsDoubleClickActionsBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_settings_double_click_actions
    override val viewModel: DoubleClickActionsViewModel by viewModels()

    override fun bind(viewModel: DoubleClickActionsViewModel) {
        withDefaults(binding.toolbar, R.string.settings_double_click_title)

        val settings = viewModel.settings

        binding.exitTheAppSwitch.isChecked = settings.doubleClickToExit
        binding.exitTheAppAction.setOnClickListener { binding.exitTheAppSwitch.isChecked = !binding.exitTheAppSwitch.isChecked }
        binding.exitTheAppSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleClickToExit = isChecked
        }

        binding.deleteNoteSwitch.isChecked = settings.doubleClickToDelete
        binding.deleteNoteAction.setOnClickListener { binding.deleteNoteSwitch.isChecked = !binding.deleteNoteSwitch.isChecked }
        binding.deleteNoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleClickToDelete = isChecked
        }

        binding.editNoteSwitch.isChecked = settings.doubleClickToEdit
        binding.editNoteAction.setOnClickListener { binding.editNoteSwitch.isChecked = !binding.editNoteSwitch.isChecked }
        binding.editNoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.doubleClickToEdit = isChecked
        }

        Analytics.screenDoubleClickActions()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
