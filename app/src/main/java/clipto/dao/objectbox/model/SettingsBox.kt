package clipto.dao.objectbox.model

import clipto.dao.objectbox.converter.*
import clipto.domain.*
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import java.util.*

@Entity
class SettingsBox : Settings() {

    @Id
    var localId: Long = 0

    override var theme: Int = -1
    override var hideOnCopy: Boolean = false
    override var doubleClickToExit: Boolean = false
    override var doubleClickToDelete: Boolean = false
    override var doubleClickToEdit: Boolean = false
    override var launchOnStartup: Boolean = false
    override var showNotification: Boolean = false
    override var restoreFilterOnStart: Boolean = true
    override var doNotTrackClipboardChanges: Boolean = true
    override var removeNotesFromClipboard: Boolean = false
    override var universalClipboard: Boolean = false
    override var emulateCopyAction: Boolean = false
    override var hideLinkPreviews: Boolean = false
    override var disableSync: Boolean = false

    override var textFont: Int = 0
    override var textSize: Int = ClientSession.TEXT_SIZE_DEFAULT
        get() = if (field == 0) ClientSession.TEXT_SIZE_DEFAULT else field
    override var textLines: Int = ClientSession.TEXT_LINES_DEFAULT
        get() = if (field == 0) ClientSession.TEXT_LINES_DEFAULT else field
    override var textSeparator: String = ClientSession.SEPARATOR_NEW_LINE
        get() = if (field == null) ClientSession.SEPARATOR_NEW_LINE else field
    override var textPositionBeginning: Boolean = false
    override var textInsertRemember: Boolean = false
    override var autoSave: Boolean = false
    override var doNotPreviewLinks: Boolean = false

    override var referralId: String? = null

    @Convert(converter = TextTypeConverter::class, dbType = Int::class)
    override var textType: TextType = TextType.TEXT_PLAIN

    @Convert(converter = SwipeActionConverter::class, dbType = Int::class)
    override var swipeActionRight: SwipeAction = SwipeAction.STAR

    @Convert(converter = SwipeActionConverter::class, dbType = Int::class)
    override var swipeActionLeft: SwipeAction = SwipeAction.DELETE

    @Convert(converter = NotificationStyleConverter::class, dbType = Int::class)
    override var notificationStyle: NotificationStyle = NotificationStyle.NULL

    @Convert(converter = FastActionMetaConverter::class, dbType = String::class)
    override var fastActionsMeta: List<FastActionMeta> = emptyList()

    @Convert(converter = FontMetaConverter::class, dbType = String::class)
    override var fontsMeta: List<FontMeta> = emptyList()

    override var useFingerprint: Boolean = false
    override var lastSessionDate: Date? = null
    override var passcode: String? = null

    override var focusOnTitle: Boolean = true

    override var filterGroupSnippetsCollapsed: Boolean = true
    override var filterGroupFiltersCollapsed: Boolean = true
    override var filterGroupNotesCollapsed: Boolean = true
    override var filterGroupTagsCollapsed: Boolean = true
    override var filterGroupFoldersCollapsed: Boolean = true
    override var filterGroupFilesCollapsed: Boolean = true
    override var dynamicValueSelectedTab: Int = 0
    override var dynamicValueSnippetSearchBy: String? = null

    override var clipboardUseDefaultNotificationSound: Boolean = false

    override var texpanderRuneEnabled: Boolean = false
    override var texpanderUserHidden: Boolean = false
    override var texpanderX: Int = 0
    override var texpanderY: Int = 0
    override var texpanderWidth: Int = 0
    override var texpanderHeight: Int = 0

    override var settingsFlatMode: Boolean = false

    override var selectedCountry: String? = null
    override var selectedLanguage: String? = null

    override var noteShowAdditionalAttributes: Boolean = false

    override var doNotDisplayAndroidQWarning: Boolean = false

    @Convert(converter = ClipParamsTabConverter::class, dbType = Int::class)
    override var clipDetailsTab: ClipDetailsTab = ClipDetailsTab.GENERAL

    override var pauseClipboard: Boolean = false

    fun updateFastActions(meta: List<FastActionMeta>) {
        fastActionsMeta = FastActionMetaConverter.cache(meta)
    }

    fun updateFonts(meta: List<FontMeta>) {
        fontsMeta = FontMetaConverter.cache(meta)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SettingsBox

        if (localId != other.localId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + localId.hashCode()
        return result
    }

}

fun Settings.toBox(): SettingsBox =
    if (this is SettingsBox) {
        this
    } else {
        SettingsBox().apply(this) as SettingsBox
    }