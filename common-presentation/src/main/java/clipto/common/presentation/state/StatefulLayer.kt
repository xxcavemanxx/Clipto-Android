package clipto.common.presentation.state

abstract class StatefulLayer<S, V> : Predicate<S> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatefulLayer<*, *>

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "StatefulLayer(id='$id')"
    }

    override fun test(state: S): Boolean = true
    abstract fun apply(state: S, view: V)
    abstract val id: String
}