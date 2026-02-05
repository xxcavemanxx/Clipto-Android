package clipto.domain

import android.app.SearchManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.util.Patterns
import android.util.SparseArray
import android.webkit.URLUtil
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import clipto.AppContext
import clipto.analytics.Analytics
import clipto.common.extensions.findFirstEmail
import clipto.common.extensions.findFirstPhone
import clipto.common.extensions.findFirstWebUrl
import clipto.common.extensions.safeIntent
import clipto.common.misc.IntentUtils
import clipto.extensions.*
import clipto.presentation.preview.link.LinkPreview
import com.wb.clipboard.R
import java.lang.ref.WeakReference
import java.util.*

enum class FastAction(
    val id: Int,
    @DrawableRes private val iconRes: Int,
    @DrawableRes private val iconRoundedRes: Int,
    @StringRes val titleRes: Int,
    var visible: Boolean = false,
    var order: Int = id,
    val requiredPreRendering: Boolean = false
) {

    COMPOSE_EMAIL(
        1,
        R.drawable.fast_action_compose_email,
        R.drawable.fast_action_compose_email_rounded,
        R.string.fast_actions_compose_email,
        true
    ) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            AppContext.get().onGetClipText(clip) {
                IntentUtils.email(textView.context, null, clip.title, it)
                callback.invoke()
            }
        }
    },

    COMPOSE_SMS(
        2,
        R.drawable.fast_action_compose_sms,
        R.drawable.fast_action_compose_sms_rounded,
        R.string.fast_actions_compose_sms,
        true
    ) {

        private var isAvailable: Boolean? = null

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            AppContext.get().onGetClipText(clip) {
                IntentUtils.sms(textView.context, it)
                callback.invoke()
            }
        }

        override fun isAvailable(context: Context): Boolean {
            return isAvailable
                ?: run {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("smsto:")
                    isAvailable = intent.resolveActivity(context.packageManager) != null
                    isAvailable!!
                }
        }
    },

    TWITTER_POST(3, R.drawable.fast_action_compose_tweet, R.drawable.fast_action_compose_tweet_rounded, R.string.fast_actions_twitter_post, true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            AppContext.get().onGetClipText(clip) {
                val text = it
                val url =
                    if (isNetworkUrl(text)) {
                        "https://twitter.com/intent/tweet?url=$text"
                    } else {
                        "https://twitter.com/intent/tweet?text=$text"
                    }
                IntentUtils.open(textView.context, url)
                callback.invoke()
            }
        }
    },

    WEB_SEARCH(4, R.drawable.fast_action_web_search, R.drawable.fast_action_web_search_rounded, R.string.fast_actions_web_search, true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val context = textView.context
            val webSearch = Intent(Intent.ACTION_WEB_SEARCH)
            webSearch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            webSearch.putExtra(SearchManager.QUERY, clip.text)

            val viewIntent = Intent(Intent.ACTION_VIEW)
            viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewIntent.data = Uri.parse("https://www.google.com/search?q=${clip.text}")

            val chooser = Intent.createChooser(webSearch, null)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))

            context.safeIntent(chooser)

            callback.invoke()
        }
    },

    DUCKDUCKGO_SEARCH(29, R.drawable.fast_action_duckduckgo, R.drawable.fast_action_duckduckgo_rounded, R.string.fast_actions_duckduckgo_search) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val url = "https://duckduckgo.com/?q=${clip.text}"
            IntentUtils.open(textView.context, url)
            callback.invoke()
        }
    },

    STACKOVERFLOW_SEARCH(30, R.drawable.fast_action_stackoverflow, R.drawable.fast_action_stackoverflow_rounded, R.string.fast_actions_stackoverflow_search) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val url = "https://stackoverflow.com/search?q=${clip.text}"
            IntentUtils.open(textView.context, url)
            callback.invoke()
        }
    },

    REDDIT_SEARCH(31, R.drawable.fast_action_reddit_search, R.drawable.fast_action_reddit_search_rounded, R.string.fast_actions_reddit_search) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val url = "https://www.reddit.com/search/?q=${clip.text}"
            IntentUtils.open(textView.context, url)
            callback.invoke()
        }
    },

    WIKI_SEARCH(5, R.drawable.fast_action_wiki_search, R.drawable.fast_action_wiki_search_rounded, R.string.fast_actions_wiki_search) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val text = clip.text
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra("query", text)
            intent.component = ComponentName(
                "org.wikipedia",
                "org.wikipedia.search.SearchActivity"
            )

            if (!textView.context.safeIntent(intent, false)) {
                val baseUrl = "https://wikipedia.org"
                IntentUtils.open(textView.context, "$baseUrl/wiki/Special:Search?search=${clip.text}")
            }

            callback.invoke()
        }
    },

    YOUTUBE_SEARCH(6, R.drawable.fast_action_youtube_search, R.drawable.fast_action_youtube_search_rounded, R.string.fast_actions_youtube_search) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val url = "https://www.youtube.com/results?search_query=${clip.text}"
            IntentUtils.open(textView.context, url)
            callback.invoke()
        }
    },

    GOOGLE_MAPS_SEARCH(35, R.drawable.fast_action_google_maps_search, R.drawable.fast_action_google_maps_search_rounded, R.string.fast_actions_google_maps_search) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val text = clip.text
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(text)}"))
            intent.setPackage("com.google.android.apps.maps")
            if (!textView.context.safeIntent(intent, false)) {
                val url = "https://www.google.com/maps/search/?api=1&query=${text}"
                IntentUtils.open(textView.context, url)
            }
            callback.invoke()
        }
    },

    GOOGLE_TRANSLATE(7, R.drawable.fast_action_google_translate, R.drawable.fast_action_google_translate_rounded, R.string.fast_actions_google_translate) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val language = AppContext.get().appState.getLanguage()
            val text = clip.text

            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra("key_text_input", text)
            intent.putExtra("key_text_output", "")
            intent.putExtra("key_language_from", "auto")
            intent.putExtra("key_language_to", language)
            intent.putExtra("key_suggest_translation", "")
            intent.putExtra("key_from_floating_window", true)
            intent.component = ComponentName(
                "com.google.android.apps.translate",
                "com.google.android.apps.translate.TranslateActivity"
            )
            if (!textView.context.safeIntent(intent, false)) {
                val url =
                    if (isNetworkUrl(text)) {
                        "http://translate.google.com/translate?js=n&sl=auto&tl=$language&u=$text"
                    } else {
                        "http://translate.google.com/#view=home&op=translate&sl=auto&tl=$language&text=$text"
                    }
                IntentUtils.open(textView.context, url)
            }
            callback.invoke()
        }
    },

    TEXT_TO_SPEECH(28, R.drawable.fast_action_tts, R.drawable.fast_action_tts_rounded, R.string.fast_actions_tts) {

        private var ttsRef = WeakReference<TextToSpeech>(null)
        private var ttsInProgress = false

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.text?.let { text ->
                val appContext = AppContext.get()
                val maxLength = TextToSpeech.getMaxSpeechInputLength()
                if (text.length > maxLength) {
                    appContext.showToast(appContext.string(R.string.fast_actions_tts_max_length, maxLength))
                    callback.invoke()
                    return
                }
                val appState = appContext.appState
                appState.getTextLanguage(text)
                    .observeOn(appState.getViewScheduler())
                    .subscribe(
                        { doProcess(text, Locale(it), callback) },
                        { doProcess(text, appState.getSelectedLocale(), callback) }
                    )
            } ?: callback.invoke()
        }

        private fun doProcess(text: String, lang: Locale, callback: () -> Unit) {
            ttsRef.get()?.shutdown()
            if (ttsInProgress) {
                updateProgress(false)
                ttsRef.clear()
                return
            }
            val appContext = AppContext.get()
            val appState = appContext.appState
            ttsRef = WeakReference(TextToSpeech(appContext.app) {
                val tts = ttsRef.get()
                if (tts != null && it == TextToSpeech.SUCCESS) {
                    try {
                        val id = UUID.randomUUID().toString()
                        val language = tts.availableLanguages.find { it.language == lang.language }
                        language?.let { tts.language = it }
                        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onDone(utteranceId: String?) {
                                tts.shutdown()
                                callback.invoke()
                                updateProgress(false)
                            }

                            override fun onError(utteranceId: String?) {
                                tts.shutdown()
                                callback.invoke()
                                updateProgress(false)
                            }

                            override fun onStart(utteranceId: String?) {
                                updateProgress(true)
                            }
                        })
                        appState.setLoadingState()
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
                    } catch (e: Exception) {
                        appContext.showToast(appContext.string(R.string.fast_actions_error, e.localizedMessage))
                    }
                } else {
                    appContext.showToast(appContext.string(R.string.fast_actions_error, it))
                    tts?.shutdown()
                    ttsRef.clear()
                    callback.invoke()
                }
            })
        }

        private fun updateProgress(inProgress: Boolean = false) {
            val appContext = AppContext.get()
            val appState = appContext.appState
            ttsInProgress = inProgress
            if (appContext.getSettings().notificationStyle == NotificationStyle.ACTIONS) {
                appContext.clipboardState.refreshClipboard()
            }
            appState.setLoadedState()
            appContext.appState.requestFastActionsRefresh()
        }

        override fun isSmartAction(): Boolean = ttsInProgress
        override fun getIconRoundedRes(): Int = if (ttsInProgress) R.drawable.fast_action_tts_rounded_active else R.drawable.fast_action_tts_rounded
    },

    SHARE(8, R.drawable.fast_action_share, R.drawable.fast_action_share_rounded, R.string.fast_actions_share) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            AppContext.get().onShare(listOf(clip), clearSelection = false)
            callback.invoke()
        }
    },

    SEND_AS_QRCODE(9, R.drawable.fast_action_qrcode, R.drawable.fast_action_qrcode_rounded, R.string.fast_actions_send_as_qrcode, true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val currentType = clip.textType.toExt()
            val qrCodeType = TextTypeExt.QRCODE
            qrCodeType.apply(textView, clip.text)
            qrCodeType.sendAsPdfOrJpeg(textView, clip, callback)
            currentType.apply(textView, clip.text)
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            if (text.length <= 2953) {
                return super.toClipAction(context, text)
            }
            return null
        }
    },

    INSERT_CONTACT(11, R.drawable.fast_action_insert_contact, R.drawable.fast_action_insert_contact_rounded, R.string.fast_actions_insert_contact) {

        private var isAvailable: Boolean? = null

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.text?.let {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                    clip.title?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                    if (Patterns.PHONE.matcher(it).matches()) {
                        putExtra(ContactsContract.Intents.Insert.PHONE, it)
                    } else if (Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                        putExtra(ContactsContract.Intents.Insert.EMAIL, it)
                    } else {
                        putExtra(ContactsContract.Intents.Insert.NOTES, it)
                    }
                }
                textView.context.safeIntent(intent)
            }
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            return isAvailable ?: run {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                }
                isAvailable = intent.resolveActivity(context.packageManager) != null
                isAvailable!!
            }
        }
    },

    CALENDAR_EVENT(12, R.drawable.fast_action_calendar_event, R.drawable.fast_action_calendar_event_rounded, R.string.fast_actions_calendar_event) {

        private var isAvailable: Boolean? = null

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, clip.title)
                putExtra(CalendarContract.Events.DESCRIPTION, clip.text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            textView.context.safeIntent(intent)
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            return isAvailable ?: run {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                }
                isAvailable = intent.resolveActivity(context.packageManager) != null
                isAvailable!!
            }
        }
    },

    SEND_AS_PDF(13, R.drawable.fast_action_send_as_file, R.drawable.fast_action_send_as_file_rounded, R.string.fast_actions_send_as_pdf_file, requiredPreRendering = true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().sendAsPdfOrJpeg(textView, clip, callback)
        }
    },

    EXPORT_TO_PDF(14, R.drawable.fast_action_export, R.drawable.fast_action_export_rounded, R.string.fast_actions_export_to_file, requiredPreRendering = true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().exportToPdfOrJpeg(textView, clip, callback)
        }
    },

    SEND_AS_JPEG(15, R.drawable.fast_action_send_as_file, R.drawable.fast_action_send_as_file_rounded, R.string.fast_actions_send_as_file, requiredPreRendering = true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().sendAsPdfOrJpeg(textView, clip, callback)
        }
    },

    EXPORT_TO_JPEG(16, R.drawable.fast_action_export, R.drawable.fast_action_export_rounded, R.string.fast_actions_export_to_file, requiredPreRendering = true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().exportToPdfOrJpeg(textView, clip, callback)
        }
    },

    SEND_AS_TXT(17, R.drawable.fast_action_send_as_file, R.drawable.fast_action_send_as_file_rounded, R.string.fast_actions_send_as_txt_file) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().sendAsPlainText(textView, clip, "txt", callback)
        }
    },

    EXPORT_TO_TXT(18, R.drawable.fast_action_export, R.drawable.fast_action_export_rounded, R.string.fast_actions_export_to_file, requiredPreRendering = true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().exportToPlainText(textView, clip, "txt", callback)
        }
    },

    SEND_AS_MD(19, R.drawable.fast_action_send_as_file, R.drawable.fast_action_send_as_file_rounded, R.string.fast_actions_send_as_md_file) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().sendAsPlainText(textView, clip, "md", callback)
        }
    },

    EXPORT_TO_MD(20, R.drawable.fast_action_export, R.drawable.fast_action_export_rounded, R.string.fast_actions_export_to_file, requiredPreRendering = true) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.textType.toExt().exportToPlainText(textView, clip, "md", callback)
        }
    },

    MORE(21, R.drawable.fast_action_more, R.drawable.fast_action_more_rounded, R.string.fast_actions_more) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) = Unit
    },

    SMART_ACTION_COPY(10, R.drawable.fast_action_copy, R.drawable.fast_action_copy_rounded, R.string.fast_actions_copy) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            if (clip.isNew()) clip.tracked = true
            AppContext.get().onCopy(clip, clearSelection = false) { callback.invoke() }
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return ClipAction(
                text = text,
                action = this,
                smartAction = true,
                label = context.getString(titleRes),
                iconRes = getIconRoundedRes()
            )
        }
    },

    SMART_ACTION_CLEAR_COPY(36, R.drawable.action_cancel, R.drawable.action_cancel, R.string.fast_actions_copy_clear) {
        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.isActive = false
            AppContext.get().clipboardState.clearClipboard()
            callback.invoke()
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return ClipAction(
                text = text,
                action = this,
                smartAction = true,
                label = context.getString(titleRes),
                iconRes = getIconRoundedRes()
            )
        }
    },

    SMART_DIAL_NUMBER(
        22,
        R.drawable.smart_action_dial_number,
        R.drawable.fast_action_dial_number_rounded,
        R.string.fast_actions_dial_number
    ) {

        private var isAvailable: Boolean? = null

        override fun isSmartAction() = true

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstPhone()?.let { IntentUtils.call(textView.context, it) }
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            if (isAvailable == null) {
                isAvailable = Intent(Intent.ACTION_DIAL).resolveActivity(context.packageManager) != null
            }
            return isAvailable!!
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstPhone()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = true,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    SMART_SEND_SMS(
        26,
        R.drawable.smart_action_send_sms,
        R.drawable.fast_action_send_sms_rounded,
        R.string.fast_actions_send_message,
        true
    ) {

        private var isAvailable: Boolean? = null

        override fun isSmartAction() = true

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstPhone()?.let { IntentUtils.smsTo(textView.context, it) }
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            return (isAvailable
                ?: run {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("smsto:")
                    isAvailable = intent.resolveActivity(context.packageManager) != null
                    isAvailable!!
                })
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstPhone()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = true,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    SMART_INSERT_CONTACT_PHONE(
        23,
        R.drawable.smart_action_contact_phone,
        R.drawable.fast_action_contact_phone_rounded,
        R.string.fast_actions_insert_contact_phone
    ) {

        private var isAvailable: Boolean? = null

        override fun isSmartAction() = true

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstPhone()?.let {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                    clip.title?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                    putExtra(ContactsContract.Intents.Insert.PHONE, it)
                }
                textView.context.safeIntent(intent)
            }
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            if (isAvailable == null) {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                }
                isAvailable = intent.resolveActivity(context.packageManager) != null
            }
            return isAvailable!!
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstPhone()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = true,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    SMART_SEND_EMAIL(
        27,
        R.drawable.smart_action_send_email,
        R.drawable.fast_action_send_email_rounded,
        R.string.fast_actions_send_message,
        true
    ) {

        override fun isSmartAction() = true

        private var isAvailable: Boolean? = null

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstEmail()?.let { IntentUtils.email(textView.context, it) }
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            return (isAvailable
                ?: run {
                    val intent = Intent(Intent.ACTION_SENDTO)
                    intent.data = Uri.parse("mailto:")
                    isAvailable = intent.resolveActivity(context.packageManager) != null
                    isAvailable!!
                })
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstEmail()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = true,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    SMART_INSERT_CONTACT_EMAIL(
        24,
        R.drawable.smart_action_contact_email,
        R.drawable.fast_action_contact_email_rounded,
        R.string.fast_actions_insert_contact_email
    ) {

        private var isAvailable: Boolean? = null

        override fun isSmartAction() = true

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstEmail()?.let {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                    clip.title?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
                    putExtra(ContactsContract.Intents.Insert.EMAIL, it)
                }
                textView.context.safeIntent(intent)
            }
            callback.invoke()
        }

        override fun isAvailable(context: Context): Boolean {
            if (isAvailable == null) {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.Contacts.CONTENT_TYPE
                }
                isAvailable = intent.resolveActivity(context.packageManager) != null
            }
            return isAvailable!!
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstEmail()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = true,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    SMART_OPEN_WEB_URL(
        25,
        R.drawable.smart_action_open_url,
        R.drawable.fast_action_open_url_rounded,
        R.string.fast_actions_open_url
    ) {

        override fun isSmartAction() = true

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstWebUrl()?.let { IntentUtils.open(textView.context, it) }
            callback.invoke()
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstWebUrl()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = true,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    PLAY_URL(
        33,
        R.drawable.fast_action_play_url,
        R.drawable.fast_action_play_url_rounded,
        R.string.fast_actions_play_url
    ) {

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstWebUrl()?.let {
                val preview = LinkPreview(url = it, title = clip.title)
                AppContext.get().onShowPreview(preview)
            }
            callback.invoke()
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            return text.findFirstWebUrl()?.let {
                ClipAction(
                    text = text,
                    action = this,
                    smartAction = false,
                    label = it.takeIf { it.length != text.length }
                        ?: context.getString(titleRes),
                    iconRes = getIconRoundedRes()
                )
            }
        }
    },

    SMART_SHORT_LINK(
        34,
        R.drawable.smart_action_short_link,
        R.drawable.fast_action_short_link_rounded,
        R.string.fast_actions_short_link
    ) {

        override fun isSmartAction() = true

        override fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit) {
            clip.toLinkifiedSpannable().findFirstWebUrl()?.let { link ->
                val appContext = AppContext.get()
                val appState = appContext.appState
                appContext.withInternet({
                    appState.setLoadingState()
                    appContext.onGetUrlShortLink(
                        link,
                        onSuccess = {
                            IntentUtils.share(appContext.app, it)
                            callback.invoke()
                            appState.setLoadedState()
                        },
                        onFailure = {
                            appState.showToast(it.message ?: appContext.string(R.string.error_unexpected))
                            callback.invoke()
                            appState.setLoadedState()
                        }
                    )
                })
            }
        }

        override fun toClipAction(context: Context, text: Spannable): ClipAction? {
            val appConfig = AppContext.get().appConfig
            return text
                .takeIf { appConfig.smartActionShortenLinkEnabled() }
                ?.findFirstWebUrl()
                ?.takeIf { it.length >= appConfig.smartActionShortenLinkMinimumLength() }
                ?.let {
                    ClipAction(
                        text = text,
                        action = this,
                        smartAction = true,
                        label = it.takeIf { it.length != text.length }
                            ?: context.getString(titleRes),
                        iconRes = getIconRoundedRes()
                    )
                }
        }
    },

    ;

    fun process(clip: Clip, textView: TextView, callback: () -> Unit = {}) {
        doProcess(clip, textView, callback)
        Analytics.onFastAction(this, clip)
    }

    open fun isSmartAction() = false

    open fun getIconRes(): Int = iconRes

    open fun getIconRoundedRes(): Int = iconRoundedRes

    protected open fun isAvailable(context: Context) = true

    protected fun isNetworkUrl(text: String?): Boolean = URLUtil.isNetworkUrl(text)

    protected abstract fun doProcess(clip: Clip, textView: TextView, callback: () -> Unit)

    fun toMeta(): FastActionMeta = FastActionMeta(id, order, visible)

    protected open fun toClipAction(context: Context, text: Spannable): ClipAction? {
        return ClipAction(
            text = text,
            action = this,
            label = context.getString(titleRes),
            iconRes = getIconRoundedRes()
        )
    }

    companion object {

        private val actions = listOf(
            COMPOSE_EMAIL,
            COMPOSE_SMS,
            TWITTER_POST,

            CALENDAR_EVENT,
            INSERT_CONTACT,

            WEB_SEARCH,
            WIKI_SEARCH,
            YOUTUBE_SEARCH,
            DUCKDUCKGO_SEARCH,
            STACKOVERFLOW_SEARCH,
            REDDIT_SEARCH,
            GOOGLE_MAPS_SEARCH,
            GOOGLE_TRANSLATE,

            TEXT_TO_SPEECH,
            PLAY_URL,

            SHARE,
//                COPY,

            SEND_AS_QRCODE,
            SEND_AS_TXT,
            SEND_AS_PDF,
            SEND_AS_MD
        ).also {
            it.forEachIndexed { index, action -> action.order = index }
        }

        private val smartActions = listOf(
            SMART_DIAL_NUMBER,
            SMART_SEND_SMS,
            SMART_INSERT_CONTACT_PHONE,
            SMART_SEND_EMAIL,
            SMART_INSERT_CONTACT_EMAIL,
            SMART_OPEN_WEB_URL,
            SMART_SHORT_LINK
        )

        private val itemsArray = SparseArray<FastAction>().also { array ->
            actions.forEach { array.put(it.id, it) }
        }

        fun getClipActions(context: Context, spannable: Spannable): List<ClipAction> {
            return smartActions
                .plus(actions.filter { it.visible }.sortedBy { it.order })
                .filter { it.isAvailable(context) }
                .plus(MORE)
                .mapNotNull { it.toClipAction(context, spannable) }
        }

        fun getAllClipActions(context: Context, clip: Clip): List<ClipAction> {
            val allActions = mutableListOf<FastAction>()
            if (clip.isActive) {
                allActions.add(SMART_ACTION_CLEAR_COPY)
            } else {
                allActions.add(SMART_ACTION_COPY)
            }
            allActions.addAll(smartActions)
            allActions.addAll(actions.sortedBy { it.order }.sortedByDescending { it.visible })
            val text = clip.toLinkifiedSpannable()
            return allActions
                .filter { it.isAvailable(context) }
                .mapNotNull { it.toClipAction(context, text) }
        }

        fun getMoreActions(): List<FastAction> = actions.sortedBy { it.order }

        fun getAllActions(): List<FastAction> = actions

        fun valueOf(id: Int): FastAction? = itemsArray.get(id)

        fun findById(id: Int): FastAction? = values().find { it.id == id }
    }

    data class ClipAction(
        val smartAction: Boolean = false,
        val action: FastAction,
        val label: CharSequence,
        val text: Spannable,
        val iconRes: Int
    ) {
        fun getIconColor(context: Context): Int {
            if (action == SMART_ACTION_CLEAR_COPY) {
                return context.getColorNegative()
            }
            if (smartAction) {
                return context.getColorPositive()
            }
            if (action.visible) {
                return context.getTextColorAccent()
            }
            return context.getTextColorSecondary()
        }
    }

}