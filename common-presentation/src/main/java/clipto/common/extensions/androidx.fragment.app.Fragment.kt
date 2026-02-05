package clipto.common.extensions

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.fragment.NavHostFragment
import clipto.common.presentation.mvvm.base.BaseNavigationActivity

fun Fragment.withSafeFragmentManager(): FragmentManager? {
    val fm = fragmentManager
    if (fm != null && !fm.isDestroyed && !fm.isStateSaved) {
        return fm
    }
    return null
}

fun Fragment.withSafeChildFragmentManager(): FragmentManager? {
    val fm = childFragmentManager
    if (!fm.isDestroyed && !fm.isStateSaved) {
        return fm
    }
    return null
}

fun Fragment.getNavController(): NavController =
        activity?.let {
            if (it is BaseNavigationActivity) {
                it.navController
            } else {
                NavHostFragment.findNavController(this)
            }
        } ?: NavHostFragment.findNavController(this)

fun Fragment.navigateTo(
        @IdRes resId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null) {
    getNavController().navigateSafe(resId, args, navOptions, navigatorExtras)
}

private fun NavController.navigateSafe(
        @IdRes resId: Int,
        args: Bundle? = null,
        navOptions: NavOptions? = null,
        navigatorExtras: Navigator.Extras? = null) {
    try {
        this.navigate(resId, args, navOptions, navigatorExtras)
    } catch (e: Exception) {
        //
    }
}