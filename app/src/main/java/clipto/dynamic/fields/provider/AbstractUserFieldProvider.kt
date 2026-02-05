package clipto.dynamic.fields.provider

import clipto.dynamic.DynamicField

abstract class AbstractUserFieldProvider<F : DynamicField> : AbstractFieldProvider<F>(), IUserFieldProvider<F>