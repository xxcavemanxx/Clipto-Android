package clipto.action.intent

import java.io.Serializable

interface IntentAction : Serializable {

    fun getSize(): Int = 0

    companion object {
        const val SIZE_NOT_SERIALIZABLE = -1
    }

}