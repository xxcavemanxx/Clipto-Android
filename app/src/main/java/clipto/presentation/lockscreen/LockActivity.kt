package clipto.presentation.lockscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import clipto.common.presentation.mvvm.base.BaseActivity
import clipto.common.presentation.mvvm.base.FragmentBackButtonListener
import clipto.extensions.onCreateWithTheme
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LockActivity : BaseActivity(), UnlockListener {

    override val layoutResId: Int = R.layout.activity_lock

    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateWithTheme()
        super.onCreate(savedInstanceState)
    }

    override fun onUnlocked() {
        finish()
    }

    override fun onUnauthorized() {
        finish()
    }

    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.pinLock) as? FragmentBackButtonListener
        fragment?.run { onFragmentBackPressed() } ?: super.onBackPressed()
    }

    companion object {
        fun start(appContext: Context) {
            val intent = Intent(appContext, LockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
        }
    }
}