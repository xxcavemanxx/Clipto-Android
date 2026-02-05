package clipto.presentation.preview.video.url

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.TextUtils
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import clipto.common.extensions.notNull
import clipto.common.presentation.mvvm.RxViewModel
import clipto.presentation.preview.PreviewHelper
import org.json.JSONTokener
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.regex.Matcher

open class AbstractYoutubeVideoUrlExtractor(context: Context) : AbstractVideoUrlExtractor(context) {

    override fun getVideoId(url: String): String? = PreviewHelper.getYoutubeId(url)

    @SuppressLint("SetJavaScriptEnabled")
    override fun extractVideoData(url: String, videoId: String): UrlData? {
        val urlData = UrlData()

        val sig: String
        var matcher: Matcher
        val embedCode: String? = downloadUrlContent("https://www.youtube.com/embed/$videoId")
        var params = "video_id=$videoId&ps=default&gl=US&hl=en"
        try {
            params += "&eurl=" + URLEncoder.encode("https://youtube.googleapis.com/v/$videoId", "UTF-8")
        } catch (e: Exception) {
            onError(e)
        }
        if (embedCode != null) {
            matcher = stsPattern.matcher(embedCode)
            params += if (matcher.find()) {
                "&sts=" + embedCode.substring(matcher.start() + 6, matcher.end())
            } else {
                "&sts="
            }
        }
        urlData.type = "dash"

        var encrypted = false
        var otherUrl: String? = null
        val extra = arrayOf("", "&el=leanback", "&el=embedded", "&el=detailpage", "&el=vevo")
        for (i in extra.indices) {
            val videoInfo: String? = downloadUrlContent("https://www.youtube.com/get_video_info?" + params + extra[i])
            var exists = false
            var hls: String? = null
            var isLive = false
            if (videoInfo != null) {
                val args = videoInfo.split("&").toTypedArray()
                for (a in args.indices) {
                    if (args[a].startsWith("dashmpd")) {
                        exists = true
                        val args2 = args[a].split("=").toTypedArray()
                        if (args2.size == 2) {
                            try {
                                urlData.playbackUrl = URLDecoder.decode(args2[1], "UTF-8")
                            } catch (e: Exception) {
                                onError(e)
                            }
                        }
                    } else if (args[a].startsWith("url_encoded_fmt_stream_map")) {
                        val args2 = args[a].split("=").toTypedArray()
                        if (args2.size == 2) {
                            try {
                                val args3 = URLDecoder.decode(args2[1], "UTF-8").split("[&,]").toTypedArray()
                                var currentUrl: String? = null
                                var isMp4 = false
                                for (c in args3.indices) {
                                    val args4 = args3[c].split("=").toTypedArray()
                                    if (args4[0].startsWith("type")) {
                                        val type = URLDecoder.decode(args4[1], "UTF-8")
                                        if (type.contains("video/mp4")) {
                                            isMp4 = true
                                        }
                                    } else if (args4[0].startsWith("url")) {
                                        currentUrl = URLDecoder.decode(args4[1], "UTF-8")
                                    } else if (args4[0].startsWith("itag")) {
                                        currentUrl = null
                                        isMp4 = false
                                    }
                                    if (isMp4 && currentUrl != null) {
                                        otherUrl = currentUrl
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                onError(e)
                            }
                        }
                    } else if (args[a].startsWith("use_cipher_signature")) {
                        val args2 = args[a].split("=").toTypedArray()
                        if (args2.size == 2) {
                            if (args2[1].toLowerCase(Locale.ROOT) == "true") {
                                encrypted = true
                            }
                        }
                    } else if (args[a].startsWith("hlsvp")) {
                        val args2 = args[a].split("=").toTypedArray()
                        if (args2.size == 2) {
                            try {
                                hls = URLDecoder.decode(args2[1], "UTF-8")
                            } catch (e: Exception) {
                                onError(e)
                            }
                        }
                    } else if (args[a].startsWith("livestream")) {
                        val args2 = args[a].split("=").toTypedArray()
                        if (args2.size == 2) {
                            if (args2[1].toLowerCase(Locale.ROOT) == "1") {
                                isLive = true
                            }
                        }
                    }
                }
            }
            if (isLive) {
                if (hls == null || encrypted || hls.contains("/s/")) {
                    return urlData
                } else {
                    urlData.playbackUrl = hls
                    urlData.type = "hls"
                }
            }
            if (exists) {
                break
            }
        }
        if (urlData.playbackUrl == null && otherUrl != null) {
            urlData.playbackUrl = otherUrl
            urlData.type = "other"
        }

        if (urlData.playbackUrl != null && (encrypted || urlData.playbackUrl!!.contains("/s/")) && embedCode != null) {
            encrypted = true
            val index: Int = urlData.playbackUrl!!.indexOf("/s/")
            var index2: Int = urlData.playbackUrl!!.indexOf('/', index + 10)
            if (index != -1) {
                if (index2 == -1) {
                    index2 = urlData.playbackUrl!!.length
                }
                sig = urlData.playbackUrl!!.substring(index, index2)
                var jsUrl: String? = null
                matcher = jsPattern.matcher(embedCode)
                if (matcher.find()) {
                    try {
                        val tokener = JSONTokener(matcher.group(1))
                        val value = tokener.nextValue()
                        if (value is String) {
                            jsUrl = value
                        }
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
                if (jsUrl != null) {
                    matcher = playerIdPattern.matcher(jsUrl)
                    val playerId: String? =
                            if (matcher.find()) {
                                matcher.group(1)!! + matcher.group(2)
                            } else {
                                null
                            }
                    var functionCode: String? = null
                    var functionName: String? = null
                    val preferences: SharedPreferences = context.getSharedPreferences("youtubecode", Activity.MODE_PRIVATE)
                    if (playerId != null) {
                        functionCode = preferences.getString(playerId, null)
                        functionName = preferences.getString(playerId + "n", null)
                    }
                    if (functionCode == null) {
                        if (jsUrl.startsWith("//")) {
                            jsUrl = "https:$jsUrl"
                        } else if (jsUrl.startsWith("/")) {
                            jsUrl = "https://www.youtube.com$jsUrl"
                        }
                        val jsCode: String? = downloadUrlContent(jsUrl)
                        if (jsCode != null) {
                            matcher = sigPattern.matcher(jsCode)
                            if (matcher.find()) {
                                functionName = matcher.group(1)
                            } else {
                                matcher = sigPattern2.matcher(jsCode)
                                if (matcher.find()) {
                                    functionName = matcher.group(1)
                                }
                            }
                            if (functionName != null) {
                                try {
                                    val extractor = JSExtractor(jsCode)
                                    functionCode = extractor.extractFunction(functionName)
                                    if (!TextUtils.isEmpty(functionCode) && playerId != null) {
                                        preferences.edit().putString(playerId, functionCode).putString(playerId + "n", functionName).apply()
                                    }
                                } catch (e: Exception) {
                                    onError(e)
                                }
                            }
                        }
                    }
                    if (!TextUtils.isEmpty(functionCode)) {
                        val interfaceName = "JavaScriptInterface"
                        functionCode += if (Build.VERSION.SDK_INT >= 21) {
                            functionName + "('" + sig.substring(3) + "');"
                        } else {
                            "window." + interfaceName + ".returnResultToJava(" + functionName + "('" + sig.substring(3) + "'));"
                        }
                        val functionCodeFinal = functionCode.notNull()
                        try {
                            val countDownLatch = CountDownLatch(1)
                            RxViewModel.defaultViewScheduler.value.scheduleDirect {
                                val webView = WebView(context)
                                webView.addJavascriptInterface(JavaScriptInterface(object : CallJavaResultInterface {
                                    override fun jsCallFinished(value: String?) {
                                        urlData.playbackUrl = urlData.playbackUrl!!.replace(sig, "/signature/$value")
                                        countDownLatch.countDown()
                                    }
                                }), interfaceName)
                                val webSettings: WebSettings = webView.settings
                                webSettings.javaScriptEnabled = true
                                webSettings.defaultTextEncodingName = "utf-8"
                                if (Build.VERSION.SDK_INT >= 21) {
                                    webView.evaluateJavascript(functionCodeFinal) { value: String ->
                                        urlData.playbackUrl = urlData.playbackUrl!!.replace(sig, "/signature/" + value.substring(1, value.length - 1))
                                        countDownLatch.countDown()
                                    }
                                } else {
                                    try {
                                        val javascript = "<script>$functionCodeFinal</script>"
                                        val data = javascript.toByteArray(StandardCharsets.UTF_8)
                                        val base64 = Base64.encodeToString(data, Base64.DEFAULT)
                                        webView.loadUrl("data:text/html;charset=utf-8;base64,$base64")
                                    } catch (e: Exception) {
                                        onError(e)
                                    }
                                }
                            }
                            countDownLatch.await()
                        } catch (e: Exception) {
                            onError(e)
                        }
                    }
                }
            }
        }

        return urlData
    }

}