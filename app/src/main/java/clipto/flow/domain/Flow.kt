package clipto.flow.domain

import clipto.domain.AttributedObject
import java.util.*

open class Flow : AttributedObject() {

    var id: String? = null
    var version: Int = 0

    var user: User? = null
    var platform: Platform? = null
    var createDate: Date? = null
    var updateDate: Date? = null
    var text: String? = null
    var sla: SLA? = null

    var params: List<FlowParam<Any>> = emptyList()
    var activities: List<Activity> = emptyList()

    fun apply(from: Flow): Flow {
        super.apply(from)

        id = from.id
        version = from.version

        user = from.user
        platform = from.platform
        createDate = from.createDate
        updateDate = from.updateDate
        text = from.text
        sla = from.sla

        params = from.params
        activities = from.activities

        return this
    }
}