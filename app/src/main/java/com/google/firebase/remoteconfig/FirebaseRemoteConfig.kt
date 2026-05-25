package com.google.firebase.remoteconfig

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class FirebaseRemoteConfig {
    companion object {
        private val instance = FirebaseRemoteConfig()
        @JvmStatic
        fun getInstance(): FirebaseRemoteConfig = instance
    }

    val all: Map<String, Any> = emptyMap()

    fun getString(key: String): String = when (key) {
        "rune_configs" -> "[]"
        "limit_clipboard_notes_range" -> "[20, 50, 100, 200, 500]"
        "limit_deleted_notes_range" -> "[20, 50, 100, 200, 500]"
        "tag_colors" -> "[[\"#f44336\", \"#e91e63\", \"#9c27b0\"]]"
        "app_latest_version" -> "1.0.0"
        "note_abbreviation_leading_symbol" -> "/"
        else -> ""
    }

    fun getBoolean(key: String): Boolean = when (key) {
        "access_attachments", "access_scan_barcode", "can_update_notification_on_resume",
        "can_take_note_from_clipboard_on_resume", "can_pause_clipboard_on_screen_lock",
        "can_silently_vibrate_on_clipboard_changes", "notification_use_application_style",
        "can_restore_last_clip_on_start", "clip_edit_focus_on_text", "note_support_fast_scroll",
        "note_support_fast_pager", "note_support_fast_pager_haptic_feedback", "markdown_editor_activated",
        "markdown_strikethrough_normalization_activated", "markdown_bullet_list_normalization_activated",
        "main_list_display_counter", "main_list_support_fast_scroll", "can_create_tag_auto_rules",
        "can_request_rebuild_index", "can_include_remote_config_in_email" -> true
        else -> false
    }

    fun getLong(key: String): Long = when (key) {
        "attachment_upload_limit" -> 10485760L // 10MB
        "clipboard_aware_interval" -> 1000L
        "clipboard_hide_on_copy_delay" -> 2000L
        "limit_clipboard_notes_default" -> 100L
        "limit_clipboard_notes_cleanup_count" -> 20L
        "logcat_cancel_timeout" -> 5000L
        "logcat_read_timeout" -> 5000L
        "max_length_tag" -> 50L
        "max_length_title" -> 100L
        "max_length_description" -> 10000L
        "max_length_abbreviation" -> 20L
        "note_scrollbar_multiplier" -> 1L
        "text_length_for_async_rendering" -> 5000L
        "clip_info_max_text_lines" -> 5L
        "auto_save_interval" -> 5000L
        "note_preview_mode_multiplier" -> 1L
        "note_prevent_accident_clicks_distance" -> 10L
        "note_link_click_radius" -> 10L
        "note_support_fast_pager_haptic_feedback_max_when_scroll" -> 100L
        "note_max_size_in_kb" -> 10240L
        "note_fast_scroll_min_text_lines" -> 20L
        "tag_rule_max_length" -> 50L
        "limit_deleted_notes_default" -> 100L
        "app_latest_version_remind_interval_in_hours" -> 24L
        else -> 0L
    }

    fun getDouble(key: String): Double = when (key) {
        "firebase_language_identification_threshold" -> 0.5
        else -> 0.0
    }

    fun setConfigSettingsAsync(settings: FirebaseRemoteConfigSettings): Task<Void> = Tasks.forResult(null)
    fun setDefaultsAsync(resourceId: Int): Task<Void> = Tasks.forResult(null)
    fun fetchAndActivate(): Task<Boolean> = Tasks.forResult(true)
}

class FirebaseRemoteConfigSettings {
    class Builder {
        fun setMinimumFetchIntervalInSeconds(minimumFetchIntervalInSeconds: Long): Builder = this
        fun build(): FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings()
    }
}
