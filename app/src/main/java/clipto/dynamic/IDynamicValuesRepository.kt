package clipto.dynamic

import clipto.domain.Clip
import io.reactivex.Single

interface IDynamicValuesRepository {

    fun getFieldsCount(clip: Clip): Single<Int>

    fun process(text: CharSequence, config: DynamicValueConfig = DynamicValueConfig()): Single<CharSequence>

    fun getFormFields(text: CharSequence, config: DynamicValueConfig = DynamicValueConfig()): Single<List<FormField>>

}