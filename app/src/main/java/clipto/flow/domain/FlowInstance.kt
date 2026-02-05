package clipto.flow.domain

import java.util.*

data class FlowInstance(
    val flowId: String,
    val flowVersion: Int,
    val user: User,
    val status: Status,
    val platform: Platform,
    val duration: Long = 0,
    val createDate: Date = Date(),
    val updateDate: Date = createDate,
    val errors: List<Error> = emptyList(),
    val params: List<FlowParam<Any>> = emptyList(),
    val transitions: List<FlowInstance> = emptyList()
)