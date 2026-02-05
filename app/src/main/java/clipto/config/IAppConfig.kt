package clipto.config

import clipto.domain.RuneConfig
import clipto.domain.User
import java.util.*

interface IAppConfig {

    fun getRxTimeout(): Long

    fun getUiTimeout(): Long

    fun getApiVersion(): Int

    fun getDate(user: User?, id: String): Date?
    fun saveDate(user: User?, id: String, date: Date?)

    fun isNewVersionAvailable(): Boolean
    fun remindAboutNewVersionLater(version: String)

    // Attachments
    fun hasAccessToAttachments(): Boolean
    fun attachmentUploadLimitInKilobytes(): Int
    fun attachmentUploadLimitInBytes(): Int
    fun attachmentUploadLimit(): Int

    // Mobile
    fun hasAccessToScanBarcode(): Boolean

    // Clipboard
    fun canUpdateNotificationOnResume(): Boolean
    fun canTakeNoteFromClipboardOnResume(): Boolean
    fun canPauseClipboardOnScreenLock(): Boolean
    fun canSilentlyVibrateOnClipboardChanges(): Boolean
    fun clipboardAwareInterval(): Long
    fun clipboardHideOnCopyDelay(): Long
    fun limitClipboardNotesRange(): IntArray
    fun limitClipboardNotesDefault(): Int
    fun limitClipboardNotesCleanupCount(): Int
    fun logcatCancelTimeout(): Int
    fun logcatReadTimeout(): Int
    fun notificationUseApplicationStyle(): Boolean
    fun canRestoreLastClipOnStart(): Boolean

    // Common
    fun maxLengthTag(): Int
    fun maxLengthTitle(): Int
    fun maxLengthDescription(): Int
    fun maxLengthAbbreviation(): Int

    // Note
    fun canCreatePublicLinks(): Boolean
    fun noteScrollBarMultiplier(): Int
    fun textLengthForAsyncRendering(): Int
    fun clipEditFocusOnText(): Boolean
    fun clipInfoMaxTextLines(): Int
    fun autoSaveInterval(): Long
    fun notePreviewModeAutoScrollMultiplier(): Int
    fun notePreventAccidentClicksDistance(): Int
    fun noteLinkClickRadius(): Int
    fun noteSupportFastScroll(): Boolean
    fun noteMaxSizeInKb(): Long
    fun getAbbreviationLeadingSymbol(): String

    fun noteSupportFastPager(): Boolean
    fun noteSupportFastPagerHapticFeedback(): Boolean
    fun noteSupportFastPagerHapticFeedbackMaxWhenScroll(): Int
    fun getFastScrollBarMinTextLines(): Int

    // Markdown
    fun markdownEditorActivated(): Boolean
    fun markdownStrikethroughNormalizationActivated(): Boolean
    fun markdownBulletListNormalizationActivated(): Boolean

    // Main List
    fun mainListDisplayCounter(): Boolean
    fun mainListSupportFastScroll(): Boolean

    // Tag
    fun getColorsMatrix(): List<List<String?>>
    fun getTagColors(): List<List<String>>
    fun canCreateTagAutoRules(): Boolean
    fun tagRuleMaxLength(): Int

    // Recycle Bin
    fun limitDeletedNotesRange(): IntArray
    fun limitDeletedNotesDefault(): Int

    // Technical
    fun canRequestRebuildIndex(): Boolean
    fun getClipListSize(): Int
    fun getLatestVersionRemindIntervalInHours(): Int
    fun getLatestVersion(): String
    fun getLatestVersionChanges(): String
    fun apiReadTimeout(): Long
    fun canReportIssues(): Boolean
    fun canReportNegativeFeedback(): Boolean
    fun canReportUnexpectedErrors(): Boolean
    fun canReportUnexpectedErrorDirectly(): Boolean
    fun dataLoadingTimeout(): Long
    fun externalActionDelay(): Long
    fun firebaseAnalyticsCollectionEnabled(): Boolean
    fun firestoreBatchSize(): Int
    fun firestoreBatchDelay(): Long
    fun firebaseLanguageIdentificationThreshold(): Float
    fun maxNotesForContextActions(): Int
    fun textLayoutDelay(): Long
    fun queryOnlyLatestChangedNotesThreshold(): Int
    fun queryOnlyLatestChangedNotesReconnectDelay(): Long
    fun queryOnlyLatestChangedNotesMinimumInterval(): Long
    fun queryOnlyLatestChangedNotesUseInitialQuery(): Boolean
    fun canIncludeRemoteConfigInEmail(): Boolean
    fun canDeleteFiles(): Boolean
    fun canRemoveNotSyncedNotesOnLogout(): Boolean
    fun getInternetStateCheckInterval(): Long
    fun getMaxOpenGraphSizeInKb():Int

    // Sync plans
    fun syncPlanActivated(): Boolean
    fun syncPlanNotesFreeLimit(): Int
    fun syncPlanNotesBonusForSnippet(): Int
    fun syncPlanNotesRecommendedLimit(): Int
    fun syncPlanContributorProgramEnabled(): Boolean
    fun syncPlans(): List<List<String>>

    // Urls
    fun getLinkPreviewExampleUrl(): String
    fun getInviteFriendRewardUrl(): String
    fun getAdbInstructionUrl(): String
    fun getNotificationPasteInstructionUrl(): String
    fun getGlobalCopyInstructionUrl(): String
    fun getTranslateUrl(): String
    fun getFacebookUrl(): String
    fun getRedditUrl(): String
    fun getGithubUrl(): String
    fun getUnexpectedErrorInstructionUrl(): String
    fun getFaqUrl(): String
    fun getDiscordUrl(): String
    fun getChangelogUrl(): String
    fun getSupportEmail(): String

    fun getAppGuideId():String

    // Backup
    fun getBackupSupportedImportFormats(): String

    // Smart Actions
    fun smartActionShortenLinkMinimumLength(): Int
    fun smartActionShortenLinkEnabled(): Boolean

    // Security
    fun autoLockInMinutes(): Int
    fun autoLockWithForeground(): Boolean
    fun suggestSetPassCodeOnSignIn(): Boolean

    // Dynamic Values
    fun deepReplaceLevel(): Int

    // Runes
    fun isRunesEnabled(): Boolean
    fun getRuneConfigs(): List<RuneConfig>
    fun getRuneSwipePowerDelay(): Long

    // Texpander
    fun texpanderTextSize(): Int
    fun texpanderLayoutDelay(): Long
    fun texpanderEventThreshold(): Long
    fun texpanderAutoHide(): Boolean
    fun texpanderDoubleClickToShow(): Boolean
    fun texpanderIgnoreInternalEvents(): Boolean
    fun texpanderLastEventStateCheckDelay(): Long
    fun texpanderDoubleClickToShowThreshold(): Int
    fun texpanderPanelDestroyOnInactivityIntervalInSeconds(): Int
    fun texpanderAdvancedConfigUrl(): String
    fun texpanderUndoRedoInputMemory(): Int
    fun texpanderCheckTypeForTextAvailability(packageName: CharSequence?): Boolean

    fun getNotificationIntentMaxSize(): Long
    fun getNotificationTextMaxSize(): Int

    // API
    fun getYoutubeDataApiKey(): String

    // Snippets
    fun isSnippetsPublicLibraryAuthRequired(): Boolean
    fun isSnippetsPublicLibraryAvailable(): Boolean
    fun getSnippetsKitBonusProgramUrl(): String
    fun getSnippetsKitMinimumSize(): Int

    fun getDynamicValueRenderingDelay(): Long
    fun getDynamicValueRefreshDelay(): Long
    fun getDynamicValueLabelMaxLength(): Int
    fun getDynamicTextRequestDelay(): Long

}

