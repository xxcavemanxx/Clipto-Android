package clipto.common.presentation.mvvm.base

import android.app.Activity

interface FragmentBackButtonListener {

    /**
     * It should return `true` in case [Activity.onBackPressed] event was consumed
     * and no actions should be done by Activity.
     * It should return `false` if activity has to be finished.
     */
    fun onFragmentBackPressed(): Boolean = true
}