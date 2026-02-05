package clipto.presentation.usecases.data

import clipto.domain.Clip
import clipto.domain.FastAction
import clipto.domain.FileRef
import clipto.presentation.clip.details.ClipDetails

data class ShowNoteDetailsRequest(
    val onAttachment: (attachment: FileRef) -> Unit = {},
    val onFastAction: (action: FastAction) -> Unit = {},
    val onChanged: (config: ClipDetails) -> Unit = {},
    val onValue: (value: String) -> Unit = {},
    val clip: Clip
)