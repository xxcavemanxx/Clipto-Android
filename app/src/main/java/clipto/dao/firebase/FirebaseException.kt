package clipto.dao.firebase

import clipto.common.presentation.mvvm.model.DataLoadingState

data class FirebaseException(
    val code: String,
    val throwable: Throwable
) : DataLoadingState()