package clipto.common.presentation.state

import android.view.View
import clipto.common.extensions.gone
import clipto.common.extensions.visible
import clipto.common.logging.L.log

class ViewState<STATE>(vararg layers: Layer<STATE, out View>) {

    private var layerList = mutableListOf(*layers)

    fun addLayer(layer: Layer<STATE, out View>): ViewState<STATE> {
        layerList.add(layer)
        return this
    }

    fun rebuild(state: STATE) {
        val visible = mutableSetOf<View>()
        val invisible = mutableSetOf<View>()
        layerList.forEach {
            if (it.apply(state)) {
                invisible.remove(it.layerView)
                visible.add(it.layerView)
            } else if (!visible.contains(it.layerView)) {
                invisible.add(it.layerView)
            }
        }
        invisible.forEach {
            log(this, "invisible layer :: {}, {}", it.contentDescription, it)
            it.gone()
        }
    }

    abstract class Layer<STATE, V : View>(val layerView: V, val id: String) {

        internal fun apply(state: STATE): Boolean {
            return if (canApply(state)) {
                if (canBind(state)) {
                    log(this@Layer, "bind layer :: {}", id)
                    layerView.setTag(TAG_ID, id)
                    doApply(state)
                }
                layerView.visible()
                true
            } else {
                false
            }
        }

        open fun canBind(state: STATE): Boolean = layerView.getTag(TAG_ID) != id
        abstract fun canApply(state: STATE): Boolean
        abstract fun doApply(state: STATE)

    }

    companion object {
        private const val TAG_ID: Int = 1234567890
    }

}