package clipto.domain

import java.util.*

data class TimeInterval(
        val period: TimePeriod,
        val from: Date? = null,
        val to: Date? = null
)