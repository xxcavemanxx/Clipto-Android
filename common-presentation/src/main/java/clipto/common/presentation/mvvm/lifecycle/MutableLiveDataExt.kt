package clipto.common.presentation.mvvm.lifecycle

import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import clipto.common.logging.L

open class MutableLiveDataExt<T> : MutableLiveData<T>() {

    @CallSuper
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, Observer { t ->
            if (owner is Fragment) {
                val parent = owner.parentFragment
                L.log(this, "observe from fragment: name={}, hidden={}, parent hidden={}", owner.javaClass.simpleName, owner.isHidden, parent?.isHidden)
            }
            observer.onChanged(t)
        })
    }

}