package clipto.common.presentation.mvvm.model

open class DataLoadingState {

    companion object {
        val NO_INTERNET = DataLoadingState()
        val LOADING = DataLoadingState()
        val LOADED = DataLoadingState()
    }

    data class Loading(val timeout: Long = 120000) : DataLoadingState()

    data class Error(
            val code: String? = null,
            val message: String? = null,
            val throwable: Throwable? = null
    ) : DataLoadingState() {
        override fun toString(): String {
            return "Error(code=$code, message=$message, throwable=$throwable)"
        }
    }
}