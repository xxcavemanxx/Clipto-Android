package clipto.presentation.contextactions

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import clipto.common.extensions.closeSystemDialogs
import clipto.common.extensions.withSafeChildFragmentManager
import clipto.common.presentation.mvvm.MvvmActivity
import clipto.common.presentation.mvvm.base.AppFragment
import clipto.extensions.onCreateWithTheme
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode

@AndroidEntryPoint
class ContextActionsActivity : MvvmActivity<ContextActionsViewModel>() {

    override val layoutResId: Int = R.layout.activity_empty
    override val viewModel: ContextActionsViewModel by viewModels()

    private val scanBarcode = registerForActivityResult(ScanCustomCode(), ::onBarcodeResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateWithTheme { it.translucentThemeId }
        super.onCreate(savedInstanceState)
    }

    override fun bind(viewModel: ContextActionsViewModel) {
        viewModel.folderState.bind(this)
        viewModel.dialogState.bind(this)
        viewModel.dialogState.bind(this, scanBarcode)
        viewModel.dynamicFieldState.bind(this)
        viewModel.filterDetailsState.bind(this)
        viewModel.intentLive.observe(this) {
            it?.let { lastIntent ->
                if (canIgnoreFocusOverlay(lastIntent)) {
                    viewModel.onHandleIntent(this, lastIntent) {
                        viewModel.onMain { onComplete() }
                    }
                } else {
                    ContextActionsFragment.process(this) {
                        viewModel.onHandleIntent(this, lastIntent) {
                            viewModel.onMain { onComplete() }
                        }
                    }
                }
            }
        }
        viewModel.finishRequestLive.observe(this) {
            onComplete()
        }
        viewModel.onIntent(intent)

        closeSystemDialogs()
    }

    override fun onNewIntent(intent: Intent?) {
        viewModel.onIntent(intent)
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (supportFragmentManager.fragments.any { it is ContextActionsFragment && it.isForced }) {
            onComplete()
        }
    }

    override fun onBackPressed() {
        onComplete()
    }

    internal fun onComplete() {
        val allFragments = supportFragmentManager.fragments
        val fragments = allFragments.filter { it is AppFragment && it.withSafeChildFragmentManager() != null }
        fragments.filterIsInstance<ContextActionsFragment>().forEach {
            it.onClose()
        }
        if (fragments.none { it !is ContextActionsFragment }) {
            finishAndRemoveTask()
        }
    }

    private fun onBarcodeResult(result: QRResult) {
        viewModel.dialogState.onBarcodeResult(result)
    }

    companion object {
        private const val ATTR_IGNORE_FOCUS_OVERLAY = "ATTR_IGNORE_FOCUS_OVERLAY"

        fun canIgnoreFocusOverlay(intent: Intent): Boolean = intent.hasExtra(ATTR_IGNORE_FOCUS_OVERLAY)

        fun withIgnoreFocusOverlay(intent: Intent): Intent {
            intent.putExtra(ATTR_IGNORE_FOCUS_OVERLAY, true)
            return intent
        }
    }
}