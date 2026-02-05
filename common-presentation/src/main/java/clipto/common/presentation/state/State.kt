package clipto.common.presentation.state

import android.content.Context
import android.view.View
import clipto.common.R
import java.util.*

@Suppress("UNCHECKED_CAST")
abstract class State<S, V, C : State<S, V, C>> : IState<S, V> {

    private val layers: List<StatefulLayer<S, V>> = ArrayList(3)

    protected var context: Context? = null

    open fun withContext(context: Context?): C {
        this.context = context
        return this as C
    }

    override fun apply(state: S, view: V) {
        if (layers.isNotEmpty()) {
            val oldLayers = getLayers(view)
            val newLayers: MutableSet<StatefulLayer<S, V>> = LinkedHashSet(oldLayers.size)
            for (layer in layers) {
                if (layer.test(state)) {
                    newLayers.add(layer)
                }
            }
            if (newLayers != oldLayers) {
                applyLayers(state, view, newLayers)
                setLayers(view, newLayers)
            }
        }
        doApply(state, view)
        bind(view)
    }

    protected open fun doApply(state: S, view: V) = Unit

    private fun applyLayers(state: S, view: V, newLayers: Set<StatefulLayer<S, V>>) {
        for (layer in newLayers) {
            layer.apply(state, view)
        }
    }

    private fun bind(view: V) {
        if (view is View) {
            view.setTag(R.id.view_state, this)
        }
    }

    private fun setLayers(view: V, layers: Set<StatefulLayer<S, V>>) {
        if (view is View) {
            view.setTag(R.id.view_state_layers, layers)
        }
    }

    private fun getLayers(view: V): Set<StatefulLayer<S, V>> {
        if (view is View) {
            return view.getTag(R.id.view_state_layers) as Set<StatefulLayer<S, V>>? ?: emptySet()
        }
        return emptySet()
    }
}