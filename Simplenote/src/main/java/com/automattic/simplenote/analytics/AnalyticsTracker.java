package com.automattic.simplenote.analytics;

import com.automattic.simplenote.Simplenote;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AnalyticsTracker {
    private static final List<Tracker> TRACKERS = new ArrayList<>();
    public static String CATEGORY_NOTE = "note";
    public static String CATEGORY_LINK = "link";
    public static String CATEGORY_SEARCH = "search";
    public static String CATEGORY_SETTING = "setting";
    public static String CATEGORY_TAG = "tag";
    public static String CATEGORY_USER = "user";
    public static String CATEGORY_WIDGET = "widget";

    private AnalyticsTracker() {
    }

    public static void registerTracker(Tracker tracker) {
        if (tracker != null) {
            TRACKERS.add(tracker);
        }
    }

    public static void track(Stat stat, String category, String label) {
        if (!Simplenote.analyticsIsEnabled()) {
            return;
        }

        for (Tracker tracker : TRACKERS) {
            tracker.track(stat, category, label, null);
        }
    }

    public static void track(Stat stat, String category, String label, Map<String, ?> properties) {
        if (!Simplenote.analyticsIsEnabled()) {
            return;
        }

        for (Tracker tracker : TRACKERS) {
            tracker.track(stat, category, label, properties);
        }
    }

    public static void refreshMetadata(String username) {
        if (!Simplenote.analyticsIsEnabled()) {
            return;
        }

        for (Tracker tracker : TRACKERS) {
            tracker.refreshMetadata(username);
        }
    }

    public static void flush() {
        if (!Simplenote.analyticsIsEnabled()) {
            return;
        }

        for (Tracker tracker : TRACKERS) {
            tracker.flush();
        }
    }

    public static void track(Stat stat) {
        if (!Simplenote.analyticsIsEnabled()) {
            return;
        }

        for (Tracker tracker : TRACKERS) {
            tracker.track(stat, null, null);
        }
    }

    public static void track(Stat stat, Map<String, ?> properties) {
        if (!Simplenote.analyticsIsEnabled()) {
            return;
        }

        for (Tracker tracker : TRACKERS) {
            tracker.track(stat, null, null, properties);
        }
    }

    @SuppressWarnings("unused")
    public enum Stat {
        EDITOR_NOTE_CREATED,
        EDITOR_NOTE_DELETED,
        EDITOR_NOTE_RESTORED,
        EDITOR_ACTIVITIES_ACCESSED,
        EDITOR_CHECKLIST_INSERTED,
        EDITOR_COLLABORATORS_ACCESSED,
        EDITOR_VERSIONS_ACCESSED,
        EDITOR_NOTE_PUBLISHED,
        EDITOR_NOTE_UNPUBLISHED,
        EDITOR_NOTE_PUBLISHED_URL_PRESSED,
        EDITOR_NOTE_CONTENT_SHARED,
        EDITOR_NOTE_EDITED,
        EDITOR_EMAIL_TAG_ADDED,
        EDITOR_EMAIL_TAG_REMOVED,
        EDITOR_TAG_ADDED,
        COLLABORATOR_ADDED,
        COLLABORATOR_REMOVED,
        EDITOR_TAG_REMOVED,
        EDITOR_NOTE_PINNED,
        EDITOR_NOTE_UNPINNED,
        LIST_NOTE_CREATED,
        LIST_NOTE_DELETED,
        LIST_TRASH_EMPTIED,
        LIST_NOTES_SEARCHED,
        LIST_TAG_VIEWED,
        LIST_NOTE_OPENED,
        LIST_TRASH_VIEWED,
        SETTINGS_PINLOCK_ENABLED,
        SETTINGS_LIST_CONDENSED_ENABLED,
        SETTINGS_ALPHABETICAL_SORT_ENABLED,
        SETTINGS_MARKDOWN_ENABLED,
        SETTINGS_THEME_UPDATED,
        SETTINGS_IMPORT_NOTES,
        SIDEBAR_SIDEBAR_PANNED,
        SIDEBAR_BUTTON_PRESSED,
        TAG_ROW_RENAMED,
        TAG_ROW_DELETED,
        TAG_CELL_PRESSED,
        TAG_MENU_RENAMED,
        TAG_MENU_DELETED,
        TAG_EDITOR_ACCESSED,
        RATINGS_SAW_PROMPT,
        RATINGS_RATED_APP,
        RATINGS_FEEDBACK_SCREEN_OPENED,
        RATINGS_DECLINED_TO_RATE_APP,
        RATINGS_LIKED_APP,
        RATINGS_DISLIKED_APP,
        RATINGS_FEEDBACK_SENT,
        RATINGS_FEEDBACK_CANCELED,
        ONE_PASSWORD_FAILED,
        ONE_PASSWORD_LOGIN,
        ONE_PASSWORD_SIGNUP,
        USER_ACCOUNT_CREATED,
        USER_ACCOUNT_DELETE_REQUESTED,
        USER_SIGNED_IN,
        USER_SIGNED_OUT,
        APPLICATION_OPENED,
        APPLICATION_CLOSED,
        WPCC_BUTTON_PRESSED,
        WPCC_LOGIN_SUCCEEDED,
        WPCC_LOGIN_FAILED,
        NOTE_SHARED_TO_WORDPRESS,
        NOTE_LIST_WIDGET_BUTTON_TAPPED,
        NOTE_LIST_WIDGET_DELETED,
        NOTE_LIST_WIDGET_FIRST_ADDED,
        NOTE_LIST_WIDGET_LAST_DELETED,
        NOTE_LIST_WIDGET_NOTE_TAPPED,
        NOTE_LIST_WIDGET_TAPPED,
        NOTE_LIST_WIDGET_SIGN_IN_TAPPED,
        NOTE_WIDGET_DELETED,
        NOTE_WIDGET_FIRST_ADDED,
        NOTE_WIDGET_LAST_DELETED,
        NOTE_WIDGET_NOTE_NOT_FOUND_TAPPED,
        NOTE_WIDGET_NOTE_TAPPED,
        NOTE_WIDGET_SIGN_IN_TAPPED,
        RECENT_SEARCH_TAPPED,
        SEARCH_EMPTY_TAPPED,
        SEARCH_MATCH_TAPPED,
        INTERNOTE_LINK_COPIED,
        INTERNOTE_LINK_CREATED,
        INTERNOTE_LINK_TAPPED,
        VERIFICATION_CONFIRM_BUTTON_TAPPED,
        VERIFICATION_CHANGE_EMAIL_BUTTON_TAPPED,
        VERIFICATION_RESEND_EMAIL_BUTTON_TAPPED,
        VERIFICATION_DISMISSED,
        SETTINGS_SEARCH_SORT_MODE,
        IAP_MONTHLY_BUTTON_TAPPED,
        IAP_YEARLY_BUTTON_TAPPED,
        IAP_UNKNOWN_BUTTON_TAPPED, // for other subscription duration options from Play Store not specifically handled in the client
        IAP_PURCHASE_COMPLETED,
        IAP_PLANS_DIALOG_DISMISSED
    }

    public interface Tracker {
        void track(Stat stat, String category, String label);

        void track(Stat stat, String category, String label, Map<String, ?> properties);

        void refreshMetadata(String username);

        void flush();
    }
}
