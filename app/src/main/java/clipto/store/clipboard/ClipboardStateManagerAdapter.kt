package clipto.store.clipboard

import clipto.domain.Clip

abstract class ClipboardStateManagerAdapter(val manager: ClipboardStateManager) : IClipboardStateManager {

    override fun onTrack(track: Boolean, restoreLastClip: Boolean) = Unit

    override fun onRefreshClipboard() {
        manager.clipboardState.refreshClipboard()
    }

    override fun onUniversalCopy(clip: Clip) {
        manager.copyClipsAction.execute(
            clips = listOf(clip),
            clearSelection = false,
            saveCopied = false
        )
    }

    override fun onCopy(clips: Collection<Clip>, saveCopied: Boolean, clearSelection: Boolean, callback: () -> Unit) {
        manager.copyClipsAction.execute(
            clips = clips,
            saveCopied = saveCopied,
            clearSelection = clearSelection,
            callback = callback
        )
    }

    override fun onCopy(
        clip: Clip,
        saveCopied: Boolean,
        clearSelection: Boolean,
        withToast: Boolean,
        callback: () -> Unit
    ) {
        manager.copyClipsAction.execute(
            clips = listOf(clip),
            saveCopied = saveCopied,
            clearSelection = clearSelection,
            withToast = withToast,
            callback = callback
        )
    }

}