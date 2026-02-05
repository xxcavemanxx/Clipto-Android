package clipto.flow.domain

import java.util.*

data class SLA(
    /** maximum duration before the flow is canceled **/
    val maxDuration: Long = 0,
    /** minimum duration before the flow can be completed **/
    val minDuration: Long = 0,
    /** date, when the flow will be available for execution **/
    val delayDate: Date? = null,
    /** date, when the flow will not be available anymore and will be automatically marked as expired **/
    val expireDate: Date? = null
)