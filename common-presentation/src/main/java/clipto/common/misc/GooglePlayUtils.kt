package clipto.common.misc

import android.content.Context
import android.content.Intent
import android.net.Uri

object GooglePlayUtils {

    fun rate(context: Context): Intent {
        val packageName = context.packageName
        return intentGooglePlay(packageName)
    }

    private fun googlePlayUrl(packageName: String): String {
        return FormatUtils.buildString("market://details?id=", packageName)
    }

    private fun intentGooglePlay(packageName: String): Intent {
        val uri = Uri.parse(googlePlayUrl(packageName))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

}
