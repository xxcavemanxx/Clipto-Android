package clipto.extensions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import android.provider.Browser
import android.text.*
import android.text.method.ArrowKeyMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.text.util.Linkify
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import androidx.core.view.doOnNextLayout
import clipto.AppContainer
import clipto.AppContext
import clipto.analytics.Analytics
import clipto.common.extensions.*
import clipto.common.logging.L
import clipto.common.misc.*
import clipto.common.presentation.text.*
import clipto.domain.Clip
import clipto.domain.TextType
import clipto.domain.Theme
import clipto.dynamic.DynamicField
import clipto.dynamic.DynamicValueConfig
import clipto.extensions.html.CodeTagHandler
import clipto.extensions.md.*
import clipto.presentation.clip.add.AddClipFragment
import clipto.presentation.clip.add.data.AddClipRequest
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.wb.clipboard.BuildConfig
import com.wb.clipboard.R
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.reactivex.disposables.Disposable
import org.commonmark.node.BulletList
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.greenrobot.essentials.io.FileUtils
import org.greenrobot.essentials.io.IoUtils
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.regex.Pattern


fun TextType.toExt() = when (this) {
    TextType.TEXT_PLAIN -> TextTypeExt.TEXT_PLAIN
    TextType.LINE_CLICKABLE -> TextTypeExt.LINE_CLICKABLE
    TextType.WORD_CLICKABLE -> TextTypeExt.WORD_CLICKABLE
    TextType.MARKDOWN -> TextTypeExt.MARKDOWN
    TextType.LINK -> TextTypeExt.LINK
    TextType.HTML -> TextTypeExt.HTML
    TextType.QRCODE -> TextTypeExt.QRCODE
}

enum class TextTypeExt(
    val type: TextType,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int
) {

    TEXT_PLAIN(
        TextType.TEXT_PLAIN,
        R.string.clip_text_type_text_plain,
        R.drawable.clip_text_type_text
    ) {
        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            textView.autoLinkMask = 0
            textView.movementMethod = EnhancedMovementMethod
            textView.text = withHighlight(textView.context, text)
            callback.invoke()
        }
    },

    LINK(
        TextType.LINK,
        R.string.clip_text_type_link,
        R.drawable.clip_text_type_link
    ) {

        private val reg = Pattern.compile("${Patterns.WEB_URL.pattern()}|${Patterns.EMAIL_ADDRESS.pattern()}|${Patterns.PHONE.pattern()}").toRegex()
        private val imageWidth = Units.displayMetrics.widthPixels - Units.DP.toPx(48f).toInt()
        private val imageHeight = imageWidth

        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            val appContext = AppContext.get()
            textView.movementMethod = withClickActions()
            val appliedText = withHighlight(textView.context, text)
            if (appContext.linkPreviewState.isPreviewEnabled()) {
                val maxWidth =
                    if (textView.tag == LINK_NOT_CLICKABLE) {
                        imageWidth - Units.DP.toPx(60f).toInt()
                    } else {
                        imageWidth
                    }
                val width = minOf((textView.width - Units.DP.toPx(24f).toInt()).takeIf { it > 0 } ?: maxWidth, maxWidth)
                appContext.linkPreviewFactory.linkify(textView, appliedText, width, imageHeight)
            } else {
                textView.setSpannableFactory(SimpleSpanBuilder.FACTORY)
                textView.setText(SpannableStringBuilder(appliedText), TextView.BufferType.SPANNABLE)
                val newText = textView.text
                if (newText is SpannableStringBuilder) {
                    newText.withUrls()
                }
            }
            callback.invoke()
        }

        override fun isValid(text: String): Boolean {
            return text.contains(reg)
        }
    },

    MARKDOWN(
        TextType.MARKDOWN,
        R.string.clip_text_type_markdown,
        R.drawable.clip_text_type_markdown
    ) {

        var markwon: Markwon? = null
        var editTextRef: EditText? = null
        var changeWatcher: TextWatcher? = null

        private fun markwon(textView: TextView): Markwon {
            val ref = markwon
            if (ref != null) {
                return ref
            }
            val state = AppContext.get().appConfig
            val context = textView.context
            markwon = Markwon.builder(context)
                .usePlugin(TablePlugin.create(context))
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun processMarkdown(markdown: String): String {
                        var value = markdown
                        if (state.markdownStrikethroughNormalizationActivated()) {
                            value = value.replace("~", "~~").replace("~~~~", "~~")
                        }
                        if (state.markdownBulletListNormalizationActivated()) {
                            value = value.replace("\n\n-", "\n-")
                        }
                        return value
                    }

                    private fun listLevel(node: Node): Int {
                        var level = 0
                        var parent = node.parent
                        while (parent != null) {
                            if (parent is ListItem) {
                                level += 1
                            }
                            parent = parent.parent
                        }
                        return level
                    }

                    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                        if (state.markdownBulletListNormalizationActivated()) {
                            builder.on(BulletList::class.java) { visitor, node ->
                                visitor.blockStart(node)
                                val length = visitor.length()
                                visitor.visitChildren(node)
                                visitor.setSpansForNodeOptional<Node>(node, length)
                                if (node.parent is ListItem && listLevel(node.parent) == 0) {
                                    visitor.ensureNewLine()
                                    visitor.forceNewLine()
                                }
                                visitor.blockEnd(node)
                            }
                            builder.on(OrderedList::class.java) { visitor, node ->
                                visitor.blockStart(node)
                                val length = visitor.length()
                                visitor.visitChildren(node)
                                visitor.setSpansForNodeOptional<Node>(node, length)
                                if (node.parent is ListItem && listLevel(node.parent) == 0) {
                                    visitor.ensureNewLine()
                                    visitor.forceNewLine()
                                }
                                visitor.blockEnd(node)
                            }
                        }
                    }
                })
                .usePlugin(JLatexMathPlugin.create(64f) {
                    it
                        .blocksEnabled(true)
                        .blocksLegacy(false)
                        .inlinesEnabled(true)
                        .theme().backgroundProvider {
                            ColorDrawable(ThemeUtils.getColor(context, R.attr.myBackgroundHighlight))
                        }
                })
                .usePlugin(LinkifyPlugin.create())
                .usePlugin(GlideImagesPlugin.create(context))
                .usePlugin(HtmlPlugin.create().apply {
                    addHandler(CodeTagHandler())
                })
                .usePlugin(MarkwonInlineParserPlugin.create())
                .build()
            return markwon!!
        }

        override fun applyEditor(editText: EditText, acceptor: () -> Boolean) {
            if (!AppContext.get().appConfig.markdownEditorActivated()) {
                return
            }
            if (changeWatcher == null || editTextRef != editText) {
                editTextRef = editText
                val mw = markwon(editText)
                val editor = MarkwonEditor.builder(mw)
                    .useEditHandler(EmphasisSpanEditHandler())
                    .useEditHandler(StrongEmphasisSpanEditHandler())
                    .useEditHandler(StrikethroughEditHandler())
                    .useEditHandler(HeadingSpanEditHandler())
                    .useEditHandler(BulletListItemEditHandler())
                    .useEditHandler(OrderedListItemEditHandler())
                    .useEditHandler(CodeEditHandler())
                    .useEditHandler(BlockQuoteEditHandler())
                    .useEditHandler(LinkEditHandler())
                    .build()

                changeWatcher = MarkwonEditorTextWatcher.withPreRender(
                    editor,
                    textTypeExecutor,
                    editText
                )
                editText.addTextChangedListener(changeWatcher)
            }
            if (acceptor.invoke()) {
                changeWatcher?.afterTextChanged(editText.text)
            }
        }

        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            if (markwon == null) {
                markwon = markwon(textView)
            }
            applyText(textView, text, callback)
        }

        private fun applyText(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            val context = textView.context
            val appContext = AppContext.get()
            withFileStoragePermissions(context, text) {
                withSpannedText(textView, text,
                    { withHighlight(appContext.app, markwon!!.toMarkdown(text.toString())) },
                    {
                        if (it is Spanned) {
                            markwon!!.setParsedMarkdown(textView, it)
                            callback.invoke()
                        }
                    })
            }
        }
    },

    HTML(
        TextType.HTML,
        R.string.clip_text_type_html,
        R.drawable.clip_text_type_html
    ) {

        var markwon: Markwon? = null

        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            val context = textView.context
            withFileStoragePermissions(context, text) {
                processText(textView, text, callback)
            }
        }

        private fun processText(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            val context = textView.context
            withSpannedText(textView, text,
                {
                    withHighlight(
                        context,
                        text?.let {
                            if (markwon == null) {
                                markwon = Markwon.builder(context)
                                    .usePlugin(GlideImagesPlugin.create(context))
                                    .usePlugin(HtmlPlugin.create().apply {
                                        addHandler(CodeTagHandler())
                                    })
                                    .build()
                            }
                            markwon!!.toMarkdown(it.toString())
                        })
                },
                {
                    if (it is Spanned) {
                        markwon!!.setParsedMarkdown(textView, it)
                        callback.invoke()
                    }
                })
        }

        val tagEnd = "\\</\\w+\\>"
        val htmlEntity = "&[a-zA-Z][a-zA-Z0-9]+;"
        val tagStart = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>"
        val tagSelfClosing = "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>"
        val htmlPattern = Pattern.compile("($tagStart.*$tagEnd)|($tagSelfClosing)|($htmlEntity)", Pattern.DOTALL)
        override fun isValid(text: String): Boolean = htmlPattern.matcher(text).find()
    },


    LINE_CLICKABLE(
        TextType.LINE_CLICKABLE,
        R.string.clip_text_type_line_clickable,
        R.drawable.clip_text_type_line_clickable
    ) {
        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            textView.autoLinkMask = 0
            val clickableText = text?.let {
                val spanBuilder = SimpleSpanBuilder(it.length)
                var isFirst = true
                it.lineSequence().forEach { line ->
                    if (!isFirst) {
                        spanBuilder.append("\n")
                    } else {
                        isFirst = false
                    }
                    if (line.isNotBlank()) {
                        spanBuilder.append(line, SimpleClickableSpan())
                    } else {
                        spanBuilder.append(line)
                    }
                }
                spanBuilder.build()
            }
            textView.text = withHighlight(textView.context, clickableText)
            BetterLinkMovementMethodExt()
                .linkify(textView)
                .setOnLinkClickListener { _, url ->
                    AppContext.get().onCopy(url)
                    true
                }
                .setOnLinkLongClickListener { _, url ->
                    val context = textView.context
                    Analytics.screenClipLink()
                    AddClipFragment.show(
                        context, AddClipRequest(
                            text = url
                        )
                    )
                    true
                }
            callback.invoke()
        }
    },

    WORD_CLICKABLE(
        TextType.WORD_CLICKABLE,
        R.string.clip_text_type_word_clickable,
        R.drawable.clip_text_type_word_clickable
    ) {
        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            textView.autoLinkMask = 0
            val clickableText = text?.let {
                val spanBuilder = SimpleSpanBuilder(it.length)
                var isFirst = true
                it.lineSequence().forEachIndexed { index, line ->
                    if (!isFirst) {
                        spanBuilder.append("\n")
                    } else {
                        isFirst = false
                    }
                    if (line.isNotBlank()) {
                        var isFirstToken = true
                        line.split(" ").forEach { token ->
                            if (!isFirstToken) {
                                spanBuilder.append(" ")
                            } else {
                                isFirstToken = false
                            }
                            if (token.isNotBlank()) {
                                spanBuilder.append(token, SimpleClickableSpan())
                            } else {
                                spanBuilder.append(token)
                            }
                        }
                    } else {
                        spanBuilder.append(line)
                    }
                }
                spanBuilder.build()
            }
            textView.text = withHighlight(textView.context, clickableText)
            BetterLinkMovementMethodExt()
                .linkify(textView)
                .setOnLinkClickListener { _, url ->
                    AppContext.get().onCopy(url)
                    true
                }
                .setOnLinkLongClickListener { _, url ->
                    val context = textView.context
                    Analytics.screenClipLink()
                    AddClipFragment.show(
                        context, AddClipRequest(
                            text = url
                        )
                    )
                    true
                }
            callback.invoke()
        }
    },

    QRCODE(
        TextType.QRCODE,
        R.string.clip_text_type_qrcode,
        R.drawable.clip_text_type_qrcode
    ) {

        override fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit) {
            textView.autoLinkMask = 0
            textView.movementMethod = ArrowKeyMovementMethod.getInstance()
            text?.let { txt ->
                val context = textView.context
                val blackColor = context.getTextColorPrimary()
                val whiteColor =
                    if (AppContext.get().appState.isLastActivityContextActions()) {
                        context.getColorContext()
                    } else {
                        context.getColorPrimaryInverse()
                    }
                val width = AndroidUtils.getPreferredDisplaySize(textView.context).x - margins
                val qrCode = encodeAsBitmap(width, txt.toString().takeIf { it.isNotEmpty() } ?: " ", blackColor, whiteColor)
                if (qrCode != null) {
                    val string = SpannableString("-")
                    string.setSpan(
                        ImageSpan(textView.context, qrCode, ImageSpan.ALIGN_BASELINE),
                        0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                    textView.text = string
                } else {
                    textView.text = txt
                }
            }
            callback.invoke()
        }

        override fun doExportToPdfOrJpeg(textView: TextView, clip: Clip, callback: () -> Unit) {
            val appContext = AppContext.get()
            val activity = textView.context.findActivity()
            if (activity == null) {
                callback.invoke()
                return
            }
            val width = AndroidUtils.getPreferredDisplaySize(activity).y
            val fileName = createFileName(activity, clip, "jpg")
            appContext.onBackground {
                try {
                    appContext.setLoadingState()
                    val bitmap = encodeAsBitmap(width, clip.text.notNull())
                    if (bitmap != null) {
                        val request = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_TITLE, fileName)
                            type = "image/jpeg"
                        }
                        appContext.onMain {
                            val contentResolver = activity.contentResolver
                            activity.withResult(request) { _, intent ->
                                intent?.data?.let { uri ->
                                    appContext.onBackground {
                                        try {
                                            contentResolver.openFileDescriptor(uri, "w")?.use {
                                                FileOutputStream(it.fileDescriptor).use { stream ->
                                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 0, stream)
                                                    bitmap.recycle()
                                                }
                                            }
                                            appContext.showToast(appContext.string(R.string.fast_actions_export_to_file_success, fileName))
                                            callback.invoke()
                                        } catch (e: Exception) {
                                            appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                                            Analytics.onError("qr_export_to_file_error", e)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Analytics.onError("qr_export_to_file_error", e)
                    appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                } finally {
                    appContext.setLoadedState()
                }
            }
        }

        override fun doSendAsPdfOrJpeg(textView: TextView, clip: Clip, callback: () -> Unit) {
            val context = textView.context
            val width = AndroidUtils.getPreferredDisplaySize(context).y
            val appContext = AppContext.get()
            appContext.onBackground {
                try {
                    appContext.setLoadingState()
                    encodeAsBitmap(width, clip.text.notNull())?.use { bitmap ->
                        val file = createFile(context, clip, "jpg")
                        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 0, it) }
                        val fileUri = createFileUri(context, file)
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                        intent.type = "image/jpeg"
                        appContext.onMain {
                            context.safeIntent(intent)
                            callback.invoke()
                        }
                    }
                } catch (e: Exception) {
                    Analytics.onError("qr_send_as_file_error", e)
                    appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                } finally {
                    appContext.setLoadedState()
                }
            }
        }

        private val margins = Units.DP.toPx(32f).toInt()
        private val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        private val format = BarcodeFormat.QR_CODE
        private val encoder = MultiFormatWriter()

        private fun encodeAsBitmap(dimension: Int, text: String, colorBlack: Int = Color.BLACK, colorWhite: Int = Color.WHITE): Bitmap? {
            try {
                val result = encoder.encode(text, format, dimension, dimension, hints)
                val width = result.width
                val height = result.height
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    val offset = y * width
                    for (x in 0 until width) {
                        pixels[offset + x] = if (result.get(x, y)) colorBlack else colorWhite
                    }
                }
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                return bitmap
            } catch (e: Exception) {
                val appContext = AppContext.get()
                appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                Analytics.onError("qr_encode_error", e)
                return null
            }
        }
    };

    fun apply(
        textView: TextView?,
        text: CharSequence?,
        skipDynamicFieldsRendering: Boolean = false,
        callback: () -> Unit = {}
    ) {
        if (textView == null) return
        try {
            if (textView.isEditable() || text == null) {
                doApply(textView, text, callback)
            } else {
                if (!skipDynamicFieldsRendering && DynamicField.isDynamic(text)) {
                    val appContext = AppContext.get()
                    val fieldId = System.identityHashCode(textView)
                    disposableMap[fieldId].disposeSilently()
                    val actionType = DynamicValueConfig.ActionType.PREVIEW
                    val config = DynamicValueConfig(
                        actionType = actionType,
                        textType = type
                    )
                    val disposable = appContext.dynamicValuesRepository.get().process(text, config)
                        .subscribeOn(appContext.appState.getBackgroundScheduler())
                        .observeOn(appContext.appState.getViewScheduler())
                        .subscribe(
                            {
                                disposableMap.remove(fieldId)
                                doApply(textView, it, callback)
                            },
                            {
                                disposableMap.remove(fieldId)
                                Analytics.onError("error_text_type_apply_dynamic", it)
                                val textPlain = TEXT_PLAIN
                                textPlain.doApply(textView, text, callback)
                                textView.tag = this
                            }
                        )
                    disposableMap[fieldId] = disposable
                } else {
                    doApply(textView, text, callback)
                }
            }
            textView.tag = this
        } catch (th: Throwable) {
            Analytics.onError("error_text_type_apply", th)
            val textPlain = TEXT_PLAIN
            textPlain.doApply(textView, text, callback)
            textView.tag = this
        }
    }

    protected fun withHighlight(context: Context, text: CharSequence?): CharSequence? {
        if (!text.isNullOrBlank()) {
            val constraint = AppContext.get().getFilters().last.textLike
            if (!constraint.isNullOrBlank()) {
                return highlight(context, text, constraint)
            }
        }
        return text
    }

    fun highlight(context: Context, text: CharSequence?, constraint: String?): CharSequence? {
        if (constraint.isNullOrBlank()) return text
        if (text.isNullOrBlank()) return text
        val indexes = arrayListOf<Pair<Int, Int>>()
        constraint.split(",").mapNotNull { it.toNullIfEmpty() }.forEach {
            var index = 0
            while (index != -1) {
                index = text.indexOf(it, index, true)
                if (index != -1) {
                    indexes.add(Pair(index, index + it.length))
                    index++
                }
            }
        }
        if (indexes.isNotEmpty()) {
            val spannable = SpannableString.valueOf(text)
            indexes.forEach { pair ->
                val highlight = BackgroundColorSpan(ThemeUtils.getColor(context, R.attr.colorHighlightBackground))
                spannable.setSpan(highlight, pair.first, pair.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return spannable
        }
        return text
    }

    abstract fun doApply(textView: TextView, text: CharSequence?, callback: () -> Unit)

    open fun applyEditor(editText: EditText, acceptor: () -> Boolean) {}

    fun sendAsPlainText(textView: TextView, clip: Clip, extension: String, callback: () -> Unit) {
        val appContext = AppContext.get()
        val context = textView.context
        appContext.onBackground {
            try {
                appContext.setLoadingState()
                val file = createFile(context, clip, extension)
                FileUtils.writeUtf8(file, clip.text)
                val fileUri = createFileUri(context, file)
                val intent = Intent(Intent.ACTION_SEND)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(Intent.EXTRA_STREAM, fileUri)
                intent.type = when (extension) {
                    "md" -> "text/markdown"
                    else -> "text/plain"
                }
                appContext.onMain {
                    context.safeIntent(intent)
                    callback.invoke()
                }
            } catch (th: Throwable) {
                appContext.showToast(appContext.string(R.string.fast_actions_error, th.message))
                Analytics.onError("error_send_as_${extension}", th)
                callback.invoke()
            } finally {
                appContext.setLoadedState()
            }
        }
    }

    fun exportToPlainText(textView: TextView, clip: Clip, extension: String, callback: () -> Unit) {
        val appContext = AppContext.get()
        val activity = textView.context.findActivity()
        if (activity == null) {
            callback.invoke()
            return
        }
        val fileName = createFileName(activity, clip, extension)
        val request = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
            type = when (extension) {
                "md" -> "text/markdown"
                else -> "text/plain"
            }
        }
        val contentResolver = activity.contentResolver
        activity.withResult(request) { _, intent ->
            intent?.data?.let { uri ->
                appContext.onBackground {
                    try {
                        appContext.setLoadingState()
                        contentResolver.openFileDescriptor(uri, "w")?.use {
                            FileOutputStream(it.fileDescriptor).use { stream ->
                                val writer = stream.writer(Charsets.UTF_8)
                                IoUtils.writeAllCharsAndClose(writer, clip.text)
                            }
                        }
                        appContext.showToast(appContext.string(R.string.fast_actions_export_to_file_success, fileName))
                    } catch (e: Exception) {
                        appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                        Analytics.onError("error_export_to_${extension}", e)
                    } finally {
                        appContext.setLoadedState()
                        callback.invoke()
                    }
                }
            }
        }
    }

    fun sendAsPdfOrJpeg(textView: TextView, clip: Clip, callback: () -> Unit) {
        doSendAsPdfOrJpeg(textView, clip, callback)
    }

    fun exportToPdfOrJpeg(textView: TextView, clip: Clip, callback: () -> Unit) {
        doExportToPdfOrJpeg(textView, clip, callback)
    }

    protected open fun doExportToPdfOrJpeg(textView: TextView, clip: Clip, callback: () -> Unit) {
        val appContext = AppContext.get()
        val activity = textView.context.findActivity()
        if (activity == null) {
            callback.invoke()
            return
        }
        val fileName = createFileName(activity, clip, "pdf")
        withPdf(textView, clip.text) { document ->
            val request = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, fileName)
                type = "application/pdf"
            }
            appContext.onMain {
                val contentResolver = activity.contentResolver
                activity.withResult(request) { _, intent ->
                    intent?.data?.let { uri ->
                        appContext.onBackground {
                            try {
                                contentResolver.openFileDescriptor(uri, "w")?.use {
                                    FileOutputStream(it.fileDescriptor).use { stream ->
                                        document.writeTo(stream)
                                        document.close()
                                    }
                                }
                                appContext.showToast(appContext.string(R.string.fast_actions_export_to_file_success, fileName))
                                callback.invoke()
                            } catch (e: Exception) {
                                appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                                Analytics.onError("error_export_to_pdf", e)
                            }
                        }
                    }
                }
            }
        }
    }

    protected open fun doSendAsPdfOrJpeg(textView: TextView, clip: Clip, callback: () -> Unit) {
        val appContext = AppContext.get()
        val context = textView.context
        withPdf(textView, clip.text) { document ->
            val file = createFile(context, clip, "pdf")
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            val fileUri = createFileUri(context, file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.type = "application/pdf"
            appContext.onMain {
                context.safeIntent(intent)
                callback.invoke()
            }
        }
    }

    private fun withPdf(textView: TextView, text: CharSequence?, callback: (document: PrintedPdfDocument) -> Unit) {
        val appContext = AppContext.get()
        val context = textView.context
        val theme = Theme.valueOf(appContext.getSettings())
        val textColor = ThemeUtils.getColor(context, android.R.attr.textColorPrimary)
        val textColorInverse = ThemeUtils.getColor(context, android.R.attr.textColorPrimaryInverse)
        textView.setTextColor(if (theme.dark) textColorInverse else textColor)
        val textConfig = appContext.mainState.getListConfig()
        textView.withConfig(textConfig.textFont, textConfig.textSize)
        doApply(textView, text) {
            textView.doOnNextLayout {
                appContext.onBackground {
                    try {
                        appContext.setLoadingState()
                        val attrs = PrintAttributes.Builder()
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                            .build()
                        val document = PrintedPdfDocument(context, attrs)
                        val width = textView.width + 144
                        val height = textView.height + 144
                        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 0)
                            .setContentRect(Rect(72, 72, width - 72, height - 72))
                            .create()
                        val page = document.startPage(pageInfo)
                        textView.draw(page.canvas)
                        document.finishPage(page)
                        callback.invoke(document)
                    } catch (e: Exception) {
                        appContext.showToast(appContext.string(R.string.fast_actions_error, e.message))
                        Analytics.onError("error_prepare_pdf", e)
                    } finally {
                        appContext.setLoadedState()
                    }
                }
            }
        }
    }

    protected fun createFile(context: Context, clip: Clip, extension: String): File {
        val files = File(context.filesDir, "files")
        FileExtraUtils.deleteQuietly(files)
        FileExtraUtils.forceMkdir(files)
        val name = createFileName(context, clip, extension)
        val file = File(files, name)
        FileExtraUtils.deleteQuietly(file)
        return file
    }

    protected fun createFileName(context: Context, clip: Clip, extension: String): String {
        val name = clip.title?.let { it.replace("[^\\w\\d]".toRegex(), "_") }?.trim()?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.clip_hint_title)
        return "${FormatUtils.buildUniqueName(name)}.$extension"
    }

    protected fun createFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.file_provider", file)
    }

    open fun isValid(text: String): Boolean = false


    companion object {
        private val textTypeExecutor by lazy { Executors.newSingleThreadExecutor() }
        private val disposableMap by lazy { mutableMapOf<Int, Disposable>() }
        private var permissionsRequested: Boolean = false

        const val LINK_NOT_CLICKABLE = 100

        val types = arrayOf(
            TEXT_PLAIN,
            LINK,
            QRCODE,
            MARKDOWN,
            HTML,
            LINE_CLICKABLE,
            WORD_CLICKABLE
        )

        private var lastClickTime = 0L
        private const val threshold = 600L

        fun withFileStoragePermissions(context: Context, text: CharSequence?, callback: () -> Unit) {
            val activity = context.findActivity()
            if (!permissionsRequested && activity is AppContainer && text.hasLocalFileReference()) {
                permissionsRequested = true
                activity.withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE) {
                    callback.invoke()
                }
            } else {
                callback.invoke()
            }
        }

        fun setClicked() {
            lastClickTime = System.currentTimeMillis()
        }

        fun isLinkClickAllowed(): Boolean {
            val prevClickTime = lastClickTime
            val currentClickTime = System.currentTimeMillis()
            lastClickTime = currentClickTime
            return prevClickTime == 0L || currentClickTime - prevClickTime > threshold
        }

        fun isLinkLongClickAllowed(): Boolean = isLinkClickAllowed()

        fun withSpannedText(textView: TextView, text: CharSequence?, spannedText: () -> CharSequence?, setter: (spannedText: CharSequence?) -> Unit) {
            val appContext = AppContext.get()
            textView.autoLinkMask = 0
            textView.movementMethod = withClickActions()
            if ((text?.length ?: 0) > appContext.appConfig.textLengthForAsyncRendering()) {
                val appliedText = SpannableStringBuilder(text ?: "")
                textView.setSpannableFactory(SimpleSpanBuilder.FACTORY)
                textView.setText(appliedText, TextView.BufferType.SPANNABLE)
                val textHashCode = System.identityHashCode(textView.text)
                textTypeExecutor.execute {
                    runCatching {
                        text?.let {
                            val spanned = spannedText.invoke() ?: ""
                            appContext.onMain {
                                if (textHashCode == System.identityHashCode(textView.text)) {
                                    setter.invoke(spanned)
                                }
                            }
                        }
                    }.exceptionOrNull()?.let {
                        appContext.showToast(appContext.string(R.string.fast_actions_error, it.message))
                        appContext.onMain {
                            if (textHashCode == System.identityHashCode(textView.text)) {
                                setter.invoke(appliedText)
                            }
                        }
                        Analytics.onError("withSpannedText", it)
                    }
                }
            } else {
                runCatching {
                    val spanned = spannedText.invoke() ?: ""
                    setter.invoke(spanned)
                }.exceptionOrNull()?.let {
                    appContext.showToast(appContext.string(R.string.fast_actions_error, it.message))
                    Analytics.onError("withSpannedText", it)
                }
            }
        }

        fun withClickActions(
            movement: BetterLinkMovementMethod = BetterLinkMovementMethodExt(),
            click: Boolean = true,
            longClick: Boolean = true
        ): BetterLinkMovementMethod {
            if (click) {
                movement.setOnLinkClickListener { textView, url ->
                    if (textView.tag == LINK_NOT_CLICKABLE) {
                        true
                    } else {
                        if (url.startsWith("file://")) {
                            val context = textView.context
                            if (context is AppContainer) {
                                context.withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE) {
                                    if (it.containsValue(false)) {
                                        Analytics.onPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
                                        return@withPermissions
                                    }
                                    try {
                                        val uri = Uri.parse(url)
                                        val file = File(uri.path!!)
                                        val newUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".file_provider", file)
                                        val intent = Intent(Intent.ACTION_VIEW, newUri)
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.packageName)
                                        L.log(this, "handle local file: {}", file)
                                        context.safeIntent(intent)
                                    } catch (e: Exception) {
                                        Analytics.onError("error_link_click", e)
                                    }
                                }
                            }
                            true
                        } else {
                            val context = textView.context
                            IntentUtils.open(context, url)
                            true
                        }
                    }
                }
            }
            if (longClick) {
                movement.setOnLinkLongClickListener { textView, text ->
                    when {
                        DynamicField.isDynamic(text) -> {
                            true
                        }
                        textView.tag == LINK_NOT_CLICKABLE -> {
                            true
                        }
                        else -> {
                            val context = textView.context
                            val url = text.replace("mailto:", "").replace("tel:", "")
                            Analytics.screenClipLink()
                            AddClipFragment.show(
                                context, AddClipRequest(
                                    text = url
                                )
                            )
                            true
                        }
                    }
                }
            }
            return movement
        }
    }

    private class SimpleClickableSpan : ClickableSpan() {
        override fun onClick(widget: View) {}
    }

    class BetterLinkMovementMethodExt : BetterLinkMovementMethod() {

        override fun dispatchUrlLongClick(textView: TextView, clickableSpan: ClickableSpan?) {
            try {
                if (isLinkLongClickAllowed()) {
                    if (clickableSpan !is MyCustomSpan) {
                        textView.cancelPendingInputEvents()
                        textView.cancelLongPress()
                    }
                    super.dispatchUrlLongClick(textView, clickableSpan)
                }
            } catch (th: Throwable) {
                Analytics.onError("dispatchUrlLongClick", th)
            }
        }

        override fun dispatchUrlClick(textView: TextView, clickableSpan: ClickableSpan?) {
            try {
                textView.cancelPendingInputEvents()
                textView.cancelLongPress()
                if (clickableSpan is MyClickableSpan) {
                    clickableSpan.onClick(textView)
                } else {
                    lastClickTime = System.currentTimeMillis()
                    super.dispatchUrlClick(textView, clickableSpan)
                }
            } catch (th: Throwable) {
                Analytics.onError("dispatchUrlClick", th)
            }
        }

        fun linkify(textView: TextView): BetterLinkMovementMethodExt {
            textView.movementMethod = this
            Linkify.addLinks(textView, Linkify.ALL)
            return this
        }
    }

}