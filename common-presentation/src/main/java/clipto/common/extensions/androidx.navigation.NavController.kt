package clipto.common.extensions

import android.os.Bundle
import androidx.navigation.NavController

fun NavController.navigateSafe(destination: Int, args:Bundle? = null) {
    runCatching { navigate(destination, args) }
}