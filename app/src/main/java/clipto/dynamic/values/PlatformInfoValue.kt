package clipto.dynamic.values

import android.os.Build
import clipto.dynamic.DynamicValueContext
import clipto.dynamic.DynamicValueType

class PlatformInfoValue : AbstractDynamicValue(DynamicValueType.PLATFORM.id) {

    override fun getValueUnsafe(context: DynamicValueContext): CharSequence {
        return "${Build.BRAND} ${Build.MODEL} (${Build.VERSION.SDK_INT})"
    }

}