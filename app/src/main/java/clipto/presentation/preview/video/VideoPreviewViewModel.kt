package clipto.presentation.preview.video

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import clipto.common.logging.L
import clipto.common.presentation.mvvm.lifecycle.SingleLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.framework.CastContext
import java.io.File


class VideoPreviewViewModel(app: Application) : VideoPreviewEmbedViewModel(app), Player.Listener, SessionAvailabilityListener {

    private val appName = string(app.applicationInfo.labelRes)
    private val userAgent = Util.getUserAgent(app, appName)

    private val castContext: CastContext? by lazy { runCatching { CastContext.getSharedInstance(app) }.getOrNull() }
    private val castPlayer: CastPlayer? by lazy { castContext?.let { CastPlayer(it) } }
    private val exoPlayer = ExoPlayer.Builder(app).build()
    private var currentItemInfo: Pair<MediaSource, String>? = null
    private var currentItem: MediaItem? = null
    private var currentPlayer: Player? = null
    private var currentItemIndex = 0

    val playerLiveData: MutableLiveData<Player> = SingleLiveData()

    override fun doClear() {
        super.doClear()
        runCatching { exoPlayer.stop() }
        runCatching { exoPlayer.release() }
        runCatching { castPlayer?.setSessionAvailabilityListener(null) }
    }

    override fun onPlayerError(error: PlaybackException) {
        val message = (error.localizedMessage ?: error.message
        ?: error.toString()).replace("com.google.android.exoplayer2.source.", "")
        errorLiveData.postValue(error)
        showToast(message)
    }

    override fun onCastSessionAvailable() {
        castPlayer?.let { setCurrentPlayer(it) }
    }

    override fun onCastSessionUnavailable() {
        setCurrentPlayer(exoPlayer)
    }

    private fun setCurrentPlayer(currentPlayer: Player) {
        if (this.currentPlayer === currentPlayer) return

        L.log(this, "setCurrentPlayer: {}", currentPlayer)

        // Player state management.
        var playbackPositionMs = C.TIME_UNSET
        var windowIndex = C.INDEX_UNSET
        var playWhenReady = false
        val previousPlayer = this.currentPlayer
        if (previousPlayer != null) {
            // Save state from the previous player.
            val playbackState = previousPlayer.playbackState
            if (playbackState != Player.STATE_ENDED) {
                playbackPositionMs = previousPlayer.currentPosition
                playWhenReady = previousPlayer.playWhenReady
                windowIndex = previousPlayer.currentMediaItemIndex
                if (windowIndex != currentItemIndex) {
                    playbackPositionMs = C.TIME_UNSET
                    windowIndex = currentItemIndex
                }
            }
            previousPlayer.stop()
            previousPlayer.clearMediaItems()
        }
        this.currentPlayer = currentPlayer


        // Media queue management.
        if (currentPlayer === exoPlayer) {
            currentItemInfo?.let {
                exoPlayer.setMediaSource(it.first)
                exoPlayer.prepare()
            }
        }

        // Playback transition.
        if (windowIndex != C.INDEX_UNSET) {
            setCurrentItem(windowIndex, playbackPositionMs, playWhenReady)
        }

        playerLiveData.postValue(currentPlayer)
    }

    private fun setCurrentItem(itemIndex: Int, positionMs: Long, playWhenReady: Boolean) {
        val currentItemRef = currentItem
        if (currentPlayer === castPlayer && castPlayer?.currentTimeline?.isEmpty == true && currentItemRef != null) {
            castPlayer?.setMediaItems(listOf(currentItemRef), itemIndex, positionMs)
        } else {
            currentPlayer!!.seekTo(itemIndex, positionMs)
            currentPlayer!!.playWhenReady = playWhenReady
        }
    }

    fun isCasting() = currentPlayer == castPlayer

    fun onPausePlayer() {
        L.log(this, "onPausePlayer")
        exoPlayer.playWhenReady = false
    }

    fun onPlay(uri: Uri, hasCache: Boolean, title: String?) {
        val url = uri.toString()
        L.log(this, "onPlay: uri={}, hasCache={}, title={}", uri, hasCache, title)
        val dataSourceFactory: DataSource.Factory =
            if (hasCache && URLUtil.isNetworkUrl(url)) {
                getCacheDataSourceFactory(app, appName)
            } else {
                DefaultDataSource.Factory(app)
            }
        currentItemInfo = createMediaSource(uri, dataSourceFactory)
        L.log(this, "onPlay: uri={}, cache={}, info={}", uri, hasCache, currentItemInfo)
        currentItem = MediaItem.Builder()
            .setUri(uri)
            .setMimeType(currentItemInfo!!.second)
            .build()
        val cast = castPlayer
        setCurrentPlayer(if (cast != null && cast.isCastSessionAvailable) cast else exoPlayer)
    }

    private fun createMediaSource(uri: Uri, dataSourceFactory: DataSource.Factory): Pair<MediaSource, String> {
        return when (Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri) to MimeTypes.APPLICATION_MPD
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri) to MimeTypes.APPLICATION_SS
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri) to MimeTypes.APPLICATION_M3U8
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri) to MimeTypes.VIDEO_UNKNOWN
            else -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri) to MimeTypes.VIDEO_UNKNOWN
        }
    }

    fun onBind(playerView: StyledPlayerView, castControlView: StyledPlayerControlView) {
        exoPlayer.playWhenReady = true
        exoPlayer.addListener(this)
        playerView.player = exoPlayer

        castPlayer?.addListener(this)
        castPlayer?.setSessionAvailabilityListener(this)
        castControlView.player = castPlayer
    }

    @Synchronized
    private fun getCacheDataSourceFactory(context: Context, appName: String): DataSource.Factory {
        if (dataSourceFactory == null) {
            dataSourceFactory = LocalCacheDataSourceFactory(context, appName)
        }
        return dataSourceFactory!!
    }

    class LocalCacheDataSourceFactory(val context: Context, val appName: String) : DataSource.Factory {

        private val maxCacheSize: Long = 100 * 1024 * 1024
        private val maxFileSize: Long = CacheDataSink.DEFAULT_FRAGMENT_SIZE
        private val fileDataSource: FileDataSource = FileDataSource()
        private val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        private val userAgent = Util.getUserAgent(context, context.getString(context.applicationInfo.labelRes))
        private val defaultDataSourceFactory: DefaultDataSourceFactory = DefaultDataSourceFactory(
            context,
            bandwidthMeter,
            DefaultHttpDataSource.Factory().setUserAgent(userAgent)
        )
        private val simpleCache: SimpleCache = SimpleCache(
            getCacheDir(),
            LeastRecentlyUsedCacheEvictor(maxCacheSize),
            StandaloneDatabaseProvider(context)
        )
        private val cacheDataSink: CacheDataSink = CacheDataSink(simpleCache, maxFileSize)

        override fun createDataSource(): DataSource {
            return CacheDataSource(
                simpleCache, defaultDataSourceFactory.createDataSource(),
                fileDataSource, cacheDataSink,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                null
            )
        }

        private fun getCacheDir(): File {
            return File(context.externalCacheDir, "$appName Player")
        }
    }

    companion object {
        private var dataSourceFactory: DataSource.Factory? = null
    }

}