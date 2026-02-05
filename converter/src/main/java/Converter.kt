import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText
import java.io.File

object Converter {

    val mapping = mapOf(
            "values" to "en-us",
            "values-ar" to "ar",
            "values-be" to "be",
            "values-bg" to "bg",
            "values-ca" to "ca",
            "values-da" to "da",
            "values-de" to "de",
            "values-el" to "el",
            "values-es" to "es",
            "values-es-rUS" to "es-us",
            "values-et" to "et",
            "values-fil" to "fil",
            "values-fr" to "fr",
            "values-hi" to "hi",
            "values-hu" to "hu",
            "values-in" to "id",
            "values-it" to "it",
            "values-ja" to "ja",
            "values-ko" to "ko-kr",
            "values-nl" to "nl",
            "values-pl" to "pl",
            "values-pt-rBR" to "pt-br",
            "values-ru" to "ru",
            "values-sl" to "sl",
            "values-sv" to "sv",
            "values-tr" to "tr",
            "values-uk" to "uk",
            "values-vi" to "vi",
            "values-zh-rCN" to "zh-hans",
            "values-zh-rTW" to "zh-hant"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val xmlModule = JacksonXmlModule()
        xmlModule.setDefaultUseWrapper(false)
        val xmlMapper = XmlMapper(xmlModule)
        File("app/src/main/res").listFiles().forEach {
            if (mapping[it.name] != null) {
                val outFile = File("converter/out/${mapping[it.name]}/index.js")
                val strings = File(it, "strings.xml")
                outFile.parentFile.mkdirs()
                val res = xmlMapper.readValue(strings, Resources::class.java)
                println(">>>===== ${strings.absolutePath} -> ${res.strings.size}, ${res.plurals.size}")
                val jsonBuilder = StringBuilder()
                jsonBuilder.append("export default {\n")
                res.strings.forEachIndexed { index, stringRes ->
                    val key = stringRes.name?.replace('.', '_')
                    val value = stringRes.text
                            ?.let { text ->
                                var newText = text.replaceFirst("%s", "{n}")
                                var idx = 1
                                while (newText.contains("%s")) {
                                    newText = newText.replaceFirst("%s", "{n${idx++}}")
                                }
                                newText
                            }
                            ?.replace('\n', ' ')
                            ?.replace("\\\"", "\"")
                    jsonBuilder.append("  $key: '${trimSpaces(value)}'")
                    if (index < res.strings.size - 1 || res.plurals.isNotEmpty()) {
                        jsonBuilder.append(",\n")
                    }
                }
                res.plurals.forEachIndexed { index, pluralRes ->
                    val key = pluralRes.name?.replace('.', '_')
                    val value = pluralRes.item.map { it.text?.replace("%s", "{n}") }.joinToString(" | ")
                    jsonBuilder.append("  $key: '${trimSpaces(value)}'")
                    if (index < res.plurals.size - 1) {
                        jsonBuilder.append(",\n")
                    }
                }
                jsonBuilder.append("\n}\n")

                outFile.writeText(jsonBuilder.toString(), Charsets.UTF_8)
            }
        }
    }

    private fun trimSpaces(value: String?): String? = value?.split("\n")?.map { it.trim() }?.joinToString("\n")

    @JacksonXmlRootElement
    class Resources {
        @JacksonXmlProperty(isAttribute = true, localName = "ignore")
        var ignore: String? = null

        @JacksonXmlProperty(isAttribute = true, localName = "tools")
        var tools: String? = null

        @JacksonXmlProperty(localName = "string")
        var strings: ArrayList<StringRes> = arrayListOf()

        @JacksonXmlProperty(localName = "plurals")
        var plurals: ArrayList<PluralsRes> = arrayListOf()
    }

    class StringRes {
        @JacksonXmlProperty(isAttribute = true, localName = "name")
        var name: String? = null

        @JacksonXmlProperty(isAttribute = true, localName = "translatable")
        var translatable: Boolean = true

        @JacksonXmlText
        var text: String? = null
    }

    class PluralsRes {
        @JacksonXmlProperty(isAttribute = true, localName = "name")
        var name: String? = null
        var item: ArrayList<PluralItem> = arrayListOf()
    }

    class PluralItem {
        @JacksonXmlProperty(isAttribute = true, localName = "quantity")
        var quantity: String? = null

        @JacksonXmlText
        var text: String? = null
    }

}
