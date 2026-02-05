package clipto.domain

import com.google.gson.annotations.SerializedName
import java.util.*

open class Settings {

    @SerializedName("theme")
    open var theme: Int = -1

    @SerializedName("hideOnCopy")
    open var hideOnCopy: Boolean = false

    @SerializedName("doubleClickToExit")
    open var doubleClickToExit: Boolean = false

    @SerializedName("doubleClickToDelete")
    open var doubleClickToDelete: Boolean = false

    @SerializedName("doubleClickToEdit")
    open var doubleClickToEdit: Boolean = false

    @SerializedName("launchOnStartup")
    open var launchOnStartup: Boolean = false

    @SerializedName("showNotification")
    open var showNotification: Boolean = false

    @SerializedName("restoreFilterOnStart")
    open var restoreFilterOnStart: Boolean = true

    @SerializedName("doNotTrackClipboardChanges")
    open var doNotTrackClipboardChanges: Boolean = true

    @SerializedName("removeNotesFromClipboard")
    open var removeNotesFromClipboard: Boolean = false

    @SerializedName("universalClipboard")
    open var universalClipboard: Boolean = false

    @SerializedName("emulateCopyAction")
    open var emulateCopyAction: Boolean = false

    @SerializedName("hideLinkPreviews")
    open var hideLinkPreviews: Boolean = false

    @SerializedName("textType")
    open var textType: TextType = TextType.TEXT_PLAIN

    @SerializedName("textFont")
    open var textFont: Int = 0

    @SerializedName("textSize")
    open var textSize: Int = ClientSession.TEXT_SIZE_DEFAULT

    @SerializedName("textLines")
    open var textLines: Int = ClientSession.TEXT_LINES_DEFAULT

    @SerializedName("textSeparator")
    open var textSeparator: String = ClientSession.SEPARATOR_NEW_LINE

    @SerializedName("textPositionBeginning")
    open var textPositionBeginning: Boolean = false

    @SerializedName("textInsertRemember")
    open var textInsertRemember: Boolean = false

    @Transient
    open var referralId: String? = null

    @SerializedName("swipeActionRight")
    open var swipeActionRight: SwipeAction = SwipeAction.STAR

    @SerializedName("swipeActionLeft")
    open var swipeActionLeft: SwipeAction = SwipeAction.DELETE

    @SerializedName("notificationStyle")
    open var notificationStyle: NotificationStyle = NotificationStyle.NULL

    @SerializedName("disableSync")
    open var disableSync: Boolean = false

    @SerializedName("autoSave")
    open var autoSave: Boolean = false

    @SerializedName("doNotPreviewLinks")
    open var doNotPreviewLinks: Boolean = false

    @SerializedName("focusOnTitle")
    open var focusOnTitle: Boolean = true

    @SerializedName("pauseClipboard")
    open var pauseClipboard: Boolean = false

    @Transient
    open var fastActionsMeta: List<FastActionMeta> = emptyList()

    @Transient
    open var fontsMeta: List<FontMeta> = emptyList()

    @Transient
    open var useFingerprint: Boolean = false

    @Transient
    open var lastSessionDate: Date? = null

    @Transient
    open var passcode: String? = null

    @SerializedName("filterGroupSnippetsCollapsed")
    open var filterGroupSnippetsCollapsed: Boolean = true

    @SerializedName("filterGroupFiltersCollapsed")
    open var filterGroupFiltersCollapsed: Boolean = true

    @SerializedName("filterGroupNotesCollapsed")
    open var filterGroupNotesCollapsed: Boolean = true

    @SerializedName("filterGroupTagsCollapsed")
    open var filterGroupTagsCollapsed: Boolean = true

    @SerializedName("filterGroupFoldersCollapsed")
    open var filterGroupFoldersCollapsed: Boolean = true

    @SerializedName("filterGroupFilesCollapsed")
    open var filterGroupFilesCollapsed: Boolean = true

    @SerializedName("dynamicValueSelectedTab")
    open var dynamicValueSelectedTab: Int = 0

    @SerializedName("dynamicValueSnippetSearchBy")
    open var dynamicValueSnippetSearchBy: String? = null

    @SerializedName("clipDetailsTab")
    open var clipDetailsTab: ClipDetailsTab = ClipDetailsTab.GENERAL

    @SerializedName("noteShowAdditionalAttributes")
    open var noteShowAdditionalAttributes: Boolean = false

    @SerializedName("settingsFlatMode")
    open var settingsFlatMode: Boolean = false

    @SerializedName("clipboardUseDefaultNotificationSound")
    open var clipboardUseDefaultNotificationSound = false

    @SerializedName("doNotDisplayAndroidQWarning")
    open var doNotDisplayAndroidQWarning: Boolean = false

    @SerializedName("texpanderRuneEnabled")
    open var texpanderRuneEnabled: Boolean = false
    open var texpanderUserHidden: Boolean = false
    open var texpanderX: Int = 0
    open var texpanderY: Int = 0
    open var texpanderWidth: Int = 0
    open var texpanderHeight: Int = 0

    @SerializedName("selectedCountry")
    open var selectedCountry: String? = null

    @SerializedName("selectedLanguage")
    open var selectedLanguage: String? = null

    fun isLocked(): Boolean = !passcode.isNullOrBlank()

    fun apply(from: Settings): Settings {
        theme = from.theme
        hideOnCopy = from.hideOnCopy
        doubleClickToExit = from.doubleClickToExit
        doubleClickToDelete = from.doubleClickToDelete
        doubleClickToEdit = from.doubleClickToEdit
        launchOnStartup = from.launchOnStartup
        showNotification = from.showNotification
        restoreFilterOnStart = from.restoreFilterOnStart
        doNotTrackClipboardChanges = from.doNotTrackClipboardChanges
        removeNotesFromClipboard = from.removeNotesFromClipboard
        universalClipboard = from.universalClipboard
        doNotPreviewLinks = from.doNotPreviewLinks
        emulateCopyAction = from.emulateCopyAction
        hideLinkPreviews = from.hideLinkPreviews
        fastActionsMeta = from.fastActionsMeta
        fontsMeta = from.fontsMeta

        textType = from.textType
        textFont = from.textFont
        textSize = from.textSize
        textLines = from.textLines
        textSeparator = from.textSeparator
        textPositionBeginning = from.textPositionBeginning
        textInsertRemember = from.textInsertRemember

        referralId = from.referralId
        swipeActionRight = from.swipeActionRight
        swipeActionLeft = from.swipeActionLeft
        notificationStyle = from.notificationStyle
        disableSync = from.disableSync
        autoSave = from.autoSave
        focusOnTitle = from.focusOnTitle

        useFingerprint = from.useFingerprint
        lastSessionDate = from.lastSessionDate
        passcode = from.passcode

        filterGroupFilesCollapsed = from.filterGroupFilesCollapsed
        filterGroupFoldersCollapsed = from.filterGroupFoldersCollapsed
        filterGroupSnippetsCollapsed = from.filterGroupSnippetsCollapsed
        filterGroupFiltersCollapsed = from.filterGroupFiltersCollapsed
        filterGroupNotesCollapsed = from.filterGroupNotesCollapsed
        filterGroupTagsCollapsed = from.filterGroupTagsCollapsed
        dynamicValueSelectedTab = from.dynamicValueSelectedTab
        dynamicValueSnippetSearchBy = from.dynamicValueSnippetSearchBy

        clipboardUseDefaultNotificationSound = from.clipboardUseDefaultNotificationSound

        texpanderRuneEnabled = from.texpanderRuneEnabled
        texpanderUserHidden = from.texpanderUserHidden
        texpanderX = from.texpanderX
        texpanderY = from.texpanderY
        texpanderWidth = from.texpanderWidth
        texpanderHeight = from.texpanderHeight

        settingsFlatMode = from.settingsFlatMode

        clipDetailsTab = from.clipDetailsTab

        selectedCountry = from.selectedCountry
        selectedLanguage = from.selectedLanguage

        noteShowAdditionalAttributes = from.noteShowAdditionalAttributes

        pauseClipboard = from.pauseClipboard

        return this
    }

    fun apply(from: ListConfig): Settings {
        hideLinkPreviews = from.hideLinkPreviews
        textLines = from.textLines
        textSize = from.textSize
        textFont = from.textFont
        return this
    }

}