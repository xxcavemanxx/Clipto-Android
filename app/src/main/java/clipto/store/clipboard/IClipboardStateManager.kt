package clipto.store.clipboard

import clipto.domain.Clip

interface IClipboardStateManager {

    fun onRefreshClipboard()

    fun onUniversalCopy(clip: Clip)

    fun onTrack(track: Boolean, restoreLastClip:Boolean = false)

    fun onCopy(clip: Clip, saveCopied: Boolean = true, clearSelection: Boolean = true, withToast: Boolean = true, callback: () -> Unit = {})

    fun onCopy(clips: Collection<Clip>, saveCopied: Boolean = true, clearSelection: Boolean = true, callback: () -> Unit = {})

}