package clipto.common.misc

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object GsonUtils {

    private const val utcDateFormat = "yyyy-MM-dd'T'HH:mm:ss"
    private const val utcDateFormatWithMillis = "yyyy-MM-dd'T'HH:mm:ss.SSS"

    private val utcDateFormatter by lazy {
        SimpleDateFormat(utcDateFormat, Locale.UK)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    private val utcDateFormatterWithMillis by lazy {
        SimpleDateFormat(utcDateFormatWithMillis, Locale.UK)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    private val gson by lazy {
        GsonBuilder()
            .setDateFormat(utcDateFormat)
            .registerTypeAdapter(Date::class.java, GsonUtcDateAdapter(utcDateFormatter))
            .create()
    }

    fun get(): Gson = gson

    fun parseDate(dateString: String?): Date? {
        if (dateString == null) {
            return null
        }
        return runCatching { utcDateFormatter.parse(dateString) }.getOrNull()
    }

    fun formatDate(date: Date?): String? {
        if (date == null) {
            return null
        }
        return utcDateFormatter.format(date)
    }

    fun parseDateWithMillis(dateString: String?): Date? {
        if (dateString == null) {
            return null
        }
        return runCatching { utcDateFormatterWithMillis.parse(dateString) }.getOrNull()
    }

    fun formatDateWithMillis(date: Date?): String? {
        if (date == null) {
            return null
        }
        return utcDateFormatterWithMillis.format(date)
    }

    fun <T> toObjectSilent(json: String?, clazz: Class<T>): T? =
        try {
            if (json == null) {
                null
            } else {
                gson.fromJson(json, clazz)
            }
        } catch (_: Exception) {
            null
        }

    fun toStringSilent(any: Any?): String? =
        try {
            if (any == null) {
                null
            } else {
                gson.toJson(any)
            }
        } catch (_: Exception) {
            null
        }

}

class StringIgnoreNullOrBlankAdapter : TypeAdapter<String?>() {

    override fun read(`in`: JsonReader): String? {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return `in`.nextString()
    }

    override fun write(out: JsonWriter, value: String?) {
        if (value.isNullOrBlank()) {
            out.nullValue()
            return
        }
        out.value(value)
    }
}

class StringIgnoreEmptyAdapter : TypeAdapter<String?>() {

    override fun read(`in`: JsonReader): String? {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return `in`.nextString()
    }

    override fun write(out: JsonWriter, value: String?) {
        if (value.isNullOrEmpty()) {
            out.nullValue()
            return
        }
        out.value(value)
    }
}

class ShortIgnoreZeroAdapter : TypeAdapter<Short>() {

    override fun read(`in`: JsonReader): Short {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return ZERO
        }
        return `in`.nextInt().toShort()
    }

    override fun write(out: JsonWriter, value: Short) {
        if (value == ZERO) {
            out.nullValue()
            return
        }
        out.value(value)
    }

    companion object {
        private val ZERO = 0.toShort()
    }
}

class IntIgnoreZeroAdapter : TypeAdapter<Int>() {

    override fun read(`in`: JsonReader): Int {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return ZERO
        }
        return `in`.nextInt()
    }

    override fun write(out: JsonWriter, value: Int) {
        if (value == ZERO) {
            out.nullValue()
            return
        }
        out.value(value)
    }

    companion object {
        private val ZERO = 0
    }
}

class LongIgnoreZeroAdapter : TypeAdapter<Long>() {

    override fun read(`in`: JsonReader): Long {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return ZERO
        }
        return `in`.nextLong()
    }

    override fun write(out: JsonWriter, value: Long) {
        if (value == ZERO) {
            out.nullValue()
            return
        }
        out.value(value)
    }

    companion object {
        private val ZERO = 0L
    }
}

class FloatIgnoreZeroAdapter : TypeAdapter<Float>() {

    override fun read(`in`: JsonReader): Float {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return ZERO
        }
        return `in`.nextDouble().toFloat()
    }

    override fun write(out: JsonWriter, value: Float) {
        if (value == ZERO) {
            out.nullValue()
            return
        }
        out.value(value)
    }

    companion object {
        private val ZERO = 0f
    }
}

class DoubleIgnoreZeroAdapter : TypeAdapter<Double>() {

    override fun read(`in`: JsonReader): Double {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return ZERO
        }
        return `in`.nextDouble()
    }

    override fun write(out: JsonWriter, value: Double) {
        if (value == ZERO) {
            out.nullValue()
            return
        }
        out.value(value)
    }

    companion object {
        private val ZERO = 0.0
    }
}

class BooleanIgnoreFalseAdapter : TypeAdapter<Boolean>() {

    override fun read(`in`: JsonReader): Boolean {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return false
        }
        return `in`.nextBoolean()
    }

    override fun write(out: JsonWriter, value: Boolean) {
        if (!value) {
            out.nullValue()
            return
        }
        out.value(value)
    }
}

class DateOfBirthAdapter : TypeAdapter<Date?>() {

    private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    override fun read(`in`: JsonReader): Date? {
        if (`in`.peek() === JsonToken.NULL) {
            `in`.nextNull()
            return null
        }
        return formatter.parse(`in`.nextString())
    }

    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.value(formatter.format(value))
    }
}

class GsonUtcDateAdapter(private val utcDateFormatter: DateFormat) : JsonSerializer<Date>, JsonDeserializer<Date> {

    @Synchronized
    override fun serialize(date: Date, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
        return JsonPrimitive(utcDateFormatter.format(date))
    }

    @Synchronized
    override fun deserialize(jsonElement: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): Date {
        try {
            return utcDateFormatter.parse(jsonElement.asString)
        } catch (e: ParseException) {
            throw JsonParseException(e)
        }
    }
}