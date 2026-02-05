package clipto.common.presentation.mvvm.base

import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment

class StatefulNavHostFragment : NavHostFragment() {

    override fun createFragmentNavigator(): Navigator<out FragmentNavigator.Destination> =
            StatefulFragmentNavigator(requireContext(), childFragmentManager, id)

}