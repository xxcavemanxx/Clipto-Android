package clipto.dynamic.presentation.field.model

import clipto.dynamic.DynamicField

data class RequestResponse(
        val id: Long = System.currentTimeMillis(),
        val field: DynamicField,
        val viewMode: ViewMode,
        var resultCode: ResultCode? = null
)