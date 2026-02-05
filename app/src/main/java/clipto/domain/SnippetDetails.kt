package clipto.domain

data class SnippetDetails(
    val snippetId: String,
    val files: List<FileRef> = emptyList()
)