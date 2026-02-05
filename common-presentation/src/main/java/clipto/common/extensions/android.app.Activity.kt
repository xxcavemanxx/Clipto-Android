package clipto.common.extensions

import android.app.Activity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import clipto.common.R
import java.util.concurrent.TimeUnit

private val backPressTimeout = TimeUnit.SECONDS.toMillis(2L)
private var lastBackPress: Long = 0

fun Activity.onBackPressDeclined(drawerId: Int = 0): Boolean {
    if (!isTaskRoot || isChild) {
        return false
    }
    if (drawerId != 0) {
        val drawer: DrawerLayout = findViewById(drawerId)
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
            return true
        } else if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END)
            return true
        }
    }
    val current = System.currentTimeMillis()
    return if (current - lastBackPress < backPressTimeout) {
        false
    } else {
        lastBackPress = current
        showToast(R.string.error_confirm_exit)
        true
    }
}