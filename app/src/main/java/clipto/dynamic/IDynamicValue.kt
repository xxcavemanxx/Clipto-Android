package clipto.dynamic

interface IDynamicValue {

    fun getKey(): String

    fun getValue(context: DynamicValueContext): CharSequence
}