package clipto.common.presentation.mvvm.lifecycle

import androidx.lifecycle.MutableLiveData

/**
 * Created by aash on 19/03/2018.
 */
class UniqueLiveData<T> : MutableLiveData<T>() {

    private var force = false

    fun postValueForce(value: T) {
        force = true
        super.postValue(value)
    }

    override fun setValue(value: T) {
        if (force || value != this.value) {
            super.setValue(value)
        }
        force = false
    }

    override fun postValue(value: T) {
        force = false
        if (value != this.value) {
            super.postValue(value)
        }
    }

}