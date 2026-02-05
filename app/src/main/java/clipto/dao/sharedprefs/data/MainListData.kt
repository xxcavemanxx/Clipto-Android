package clipto.dao.sharedprefs.data

import clipto.domain.MainAction
import com.google.gson.annotations.SerializedName

data class MainListData(
    @SerializedName("lastAction") val lastAction: MainAction? = null,
    @SerializedName("rememberLastAction") val rememberLastAction: Boolean = false
)