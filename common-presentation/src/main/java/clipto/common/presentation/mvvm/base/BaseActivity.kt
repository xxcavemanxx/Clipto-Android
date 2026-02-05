package clipto.common.presentation.mvvm.base

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    protected abstract val layoutResId: Int

    override fun onStart() {
        try {
            super.onStart()
        } catch (e: Exception) {
            //
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutResId)
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
        } catch (e: IllegalStateException) {
            //
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return try {
            super.dispatchKeyEvent(event)
        } catch (e: Exception) {
            false
        }
    }

    open fun onResumeManually() {}

    companion object {
        const val FULL_SCREEN_FLAGS = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )

        const val DEFAULT_SCREEN_FLAGS = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

}

fun Activity.hideSystemUI() {
    window?.decorView?.systemUiVisibility = BaseActivity.FULL_SCREEN_FLAGS
}

fun Activity.showSystemUI() {
    window.decorView.systemUiVisibility = BaseActivity.DEFAULT_SCREEN_FLAGS
    window?.apply {
        addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
}

fun Activity.setBackgroundColor(color: Int) {
    window?.decorView?.setBackgroundColor(color)
}

fun View.hideSystemUI() {
    systemUiVisibility = BaseActivity.FULL_SCREEN_FLAGS
    requestLayout()
}

fun View.showSystemUI() {
    systemUiVisibility = BaseActivity.DEFAULT_SCREEN_FLAGS
    requestLayout()
}