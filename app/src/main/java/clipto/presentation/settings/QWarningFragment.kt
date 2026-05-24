package clipto.presentation.settings

import com.wb.clipboard.databinding.FragmentSettingsQWarningBinding
import android.os.Bundle
import android.view.View
import clipto.AppContext
import clipto.analytics.Analytics
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.trimSpaces
import clipto.common.misc.IntentUtils
import clipto.common.presentation.mvvm.base.BaseFragment
import clipto.domain.Clip
import clipto.domain.TextType
import com.wb.clipboard.R
import clipto.extensions.from

class QWarningFragment : BaseFragment() {

    
    private var _binding: FragmentSettingsQWarningBinding? = null
    private val binding get() = _binding!!
override val layoutResId: Int = R.layout.fragment_settings_q_warning

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsQWarningBinding.bind(view)
        withDefaults(binding.toolbar)

        val appContext = AppContext.get()

        // GLOBAL
        binding.cliptoActionLearnMore?.setDebounceClickListener { IntentUtils.open(appContext.app, appContext.appConfig.getGlobalCopyInstructionUrl()) }

        // NOTIFICATION
        binding.pasteLearnMore?.setDebounceClickListener { IntentUtils.open(appContext.app, appContext.appConfig.getNotificationPasteInstructionUrl()) }

        // ADB
        binding.adbLearnMore?.setDebounceClickListener { IntentUtils.open(appContext.app, appContext.appConfig.getAdbInstructionUrl()) }
        val command = appContext.string(R.string.settings_track_clipboard_q_solution_3_command, requireContext().packageName).toString().trimSpaces().toString()
        binding.command3.setOnClickListener {
            appContext.onCopy(
                    Clip.from(command, tracked = true).apply { textType = TextType.LINE_CLICKABLE },
                    clearSelection = false,
                    saveCopied = true
            )
        }
        binding.command3.text = command

        Analytics.screenQWarning()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
