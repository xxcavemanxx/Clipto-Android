package clipto.presentation.preview.video.url

data class UrlData(
        var playbackUrl: String? = null,
        var embedUrl:String? = null,
        var type:String? = null,
        var error: Throwable? = null
)