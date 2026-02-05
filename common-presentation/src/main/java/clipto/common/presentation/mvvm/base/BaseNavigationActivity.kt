package clipto.common.presentation.mvvm.base

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

/**
 * Sample layout:
 *
 * <?xml version="1.0" encoding="utf-8"?>
 * <layout xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto">
 *     <FrameLayout android:layout_width="match_parent" android:layout_height="match_parent">
 *         <fragment
 *             android:id="@+id/navHostFragment"
 *             android:name="clipto.common.presentation.mvvm.base.StatefulNavHostFragment"
 *             android:layout_width="match_parent"
 *             android:layout_height="match_parent"
 *             app:defaultNavHost="true" />
 *     </FrameLayout>
 * </layout>
 *
 */
abstract class BaseNavigationActivity : BaseActivity() {

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navFragment = getNavHostFragment()
        navController = navFragment.navController
    }

    override fun onSupportNavigateUp(): Boolean = !fragmentBackPress() && navController.navigateUp()

    private fun fragmentBackPress(): Boolean {
        val navHost = getNavHostFragment()
        val fragment = getTopBackButtonListenerFragment(navHost)
        return fragment?.onFragmentBackPressed() ?: false
    }

    private fun getTopBackButtonListenerFragment(navHost: Fragment?): FragmentBackButtonListener? {
        val fragments = navHost?.childFragmentManager?.fragments
        return fragments?.lastOrNull() as? FragmentBackButtonListener
    }

    protected abstract fun getNavHostFragment(): NavHostFragment

}