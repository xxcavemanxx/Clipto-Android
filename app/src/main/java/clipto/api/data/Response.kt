package clipto.api.data

import clipto.dao.firebase.mapper.DateMapper
import clipto.domain.UserRole
import java.util.*

data class Response(
    val accessRole: UserRole = UserRole.USER,
    val serverTime: Date? = null,
    val data: Any? = null
) {
    companion object {
        fun wrap(any: Any?): Response {
            if (any is Map<*, *>) {
                val accessRole = UserRole.byId(any["accessRole"]?.toString())
                val serverTime = DateMapper.toDate(any["serverTime"])
                val data = any["data"]
                return Response(
                    accessRole = accessRole,
                    serverTime = serverTime,
                    data = data
                )
            }
            return Response()
        }
    }
}