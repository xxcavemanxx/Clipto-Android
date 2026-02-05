package clipto.api.data

data class CheckSessionRequest(
        val retryAttempt: Int,
        val notesCount: Int
)