package clipto.analytics

import clipto.common.analytics.A
import clipto.domain.FastAction
import clipto.domain.Clip
import java.net.UnknownHostException

object Analytics {

    fun onScreen(id: String) {
        A.event(id)
    }

    fun onReconnectSnapshot(id: String, count: Int) {
        A.event("on_reconnect_$id", "count", count)
    }

    fun onNewVersionAvailable() {
        A.event("on_new_version_available")
    }

    fun onDownloadNow() {
        A.event("on_download_now")
    }

    fun onDownloadLater() {
        A.event("on_download_later")
    }

    fun onActiveClipsListenerReconnect() {
        A.event("on_active_clips_listener_reconnect")
    }

    fun onDeletedClipsListenerReconnect() {
        A.event("on_deleted_clips_listener_reconnect")
    }

    fun onFiltersListenerReconnect() {
        A.event("on_filters_listener_reconnect")
    }

    fun screenClipPaste() {
        A.event("screen_clip_info_paste")
    }

    fun screenClipShare() {
        A.event("screen_clip_info_share")
    }

    fun screenClipProcessText() {
        A.event("screen_clip_info_process_text")
    }

    fun screenClipInfoEditTags() {
        A.event("screen_clip_info_edit_tags")
    }

    fun screenClipDetails(clip: Clip) {
        A.event("screen_clip_details", "id", clip.firestoreId, "length", clip.text?.length)
    }

    fun screenClipLink() {
        A.event("screen_clip_info_link")
    }

    fun screenClipInfo(clip: Clip) {
        A.event("screen_clip_info", "id", clip.firestoreId, "length", clip.text?.length)
    }

    fun screenEditClipAttributes() {
        A.event("screen_edit_clip_attributes")
    }

    fun screenMergeClips() {
        A.event("screen_merge_clips")
    }

    fun screenAttachmentAdd() {
        A.event("screen_attachment_add")
    }

    fun screenFaq() {
        A.event("screen_faq")
    }

    fun screenQWarning() {
        A.event("screen_q_warning")
    }

    fun screenTags() {
        A.event("screen_tags")
    }

    fun screenTagEdit() {
        A.event("screen_tag_edit")
    }

    fun screenTagLink() {
        A.event("screen_tag_link")
    }

    fun screenFilter() {
        A.event("screen_filter")
    }

    fun screenFilterDetailsClipboard() {
        A.event("screen_filter_details_clipboard")
    }

    fun screenFilterDetailsDeleted() {
        A.event("screen_filter_details_deleted")
    }

    fun screenClipDynamicData() {
        A.event("screen_clip_dynamic_data")
    }

    fun screenClipDetails() {
        A.event("screen_clip_details")
    }

    fun screenFilterDetailsFiltered() {
        A.event("screen_filter_details_filtered")
    }

    fun screenFilterDetailsStarred() {
        A.event("screen_filter_details_starred")
    }

    fun screenFilterDetailsTag() {
        A.event("screen_filter_details_tag")
    }

    fun screenFilterDetailsLink() {
        A.event("screen_filter_details_link")
    }

    fun screenDoubleClickActions() {
        A.event("screen_double_click_actions")
    }

    fun screenSwipeActions() {
        A.event("screen_swipe_actions")
    }

    fun screenFastActions() {
        A.event("screen_fast_actions")
    }

    fun screenFonts() {
        A.event("screen_fonts")
    }

    /**
     * Shared with app
     */
    fun screenAttachmentCreate() {
        A.event("screen_attachment_create")
    }

    fun screenAttachmentView() {
        A.event("screen_attachment_view")
    }

    fun screenConfigClipList() {
        A.event("screen_config_clip_list")
    }

    fun screenConfigClip() {
        A.event("screen_config_clip")
    }

    fun screenAccount() {
        A.event("screen_account")
    }

    fun screenSelectPlan() {
        A.event("screen_select_plan")
    }

    fun screenSelectPlanBanner() {
        A.event("screen_select_plan_banner")
    }

    fun screenDonations() {
        A.event("screen_donations")
    }

    fun screenSettings() {
        A.event("screen_settings")
    }

    fun screenRunes() {
        A.event("screen_runes")
    }

    fun screenScanBarcode() {
        A.event("screen_scan_barcode")
    }

    fun onError(method: String, th: Throwable) {
        if(th !is UnknownHostException) {
            A.error(method, th)
        }
    }

    fun onPermissionDenied(permission: String) {
        A.event("on_permisson_denied", "permission", permission)
    }

    fun onPrivacyPolicy() {
        A.event("on_privacy_policy")
    }

    fun onTermsOfService() {
        A.event("on_terms_of_service")
    }

    fun onRate() {
        A.event("on_rate")
    }

    fun onRateFive() {
        A.event("on_rate_five")
    }

    fun onReportNegativeFeedback() {
        A.event("on_report_negative_feedback")
    }

    fun onTranslate() {
        A.event("on_translate")
    }

    fun onShareApp() {
        A.event("on_share_app")
    }

    fun onOpenBrowserApp() {
        A.event("on_open_browser_app")
    }

    fun onDownloadDesktopApp() {
        A.event("on_download_desktop_app")
    }

    fun onIssue() {
        A.event("on_issue")
    }

    fun onEmail() {
        A.event("on_email")
    }

    fun onBugReport() {
        A.event("on_bug_report")
    }

    fun onBugInstructionRead() {
        A.event("on_bug_instruction_read")
    }

    fun onReddit() {
        A.event("on_reddit")
    }

    fun onDiscord() {
        A.event("on_discord")
    }

    fun onChangelog() {
        A.event("on_changelog")
    }

    fun onFacebook() {
        A.event("on_facebook")
    }

    fun onSignedIn() {
        A.event("on_signed_in")
    }

    fun onSignedOut() {
        A.event("on_signed_out")
    }

    fun onSyncDisabled() {
        A.event("on_sync_disabled")
    }

    fun onSyncEnabled() {
        A.event("on_sync_enabled")
    }

    fun onClearClipboard() {
        A.event("on_clear_clipboard")
    }

    fun onClearDeleted() {
        A.event("on_clear_deleted")
    }

    fun onSearchByCyrillic() {
        A.event("on_search_by_cyrillic")
    }

    fun onTrackedAfterReboot() {
        A.event("on_tracked_after_reboot")
    }

    fun onTrackedAfterUpdate() {
        A.event("on_tracked_after_update")
    }

    fun onRestoreAfterKill() {
        A.event("on_restore_after_kill")
    }

    fun onNoteInserted() {
        A.event("on_note_inserted")
    }

    fun onNoteInsertedWithPreview() {
        A.event("on_note_inserted_with_preview")
    }

    fun onNoteInsertedRemembered() {
        A.event("on_note_inserted_remembered")
    }

    fun onBarcodeScanned(type: Int) {
        A.event("on_barcode_scanned", "type", type)
    }

    fun onBackupAll() {
        A.event("on_backup_all")
    }

    fun onBackupSelected() {
        A.event("on_backup_selected")
    }

    fun onFastAction(action: FastAction, clip: Clip) {
        A.event("on_fast_action_${action.id}", "id", clip.firestoreId, "length", clip.text?.length)
    }

    fun onRestoreFromClipper() {
        A.event("on_restore_from_clipper")
    }

    fun onRestoreFromClipboardManager() {
        A.event("on_restore_from_cm_devdnua")
    }

    fun onRestoreFromSimplenote() {
        A.event("on_restore_from_simplenote")
    }

    fun onRestoreFromClipStack() {
        A.event("on_restore_from_clipstack")
    }

    fun onRestoreFromClipto() {
        A.event("on_restore_from_clipto")
    }

    fun onRestrictSync() {
        A.event("on_restrict_sync")
    }

    fun onNotePreviewTouched() {
        A.event("on_note_preview_touched")
    }

    fun initTrackClipboard() {
        A.event("init_track_clipboard")
    }

    fun initUniversalClipboard() {
        A.event("init_universal_clipboard")
    }

    fun initExcludeCustomAttrs() {
        A.event("init_exclude_custom_attrs")
    }

    fun initLaunchOnStartup() {
        A.event("init_launch_on_startup")
    }

    fun initEmulateCopyAction() {
        A.event("init_emulate_copy_action")
    }

    fun initIgnoreQWarning() {
        A.event("init_ignore_q_warning")
    }

    fun errorWrongAuthState() {
        A.event("error_wrong_auth_state")
    }

    fun errorWrongActivityStartIntent() {
        A.event("error_wrong_activity_start_intent")
    }

    fun onNotSerializableAction(actionId: String, actionSize: Int) {
        A.event("on_not_serializable_action", "id", actionId, "size", actionSize)
    }

    // FEATURES
    fun featureAutoTagByComma(comma: String) {
        A.event("feature_auto_tag", "comma", comma)
    }

    fun onPreview(type: String) {
        A.event("on_preview", "type", type)
    }

}