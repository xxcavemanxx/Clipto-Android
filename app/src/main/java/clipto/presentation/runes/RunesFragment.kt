package clipto.presentation.runes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.provider.FontRequest
import androidx.emoji.text.EmojiCompat
import androidx.emoji.text.FontRequestEmojiCompatConfig
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import clipto.analytics.Analytics
import clipto.common.extensions.navigateTo
import clipto.common.extensions.setDebounceClickListener
import clipto.common.extensions.toEmoji
import clipto.common.presentation.mvvm.MvvmFragment
import clipto.common.presentation.mvvm.base.StatefulFragment
import clipto.extensions.log
import clipto.presentation.common.recyclerview.FlowLayoutManagerExt
import com.wb.clipboard.R
import com.xiaofeng.flowlayoutmanager.Alignment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_runes.*
import kotlinx.android.synthetic.main.fragment_settings.recyclerView


@AndroidEntryPoint
class RunesFragment : MvvmFragment<RunesViewModel>(), StatefulFragment {

    override val layoutResId: Int = R.layout.fragment_runes
    override val viewModel: RunesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initEmojiCompat()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun initEmojiCompat() {
        val fontRequest = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            "Noto Color Emoji Compat",
            R.array.com_google_android_gms_fonts_certs
        )
        val config: EmojiCompat.Config = FontRequestEmojiCompatConfig(requireContext().applicationContext, fontRequest)
        config.setReplaceAll(true)
            .registerInitCallback(object : EmojiCompat.InitCallback() {
                override fun onInitialized() = log("EmojiCompat initialized")
                override fun onFailed(throwable: Throwable?) = log("EmojiCompat initialization failed", throwable)
            })
        EmojiCompat.init(config)
    }

    override fun bind(viewModel: RunesViewModel) {
        val ctx = requireContext()

        tvLanguage.setDebounceClickListener { viewModel.onChangeLanguage() }
        ivMode.setDebounceClickListener { viewModel.onChangeMode() }
        tvTitle.setDebounceClickListener { viewModel.onShowHint() }
        ivBack.setDebounceClickListener { navigateUp() }

        viewModel.languageLive.observe(viewLifecycleOwner) {
            tvLanguage?.text = it.toEmoji()
        }

        viewModel.runesLive.observe(viewLifecycleOwner) {
            if (viewModel.isFlatMode()) return@observe
            ivMode?.setImageResource(R.drawable.settings_mode_grid)
            var adapter = recyclerView?.adapter
            if (adapter !is RunesAdapter) {
                adapter = RunesAdapter(ctx) { viewModel.onSelectRune(it) }
                recyclerView?.layoutManager = FlowLayoutManagerExt().also { it.setAlignment(Alignment.CENTER) }
                recyclerView?.adapter = adapter
            }
            adapter.submitList(it)
        }

        viewModel.runesFlatLive.observe(viewLifecycleOwner) {
            if (!viewModel.isFlatMode()) return@observe
            ivMode?.setImageResource(R.drawable.settings_mode_flat)
            var adapter = recyclerView?.adapter
            if (adapter !is RunesFlatAdapter) {
                adapter = RunesFlatAdapter(this)
                recyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                recyclerView?.adapter = adapter
            }
            val adapterRef = adapter as RunesFlatAdapter
            adapterRef.submitList(it)
        }

        viewModel.runeLive.observe(viewLifecycleOwner) {
            navigateTo(R.id.action_rune_settings, RuneSettingsViewModel.withArgs(it.getId()))
        }

        viewModel.settingsLive.observe(viewLifecycleOwner) {
            viewModel.onRefresh()
        }

        viewModel.syncLimitLive.observe(viewLifecycleOwner) {
            viewModel.onRefresh()
        }

        Analytics.screenRunes()
    }

}
