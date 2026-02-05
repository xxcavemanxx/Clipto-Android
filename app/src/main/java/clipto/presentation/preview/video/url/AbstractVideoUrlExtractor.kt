package clipto.presentation.preview.video.url

import android.content.Context
import android.text.TextUtils
import android.webkit.JavascriptInterface
import clipto.common.logging.L
import clipto.common.misc.AndroidUtils
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

abstract class AbstractVideoUrlExtractor(val context: Context) : IVideoUrlExtractor {

    companion object {
        val stsPattern by lazy { Pattern.compile("\"sts\"\\s*:\\s*(\\d+)") }
        val jsPattern by lazy { Pattern.compile("\"assets\":.+?\"js\":\\s*(\"[^\"]+\")") }
        val sigPattern by lazy { Pattern.compile("\\.sig\\|\\|([a-zA-Z0-9$]+)\\(") }
        val sigPattern2 by lazy { Pattern.compile("[\"']signature[\"']\\s*,\\s*([a-zA-Z0-9$]+)\\(") }
        val stmtVarPattern by lazy { Pattern.compile("var\\s") }
        val stmtReturnPattern by lazy { Pattern.compile("return(?:\\s+|$)") }
        val exprParensPattern by lazy { Pattern.compile("[()]") }
        val playerIdPattern by lazy { Pattern.compile(".*?-([a-zA-Z0-9_-]+)(?:/watch_as3|/html5player(?:-new)?|(?:/[a-z]{2}_[A-Z]{2})?/base)?\\.([a-z]+)$") }
        val exprName = "[a-zA-Z_$][a-zA-Z_$0-9]*"
    }

    protected open fun downloadUrlContent(url: String): String? {
        L.log(this, "downloadUrlContent: {}", url)
        return downloadUrlContent(url, null, true)
    }

    protected open fun downloadUrlContent(url: String, headers: HashMap<String, String>?, tryGzip: Boolean): String? {
        var canRetry = true
        var httpConnectionStream: InputStream? = null
        var done = false
        var result: StringBuilder? = null
        var httpConnection: URLConnection? = null
        try {
            var downloadUrl = URL(url)
            httpConnection = downloadUrl.openConnection()
            httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20150101 Firefox/47.0 (Chrome)")
            if (tryGzip) {
                httpConnection.addRequestProperty("Accept-Encoding", "gzip, deflate")
            }
            httpConnection.addRequestProperty("Accept-Language", "en-us,en;q=0.5")
            httpConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            httpConnection.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7")
            if (headers != null) {
                for ((key, value) in headers) {
                    httpConnection.addRequestProperty(key, value)
                }
            }
            httpConnection.connectTimeout = 5000
            httpConnection.readTimeout = 5000
            if (httpConnection is HttpURLConnection) {
                val httpURLConnection = httpConnection
                httpURLConnection.instanceFollowRedirects = true
                val status = httpURLConnection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    val newUrl = httpURLConnection.getHeaderField("Location")
                    val cookies = httpURLConnection.getHeaderField("Set-Cookie")
                    downloadUrl = URL(newUrl)
                    httpConnection = downloadUrl.openConnection()
                    httpConnection.setRequestProperty("Cookie", cookies)
                    httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:10.0) Gecko/20150101 Firefox/47.0 (Chrome)")
                    if (tryGzip) {
                        httpConnection.addRequestProperty("Accept-Encoding", "gzip, deflate")
                    }
                    httpConnection.addRequestProperty("Accept-Language", "en-us,en;q=0.5")
                    httpConnection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    httpConnection.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7")
                    if (headers != null) {
                        for ((key, value) in headers) {
                            httpConnection.addRequestProperty(key, value)
                        }
                    }
                }
            }
            httpConnection.connect()
            if (tryGzip) {
                try {
                    httpConnectionStream = GZIPInputStream(httpConnection.getInputStream())
                } catch (e: Exception) {
                    try {
                        httpConnectionStream?.close()
                    } catch (ignore: Exception) {
                    }
                    httpConnection = downloadUrl.openConnection()
                    httpConnection.connect()
                    httpConnectionStream = httpConnection.getInputStream()
                }
            } else {
                httpConnectionStream = httpConnection.getInputStream()
            }
        } catch (e: Throwable) {
            if (e is SocketTimeoutException) {
                if (AndroidUtils.isConnected(context)) {
                    canRetry = false
                }
            } else if (e is UnknownHostException) {
                canRetry = false
            } else if (e is SocketException) {
                if (e.message != null && e.message!!.contains("ECONNRESET")) {
                    canRetry = false
                }
            } else if (e is FileNotFoundException) {
                canRetry = false
            }
            onError(e)
        }
        if (canRetry) {
            try {
                if (httpConnection is HttpURLConnection) {
                    val code = httpConnection.responseCode
                    if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_ACCEPTED && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                        //canRetry = false;
                    }
                }
            } catch (e: Exception) {
                onError(e)
            }
            if (httpConnectionStream != null) {
                try {
                    val data = ByteArray(1024 * 32)
                    while (true) {
                        try {
                            val read = httpConnectionStream.read(data)
                            if (read > 0) {
                                if (result == null) {
                                    result = StringBuilder()
                                }
                                result.append(String(data, 0, read, StandardCharsets.UTF_8))
                            } else if (read == -1) {
                                done = true
                                break
                            } else {
                                break
                            }
                        } catch (e: Exception) {
                            onError(e)
                            break
                        }
                    }
                } catch (e: Throwable) {
                    onError(e)
                }
            }
            try {
                httpConnectionStream?.close()
            } catch (e: Throwable) {
                onError(e)
            }
        }
        return if (done) result.toString() else null
    }

    interface CallJavaResultInterface {
        fun jsCallFinished(value: String?)
    }

    class JavaScriptInterface(private val callJavaResultInterface: CallJavaResultInterface) {

        @JavascriptInterface
        fun returnResultToJava(value: String?) {
            callJavaResultInterface.jsCallFinished(value)
        }

    }

    inner class JSExtractor(private val jsCode: String) {
        var codeLines = ArrayList<String?>()
        private val operators = arrayOf("|", "^", "&", ">>", "<<", "-", "+", "%", "/", "*")
        private val assignOperators = arrayOf("|=", "^=", "&=", ">>=", "<<=", "-=", "+=", "%=", "/=", "*=", "=")

        private fun interpretExpression(exprParam: String, localVars: HashMap<String, String?>, allowRecursion: Int) {
            var expr = exprParam
            expr = expr.trim { it <= ' ' }
            if (TextUtils.isEmpty(expr)) {
                return
            }
            if (expr[0] == '(') {
                var parensCount = 0
                val matcher = exprParensPattern.matcher(expr)
                while (matcher.find()) {
                    val group = matcher.group(0)!!
                    if (group.indexOf('0') == '('.toInt()) {
                        parensCount++
                    } else {
                        parensCount--
                        if (parensCount == 0) {
                            val subExpr = expr.substring(1, matcher.start())
                            interpretExpression(subExpr, localVars, allowRecursion)
                            val remainingExpr = expr.substring(matcher.end()).trim { it <= ' ' }
                            expr = if (TextUtils.isEmpty(remainingExpr)) {
                                return
                            } else {
                                remainingExpr
                            }
                            break
                        }
                    }
                }
                if (parensCount != 0) {
                    throw Exception(String.format("Premature end of parens in %s", expr))
                }
            }
            for (a in assignOperators.indices) {
                val func = assignOperators[a]
                val matcher = Pattern.compile(String.format(Locale.US, "(?x)(%s)(?:\\[([^\\]]+?)\\])?\\s*%s(.*)$", exprName, Pattern.quote(func))).matcher(expr)
                if (!matcher.find()) {
                    continue
                }
                interpretExpression(matcher.group(3)!!, localVars, allowRecursion - 1)
                val index = matcher.group(2)!!
                if (!TextUtils.isEmpty(index)) {
                    interpretExpression(index, localVars, allowRecursion)
                } else {
                    localVars[matcher.group(1)!!] = ""
                }
                return
            }
            try {
                expr.toInt()
                return
            } catch (e: Exception) {
                //ignore
            }
            var matcher = Pattern.compile(String.format(Locale.US, "(?!if|return|true|false)(%s)$", exprName)).matcher(expr)
            if (matcher.find()) {
                return
            }
            if (expr[0] == '"' && expr[expr.length - 1] == '"') {
                return
            }
            try {
                JSONObject(expr).toString()
                return
            } catch (e: Exception) {
                //ignore
            }
            matcher = Pattern.compile(String.format(Locale.US, "(%s)\\[(.+)\\]$", exprName)).matcher(expr)
            if (matcher.find()) {
                val `val` = matcher.group(1)
                interpretExpression(matcher.group(2)!!, localVars, allowRecursion - 1)
                return
            }
            matcher = Pattern.compile(String.format(Locale.US, "(%s)(?:\\.([^(]+)|\\[([^]]+)\\])\\s*(?:\\(+([^()]*)\\))?$", exprName)).matcher(expr)
            if (matcher.find()) {
                val variable = matcher.group(1)!!
                val m1 = matcher.group(2)!!
                val m2 = matcher.group(3)!!
                val member = (if (TextUtils.isEmpty(m1)) m2 else m1).replace("\"", "")
                val arg_str = matcher.group(4)
                if (localVars[variable] == null) {
                    extractObject(variable)
                }
                if (arg_str == null) {
                    return
                }
                if (expr[expr.length - 1] != ')') {
                    throw Exception("last char not ')'")
                }
                var argvals: Array<String?>
                if (arg_str.isNotEmpty()) {
                    val args = arg_str.split(",").toTypedArray()
                    for (a in args.indices) {
                        interpretExpression(args[a], localVars, allowRecursion)
                    }
                }
                return
            }
            matcher = Pattern.compile(String.format(Locale.US, "(%s)\\[(.+)\\]$", exprName)).matcher(expr)
            if (matcher.find()) {
                val `val`: Any? = localVars[matcher.group(1)!!]
                interpretExpression(matcher.group(2)!!, localVars, allowRecursion - 1)
                return
            }
            for (a in operators.indices) {
                val func = operators[a]
                matcher = Pattern.compile(String.format(Locale.US, "(.+?)%s(.+)", Pattern.quote(func))).matcher(expr)
                if (!matcher.find()) {
                    continue
                }
                val abort = BooleanArray(1)
                interpretStatement(matcher.group(1)!!, localVars, abort, allowRecursion - 1)
                if (abort[0]) {
                    throw Exception(String.format("Premature left-side return of %s in %s", func, expr))
                }
                interpretStatement(matcher.group(2)!!, localVars, abort, allowRecursion - 1)
                if (abort[0]) {
                    throw Exception(String.format("Premature right-side return of %s in %s", func, expr))
                }
            }
            matcher = Pattern.compile(String.format(Locale.US, "^(%s)\\(([a-zA-Z0-9_$,]*)\\)$", exprName)).matcher(expr)
            if (matcher.find()) {
                val fname = matcher.group(1)!!
                extractFunction(fname)
            }
            throw Exception(String.format("Unsupported JS expression %s", expr))
        }

        private fun interpretStatement(stmtParam: String, localVars: HashMap<String, String?>, abort: BooleanArray, allowRecursion: Int) {
            var stmt = stmtParam
            if (allowRecursion < 0) {
                throw Exception("recursion limit reached")
            }
            abort[0] = false
            stmt = stmt.trim { it <= ' ' }
            var matcher = stmtVarPattern.matcher(stmt)
            val expr: String
            if (matcher.find()) {
                expr = stmt.substring(matcher.group(0)!!.length)
            } else {
                matcher = stmtReturnPattern.matcher(stmt)
                if (matcher.find()) {
                    expr = stmt.substring(matcher.group(0)!!.length)
                    abort[0] = true
                } else {
                    expr = stmt
                }
            }
            interpretExpression(expr, localVars, allowRecursion)
        }

        private fun extractObject(objname: String): HashMap<String, Any> {
            val funcName = "(?:[a-zA-Z$0-9]+|\"[a-zA-Z$0-9]+\"|'[a-zA-Z$0-9]+')"
            val obj = HashMap<String, Any>()
            //                                                                                         ?P<fields>
            var matcher = Pattern.compile(String.format(Locale.US, "(?:var\\s+)?%s\\s*=\\s*\\{\\s*((%s\\s*:\\s*function\\(.*?\\)\\s*\\{.*?\\}(?:,\\s*)?)*)\\}\\s*;", Pattern.quote(objname), funcName)).matcher(jsCode)
            var fields: String? = null
            while (matcher.find()) {
                val code = matcher.group()
                fields = matcher.group(2)
                if (TextUtils.isEmpty(fields)) {
                    continue
                }
                if (!codeLines.contains(code)) {
                    codeLines.add(matcher.group())
                }
                break
            }
            //                          ?P<key>                            ?P<args>     ?P<code>
            matcher = Pattern.compile(String.format("(%s)\\s*:\\s*function\\(([a-z,]+)\\)\\{([^}]+)\\}", funcName)).matcher(fields!!)
            while (matcher.find()) {
                val argnames = matcher.group(2)!!.split(",").toTypedArray()
                buildFunction(argnames, matcher.group(3)!!)
            }
            return obj
        }

        private fun buildFunction(argNames: Array<String>, funcCode: String) {
            val localVars = HashMap<String, String?>()
            for (a in argNames.indices) {
                localVars[argNames[a]] = ""
            }
            val stmts = funcCode.split(";").toTypedArray()
            val abort = BooleanArray(1)
            for (a in stmts.indices) {
                interpretStatement(stmts[a], localVars, abort, 100)
                if (abort[0]) {
                    return
                }
            }
        }

        fun extractFunction(funcName: String): String {
            try {
                val quote = Pattern.quote(funcName)
                val funcPattern = Pattern.compile(String.format(Locale.US, "(?x)(?:function\\s+%s|[{;,]\\s*%s\\s*=\\s*function|var\\s+%s\\s*=\\s*function)\\s*\\(([^)]*)\\)\\s*\\{([^}]+)\\}", quote, quote, quote))
                val matcher = funcPattern.matcher(jsCode)
                if (matcher.find()) {
                    val group = matcher.group()
                    if (!codeLines.contains(group)) {
                        codeLines.add("$group;")
                    }
                    buildFunction(matcher.group(1)!!.split(",").toTypedArray(), matcher.group(2)!!)
                }
            } catch (e: Exception) {
                codeLines.clear()
                onError(e)
            }
            return TextUtils.join("", codeLines)
        }

    }

    protected fun onError(th: Throwable) {
        L.log(this, "onError", th)
    }

}