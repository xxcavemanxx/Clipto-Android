package clipto.common.misc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import clipto.common.extensions.safeIntent

object IntentUtils {

    fun getPendingIntentFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    fun share(context: Context, text: String?, title: String? = null) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_TITLE, title)
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, null)
        context.safeIntent(chooser)
    }

    fun email(context: Context, email: String? = null, subject: String? = null, message: String? = null) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        if (subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        if (message != null) {
            intent.putExtra(Intent.EXTRA_TEXT, message)
        }
        if (email != null) {
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        val chooser = Intent.createChooser(intent, subject)
        context.safeIntent(chooser)
    }

    fun call(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        context.safeIntent(intent)
    }

    fun sms(context: Context, text: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:")
        intent.putExtra("sms_body", text)
        context.safeIntent(intent)
    }

    fun smsTo(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("smsto:${phone}")
        context.safeIntent(intent)
    }

    fun open(context: Context, url: String, type: String? = null): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (type != null) {
            intent.setDataAndType(Uri.parse(url), type)
        } else {
            intent.data = Uri.parse(url)
        }
        val chooser = Intent.createChooser(intent, null)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return context.safeIntent(chooser)
    }

    fun send(context: Context, uri: Uri, type: String? = null): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            if (type != null) {
                setDataAndType(uri, type)
            } else {
                data = uri
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            if (type != null) {
                setDataAndType(uri, type)
            } else {
                data = uri
            }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, null)
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(viewIntent))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return context.safeIntent(chooser)
    }

}
