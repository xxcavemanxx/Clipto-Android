package clipto.dynamic.models

import android.os.Build
import clipto.common.misc.AndroidUtils
import com.wb.clipboard.R

enum class DeviceInfoType(val titleRes: Int, val id: String, val provider: () -> String) {

    IP_ADDRESS(R.string.clip_add_dynamic_value_ip, "ip", { AndroidUtils.getIpAddress(true) }),

    PLATFORM(R.string.clip_add_dynamic_value_platform, "platform", { "${Build.BRAND} ${Build.MODEL} (${Build.VERSION.SDK_INT})" }),

    ;

    companion object {
        fun getById(id: String?): DeviceInfoType? = values().find { it.id == id }

        fun getByIdOrDefault(id: String?): DeviceInfoType = getById(id) ?: PLATFORM
    }

}