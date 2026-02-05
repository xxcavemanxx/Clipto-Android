package clipto.presentation.preview.video

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import clipto.common.extensions.safeIntent
import clipto.common.logging.L
import clipto.common.presentation.mvvm.base.BaseActivity
import clipto.common.presentation.mvvm.base.showSystemUI
import clipto.extensions.onCreateWithLocale
import com.wb.clipboard.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VideoPreviewActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_preview_video

    override fun onCreate(savedInstanceState: Bundle?) {
        onCreateWithLocale()
        super.onCreate(savedInstanceState)
        showSystemUI()
        val url = intent.data
        if (url != null) {
            val cache = false
            val embed = intent.getBooleanExtra(ATTR_EMBED, false)
            val title = intent.getStringExtra(VideoPreviewFragment.ATTR_TITLE)
            if (embed) {
                VideoPreviewEmbedFragment.open(this, url.toString(), title)
            } else {
                VideoPreviewFragment.open(this, url.toString(), cache, title)
            }
        } else {
            finish()
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        const val ATTR_EMBED = "attr_embed"

        fun play(context: Context, url: String, title: String? = null, embed: Boolean = false) {
            L.log(this, "preview: url={}", url)
            val intent = Intent(context, VideoPreviewActivity::class.java)
            intent.putExtra(VideoPreviewFragment.ATTR_TITLE, title)
            intent.putExtra(ATTR_EMBED, embed)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data = Uri.parse(url)
            context.safeIntent(intent)
        }
    }

}