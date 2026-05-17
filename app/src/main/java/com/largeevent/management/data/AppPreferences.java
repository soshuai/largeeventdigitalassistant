package com.largeevent.management.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 全局简单偏好设置存储
 */
public class AppPreferences {

    private static final String PREF_NAME = "large_event_preferences";
    private static final String KEY_SERVER_URL = "key_server_url";
    private static final String KEY_SELECTED_SESSIONS = "key_selected_sessions";
    private static final String KEY_SELECTED_PERMISSIONS = "key_selected_permissions";
    private static final String KEY_SELECTED_VENUE_PERMISSIONS = "key_selected_venue_permissions";
    private static final String KEY_SELECTED_AREA_PERMISSIONS = "key_selected_area_permissions";
    private static final String KEY_SELECTED_LOCATION_ID = "key_selected_location_id";
    private static final String KEY_SELECTED_ZONE_ID = "key_selected_zone_id";
    private static final String KEY_LAST_PERSON_COUNT = "key_last_person_count";
    private static final String KEY_LAST_VEHICLE_COUNT = "key_last_vehicle_count";
    private static final String KEY_LAST_SYNC_STATUS = "key_last_sync_status";
    private static final String KEY_LAST_ACTIVE_ID = "key_last_active_id";
    private static final String KEY_DEVICE_CODE = "key_device_code";
    private static final String KEY_DEVICE_LOCATION = "key_device_location";

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getServerUrl(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String url = prefs.getString(KEY_SERVER_URL, "");
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        return ensureEndsWithSlash(url);
    }

    public static void setServerUrl(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        url = ensureEndsWithSlash(url);
        getPrefs(context)
                .edit()
                .putString(KEY_SERVER_URL, url)
                .apply();
    }

    public static Set<String> getSelectedSessions(Context context) {
        String activeId = getLastActiveId(context);
        return getSelectedSessions(context, activeId);
    }

    public static Set<String> getSelectedSessions(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return new HashSet<>();
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_SESSIONS + "_" + activeId;
        Set<String> values = prefs.getStringSet(key, null);
        if (values == null) {
            return new HashSet<>();
        }
        return new HashSet<>(values);
    }

    public static void setSelectedSessions(Context context, Set<String> sessions) {
        String activeId = getLastActiveId(context);
        setSelectedSessions(context, activeId, sessions);
    }

    public static void setSelectedSessions(Context context, String activeId, Set<String> sessions) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_SESSIONS + "_" + activeId;
        prefs.edit()
                .putStringSet(key,
                        sessions == null ? Collections.emptySet() : new HashSet<>(sessions))
                .apply();
    }

    public static Set<String> getSelectedPermissions(Context context) {
        return new HashSet<>(getLegacyPermissions(context));
    }

    public static void setSelectedPermissions(Context context, Set<String> permissions) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putStringSet(KEY_SELECTED_PERMISSIONS,
                        permissions == null ? Collections.emptySet() : new HashSet<>(permissions))
                .apply();
    }

    public static Set<String> getSelectedVenuePermissions(Context context) {
        String activeId = getLastActiveId(context);
        return getSelectedVenuePermissions(context, activeId);
    }

    public static Set<String> getSelectedVenuePermissions(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return new HashSet<>();
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_VENUE_PERMISSIONS + "_" + activeId;
        Set<String> values = prefs.getStringSet(key, null);
        if (values == null) {
            return new HashSet<>();
        }
        return new HashSet<>(values);
    }

    public static void setSelectedVenuePermissions(Context context, Set<String> permissions) {
        String activeId = getLastActiveId(context);
        setSelectedVenuePermissions(context, activeId, permissions);
    }

    public static void setSelectedVenuePermissions(Context context, String activeId, Set<String> permissions) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_VENUE_PERMISSIONS + "_" + activeId;
        prefs.edit().putStringSet(key, permissions == null ?
                Collections.emptySet() : new HashSet<>(permissions)).apply();
    }

    public static Set<String> getSelectedAreaPermissions(Context context) {
        String activeId = getLastActiveId(context);
        return getSelectedAreaPermissions(context, activeId);
    }

    public static Set<String> getSelectedAreaPermissions(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return new HashSet<>();
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_AREA_PERMISSIONS + "_" + activeId;
        Set<String> values = prefs.getStringSet(key, null);
        if (values == null) {
            return new HashSet<>();
        }
        return new HashSet<>(values);
    }

    public static void setSelectedAreaPermissions(Context context, Set<String> permissions) {
        String activeId = getLastActiveId(context);
        setSelectedAreaPermissions(context, activeId, permissions);
    }

    public static void setSelectedAreaPermissions(Context context, String activeId, Set<String> permissions) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_AREA_PERMISSIONS + "_" + activeId;
        prefs.edit()
                .putStringSet(key,
                        permissions == null ? Collections.emptySet() : new HashSet<>(permissions))
                .apply();
    }

    public static void setLastSyncResult(Context context, int personCount, int vehicleCount, String message) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putInt(KEY_LAST_PERSON_COUNT, Math.max(personCount, 0))
                .putInt(KEY_LAST_VEHICLE_COUNT, Math.max(vehicleCount, 0))
                .putString(KEY_LAST_SYNC_STATUS, message == null ? "" : message)
                .apply();
    }

    public static int getLastPersonCount(Context context) {
        return getPrefs(context).getInt(KEY_LAST_PERSON_COUNT, 0);
    }

    public static int getLastVehicleCount(Context context) {
        return getPrefs(context).getInt(KEY_LAST_VEHICLE_COUNT, 0);
    }

    public static String getLastSyncStatus(Context context) {
        return getPrefs(context).getString(KEY_LAST_SYNC_STATUS, "");
    }

    public static void setLastActiveId(Context context, String activeId) {
        getPrefs(context).edit().putString(KEY_LAST_ACTIVE_ID,
                TextUtils.isEmpty(activeId) ? "" : activeId).apply();
    }

    public static String getLastActiveId(Context context) {
        return getPrefs(context).getString(KEY_LAST_ACTIVE_ID, "");
    }

    /**
     * 设置设备编码
     */
    public static void setDeviceCode(Context context, String deviceCode) {
        getPrefs(context).edit().putString(KEY_DEVICE_CODE,
                TextUtils.isEmpty(deviceCode) ? "" : deviceCode).apply();
    }

    /**
     * 获取设备编码
     */
    public static String getDeviceCode(Context context) {
        return getPrefs(context).getString(KEY_DEVICE_CODE, "");
    }

    /**
     * 设置设备位置
     */
    public static void setDeviceLocation(Context context, String location) {
        getPrefs(context).edit().putString(KEY_DEVICE_LOCATION,
                TextUtils.isEmpty(location) ? "" : location).apply();
    }

    /**
     * 获取设备位置
     */
    public static String getDeviceLocation(Context context) {
        return getPrefs(context).getString(KEY_DEVICE_LOCATION, "");
    }

    /**
     * 获取选中的位置ID(按活动隔离)
     */
    public static String getSelectedLocationId(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return null;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_LOCATION_ID + "_" + activeId;
        return prefs.getString(key, null);
    }

    /**
     * 设置选中的位置ID(按活动隔离)
     */
    public static void setSelectedLocationId(Context context, String activeId, String locationId) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_LOCATION_ID + "_" + activeId;
        prefs.edit().putString(key, locationId).apply();
    }

    /**
     * 获取选中的分区ID(按活动隔离)
     */
    public static String getSelectedZoneId(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return null;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_ZONE_ID + "_" + activeId;
        return prefs.getString(key, null);
    }

    /**
     * 设置选中的分区ID(按活动隔离)
     */
    public static void setSelectedZoneId(Context context, String activeId, String zoneId) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_ZONE_ID + "_" + activeId;
        prefs.edit().putString(key, zoneId).apply();
    }

    /**
     * 清空所有权限选择（用于测试或重置）
     */
    public static void clearAllPermissions(Context context) {
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .remove(KEY_SELECTED_SESSIONS)
                .remove(KEY_SELECTED_PERMISSIONS)
                .remove(KEY_SELECTED_VENUE_PERMISSIONS)
                .remove(KEY_SELECTED_AREA_PERMISSIONS)
                .apply();
    }

    private static Set<String> getLegacyPermissions(Context context) {
        SharedPreferences prefs = getPrefs(context);
        Set<String> values = prefs.getStringSet(KEY_SELECTED_PERMISSIONS, null);
        if (values == null) {
            return new HashSet<>();
        }
        return new HashSet<>(values);
    }

    private static String ensureEndsWithSlash(String url) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }
        if (url.endsWith("/")) {
            return url;
        }
        return url + "/";
    }
}


