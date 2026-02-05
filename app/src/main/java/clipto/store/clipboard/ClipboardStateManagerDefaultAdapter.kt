package clipto.store.clipboard

internal class ClipboardStateManagerDefaultAdapter constructor(manager: ClipboardStateManager) : ClipboardStateManagerAdapter(manager) {

    override fun onTrack(track: Boolean, restoreLastClip: Boolean) {
        if (track) {
            manager.clipboardManager.addPrimaryClipChangedListener(manager)
        } else {
            manager.clipboardManager.removePrimaryClipChangedListener(manager)
        }
    }

}