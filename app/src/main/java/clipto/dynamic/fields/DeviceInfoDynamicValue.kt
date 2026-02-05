package clipto.dynamic.fields

import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValue
import clipto.dynamic.models.DeviceInfoType

class DeviceInfoDynamicValue : DynamicValue(ID) {

    var type: String = DeviceInfoType.PLATFORM.id

    override fun getFieldValueUnsafe(): String = DeviceInfoType.getByIdOrDefault(type).provider.invoke()
    override fun apply(from: DynamicField) = Unit
    override fun hasValue(): Boolean = true
    override fun clear() = Unit

    companion object {
        const val ID = "device"
    }

}