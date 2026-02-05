package clipto.domain

import com.google.gson.annotations.SerializedName

data class RuneConfig(
        @SerializedName("id") val id: String,
        @SerializedName("colorLight") val colorLight: String,
        @SerializedName("colorDark") val colorDark: String
)