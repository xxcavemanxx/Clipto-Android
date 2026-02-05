package clipto.flow.domain

data class FlowParam<O>(
    val required: Boolean = false,
    val key: String,
    val value: O?
)