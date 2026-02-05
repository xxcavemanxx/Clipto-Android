package clipto.presentation.preview.video.url

interface IVideoUrlExtractor {

    fun getVideoId(url: String): String?

    fun extractVideoData(url: String, videoId: String): UrlData?

}