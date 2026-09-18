package com.largeevent.management.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.Nullable;

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
    private static final String KEY_SELECTED_CERT_ZONE_PERMISSIONS = "key_selected_cert_zone_permissions";
    private static final String KEY_DEVICE_MATRIX_VENUE = "key_device_matrix_venue";
    private static final String KEY_DEVICE_MATRIX_SPORT = "key_device_matrix_sport";
    /**
     * 区域权限（venueArea / areaPrivileges）；新 key，避免与旧错误语义混用
     */
    private static final String KEY_DEVICE_MATRIX_AREA = "key_device_matrix_area";
    /**
     * 分区权限（venuePartition / zonePrivileges）
     */
    private static final String KEY_DEVICE_MATRIX_PARTITION = "key_device_matrix_partition";
    /**
     * 停车通行码（park / parkingCode）
     */
    private static final String KEY_DEVICE_MATRIX_PARK = "key_device_matrix_park";
    /**
     * 最近一次 getMatrixAuthInfoList / getCarMatrixAuthList 原始 JSON（展示用）
     */
    private static final String KEY_DEVICE_MATRIX_JSON = "key_device_matrix_json";
    private static final String KEY_ACTIVATION_CHECK = "key_activation_check_enabled";
    private static final String KEY_EQP_TYPE = "key_eqp_type";
    private static final String KEY_SUB_UNIT = "key_sub_unit_name";
    private static final String KEY_DEVICE_PERM_CONFIGURED = "key_device_perm_configured";
    private static final String KEY_SELECTED_LOCATION_ID = "key_selected_location_id";
    private static final String KEY_SELECTED_ZONE_ID = "key_selected_zone_id";
    /**
     * 已保存设置并完成设备注册+权限拉取的活动与位置/分区（value: locationId|zoneId）
     */
    private static final String KEY_DEVICE_AUTH_SYNCED = "key_device_auth_synced_";
    private static final String KEY_LAST_PERSON_COUNT = "key_last_person_count";
    private static final String KEY_LAST_VEHICLE_COUNT = "key_last_vehicle_count";
    private static final String KEY_LAST_SYNC_STATUS = "key_last_sync_status";
    private static final String KEY_LAST_ACTIVE_ID = "key_last_active_id";
    private static final String KEY_DEVICE_CODE = "key_device_code";
    private static final String KEY_DEVICE_LOCATION = "key_device_location";
    /**
     * 活动设置页当前选择的业务模块：1 人证 / 2 车证
     */
    private static final String KEY_MODULE_TYPE = "key_module_type";

    private static final boolean test = false;

    private static String scopedKey(String prefix, String activeId, int moduleType) {
        return prefix + "_" + activeId + "_m" + ModuleType.normalize(moduleType);
    }

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

    public static Set<String> getSelectedCertZonePermissions(Context context) {
        String activeId = getLastActiveId(context);
        return getSelectedCertZonePermissions(context, activeId);
    }

    public static Set<String> getSelectedCertZonePermissions(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return new HashSet<>();
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_CERT_ZONE_PERMISSIONS + "_" + activeId;
        Set<String> values = prefs.getStringSet(key, null);
        if (values == null) {
            return new HashSet<>();
        }
        return new HashSet<>(values);
    }

    public static void setSelectedCertZonePermissions(Context context, Set<String> permissions) {
        String activeId = getLastActiveId(context);
        setSelectedCertZonePermissions(context, activeId, permissions);
    }

    public static void setSelectedCertZonePermissions(Context context, String activeId, Set<String> permissions) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences prefs = getPrefs(context);
        String key = KEY_SELECTED_CERT_ZONE_PERMISSIONS + "_" + activeId;
        prefs.edit()
                .putStringSet(key,
                        permissions == null ? Collections.emptySet() : new HashSet<>(permissions))
                .apply();
    }

    /**
     * 缓存最近一次 getMatrixAuthInfoList 解析出的设备权限（按人证/车证隔离）
     */
    public static void setDeviceMatrixAuthCodes(
            Context context,
            String activeId,
            int moduleType,
            DevicePermissionHelper.PermissionSets sets) {
        if (TextUtils.isEmpty(activeId) || sets == null) {
            return;
        }
        int module = ModuleType.normalize(moduleType);
        SharedPreferences prefs = getPrefs(context);
        prefs.edit()
                .putStringSet(scopedKey(KEY_DEVICE_MATRIX_VENUE, activeId, module), new HashSet<>(sets.venueCodes))
                .putStringSet(scopedKey(KEY_DEVICE_MATRIX_SPORT, activeId, module), new HashSet<>(sets.sportCodes))
                .putStringSet(scopedKey(KEY_DEVICE_MATRIX_AREA, activeId, module), new HashSet<>(sets.areaCodes))
                .putStringSet(scopedKey(KEY_DEVICE_MATRIX_PARTITION, activeId, module), new HashSet<>(sets.partitionCodes))
                .putStringSet(scopedKey(KEY_DEVICE_MATRIX_PARK, activeId, module), new HashSet<>(sets.parkCodes))
                .putBoolean(scopedKey(KEY_DEVICE_PERM_CONFIGURED, activeId, module),
                        module == ModuleType.VEHICLE ? sets.hasCarConfigured() : sets.hasAnyConfigured())
                .apply();
    }

    /**
     * 缓存 matrixAuth 列表 JSON，供设置页按接口数据展示权限芯片
     */
    public static void setDeviceMatrixAuthJson(
            Context context, String activeId, int moduleType, @Nullable String json) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        int module = ModuleType.normalize(moduleType);
        getPrefs(context).edit()
                .putString(scopedKey(KEY_DEVICE_MATRIX_JSON, activeId, module), json == null ? "" : json)
                .apply();
    }

    @Nullable
    public static String getDeviceMatrixAuthJson(Context context, String activeId, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return null;
        }
        int module = ModuleType.normalize(moduleType);
        return getPrefs(context).getString(scopedKey(KEY_DEVICE_MATRIX_JSON, activeId, module), null);
    }

    /**
     * 清空指定模块的设备权限缓存（注册失败时使用）
     */
    public static void clearDeviceMatrixAuth(Context context, String activeId, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        int module = ModuleType.normalize(moduleType);
        getPrefs(context).edit()
                .remove(scopedKey(KEY_DEVICE_MATRIX_VENUE, activeId, module))
                .remove(scopedKey(KEY_DEVICE_MATRIX_SPORT, activeId, module))
                .remove(scopedKey(KEY_DEVICE_MATRIX_AREA, activeId, module))
                .remove(scopedKey(KEY_DEVICE_MATRIX_PARTITION, activeId, module))
                .remove(scopedKey(KEY_DEVICE_MATRIX_PARK, activeId, module))
                .remove(scopedKey(KEY_DEVICE_MATRIX_JSON, activeId, module))
                .remove(scopedKey(KEY_DEVICE_PERM_CONFIGURED, activeId, module))
                .remove(KEY_DEVICE_AUTH_SYNCED + activeId + "_m" + module)
                .apply();
    }

    /**
     * @deprecated 使用带 moduleType 的重载
     */
    public static void setDeviceMatrixAuthCodes(
            Context context,
            String activeId,
            DevicePermissionHelper.PermissionSets sets) {
        setDeviceMatrixAuthCodes(context, activeId, ModuleType.PERSON, sets);
    }

    public static DevicePermissionHelper.PermissionSets getDeviceMatrixAuthCodes(
            Context context, String activeId, int moduleType) {
        DevicePermissionHelper.PermissionSets sets = new DevicePermissionHelper.PermissionSets();
        if (TextUtils.isEmpty(activeId)) {
            return sets;
        }
        int module = ModuleType.normalize(moduleType);
        SharedPreferences prefs = getPrefs(context);
        Set<String> venues = prefs.getStringSet(scopedKey(KEY_DEVICE_MATRIX_VENUE, activeId, module), null);
        Set<String> sports = prefs.getStringSet(scopedKey(KEY_DEVICE_MATRIX_SPORT, activeId, module), null);
        Set<String> areas = prefs.getStringSet(scopedKey(KEY_DEVICE_MATRIX_AREA, activeId, module), null);
        Set<String> partitions = prefs.getStringSet(scopedKey(KEY_DEVICE_MATRIX_PARTITION, activeId, module), null);
        Set<String> parks = prefs.getStringSet(scopedKey(KEY_DEVICE_MATRIX_PARK, activeId, module), null);
        // 无 AREA key 表示尚未按「区域/分区」纠正后的语义写入，忽略旧 PARTITION，避免误用
        if (areas == null) {
            partitions = null;
        }
        // 兼容旧版未分模块的缓存（仅场馆/分项）
        if (venues == null && sports == null && areas == null && partitions == null
                && module == ModuleType.PERSON) {
            venues = prefs.getStringSet(KEY_DEVICE_MATRIX_VENUE + "_" + activeId, null);
            sports = prefs.getStringSet(KEY_DEVICE_MATRIX_SPORT + "_" + activeId, null);
        }
        if (venues != null) {
            addFilteredPrivilegeCodes(sets.venueCodes, venues);
        }
        if (sports != null) {
            addFilteredPrivilegeCodes(sets.sportCodes, sports);
        }
        if (areas != null) {
            addFilteredPrivilegeCodes(sets.areaCodes, areas);
        }
        if (partitions != null) {
            addFilteredPrivilegeCodes(sets.partitionCodes, partitions);
        }
        if (parks != null) {
            addFilteredPrivilegeCodes(sets.parkCodes, parks);
        }
        return sets;
    }

    private static void addFilteredPrivilegeCodes(Set<String> target, Set<String> source) {
        if (target == null || source == null) {
            return;
        }
        for (String code : source) {
            if (!DevicePermissionHelper.isPlaceholderPrivilegeCode(code)) {
                target.add(code);
            }
        }
    }

    public static DevicePermissionHelper.PermissionSets getDeviceMatrixAuthCodes(
            Context context, String activeId) {
        return getDeviceMatrixAuthCodes(context, activeId, ModuleType.PERSON);
    }

    public static boolean isActivationCheckEnabled(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return false;
        }
        return getPrefs(context).getBoolean(KEY_ACTIVATION_CHECK + "_" + activeId, false);
    }

    public static void setActivationCheckEnabled(Context context, String activeId, boolean enabled) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit().putBoolean(KEY_ACTIVATION_CHECK + "_" + activeId, enabled).apply();
    }

    public static String getEqpType(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return PersonVerificationHelper.EQP_HANDHELD;
        }
        String type = getPrefs(context).getString(KEY_EQP_TYPE + "_" + activeId, null);
        return TextUtils.isEmpty(type) ? PersonVerificationHelper.EQP_HANDHELD : type;
    }

    public static void setEqpType(Context context, String activeId, String eqpType) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit()
                .putString(KEY_EQP_TYPE + "_" + activeId,
                        TextUtils.isEmpty(eqpType) ? PersonVerificationHelper.EQP_HANDHELD : eqpType)
                .apply();
    }

    @Nullable
    public static String getSelectedSubUnitName(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return null;
        }
        return getPrefs(context).getString(KEY_SUB_UNIT + "_" + activeId, null);
    }

    public static void setSelectedSubUnitName(Context context, String activeId, @Nullable String name) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        SharedPreferences.Editor editor = getPrefs(context).edit();
        String key = KEY_SUB_UNIT + "_" + activeId;
        if (TextUtils.isEmpty(name)) {
            editor.remove(key);
        } else {
            editor.putString(key, name);
        }
        editor.apply();
    }

    public static void setDevicePermissionConfigured(Context context, String activeId, int moduleType,
                                                     boolean configured) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit()
                .putBoolean(scopedKey(KEY_DEVICE_PERM_CONFIGURED, activeId, moduleType), configured)
                .apply();
    }

    public static void setDevicePermissionConfigured(Context context, String activeId, boolean configured) {
        setDevicePermissionConfigured(context, activeId, ModuleType.PERSON, configured);
    }

    public static boolean isDevicePermissionConfigured(Context context, String activeId, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return false;
        }
        int module = ModuleType.normalize(moduleType);
        SharedPreferences prefs = getPrefs(context);
        if (prefs.getBoolean(scopedKey(KEY_DEVICE_PERM_CONFIGURED, activeId, module), false)) {
            return true;
        }
        DevicePermissionHelper.PermissionSets sets = getDeviceMatrixAuthCodes(context, activeId, module);
        return module == ModuleType.VEHICLE ? sets.hasCarConfigured() : sets.hasPersonConfigured();
    }

    public static boolean isDevicePermissionConfigured(Context context, String activeId) {
        return isDevicePermissionConfigured(context, activeId, ModuleType.PERSON);
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
     * 解析活动代码 eventCode（updateCardInfo 必填）。
     * 优先取当前活动 {@link com.largeevent.management.model.BasicInfo.ActiveModel#accreditationCode}，
     * 否则回退为 activityId。
     */
    @Nullable
    public static String resolveEventCode(
            Context context, @Nullable com.largeevent.management.model.BasicInfo basicInfo) {
        String activityId = getLastActiveId(context);
        if (basicInfo != null && !TextUtils.isEmpty(activityId)) {
            for (com.largeevent.management.model.BasicInfo.ActiveModel model : basicInfo.getActiveModels()) {
                if (model == null || model.id == null) {
                    continue;
                }
                if (activityId.equals(model.id) && !TextUtils.isEmpty(model.accreditationCode)) {
                    return model.accreditationCode.trim();
                }
            }
        }
        return TextUtils.isEmpty(activityId) ? null : activityId.trim();
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
     * 确保已有设备编码；为空时生成并持久化
     */
    public static String ensureDeviceCode(Context context) {
        String code = getDeviceCode(context);
        if (test) {
            code = "DEV_1788616993593";
        }
        if (TextUtils.isEmpty(code)) {
            code = "DEV_" + System.currentTimeMillis();
            setDeviceCode(context, code);
        }
        return code;
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
     * 获取选中的位置ID(按活动 + 人证/车证模块隔离)
     */
    public static String getSelectedLocationId(Context context, String activeId, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return null;
        }
        SharedPreferences prefs = getPrefs(context);
        int module = ModuleType.normalize(moduleType);
        String value = prefs.getString(scopedKey(KEY_SELECTED_LOCATION_ID, activeId, module), null);
        if (TextUtils.isEmpty(value) && module == ModuleType.PERSON) {
            value = prefs.getString(KEY_SELECTED_LOCATION_ID + "_" + activeId, null);
        }
        return value;
    }

    public static String getSelectedLocationId(Context context, String activeId) {
        return getSelectedLocationId(context, activeId, ModuleType.PERSON);
    }

    public static void setSelectedLocationId(Context context, String activeId, int moduleType,
                                             String locationId) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit()
                .putString(scopedKey(KEY_SELECTED_LOCATION_ID, activeId, moduleType), locationId)
                .apply();
    }

    public static void setSelectedLocationId(Context context, String activeId, String locationId) {
        setSelectedLocationId(context, activeId, ModuleType.PERSON, locationId);
    }

    public static String getSelectedZoneId(Context context, String activeId, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return null;
        }
        SharedPreferences prefs = getPrefs(context);
        int module = ModuleType.normalize(moduleType);
        String value = prefs.getString(scopedKey(KEY_SELECTED_ZONE_ID, activeId, module), null);
        if (TextUtils.isEmpty(value) && module == ModuleType.PERSON) {
            value = prefs.getString(KEY_SELECTED_ZONE_ID + "_" + activeId, null);
        }
        return value;
    }

    public static String getSelectedZoneId(Context context, String activeId) {
        return getSelectedZoneId(context, activeId, ModuleType.PERSON);
    }

    public static void setSelectedZoneId(Context context, String activeId, int moduleType, String zoneId) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit()
                .putString(scopedKey(KEY_SELECTED_ZONE_ID, activeId, moduleType), zoneId)
                .apply();
    }

    public static void setSelectedZoneId(Context context, String activeId, String zoneId) {
        setSelectedZoneId(context, activeId, ModuleType.PERSON, zoneId);
    }

    public static int getModuleType(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return ModuleType.PERSON;
        }
        return ModuleType.normalize(
                getPrefs(context).getInt(KEY_MODULE_TYPE + "_" + activeId, ModuleType.PERSON));
    }

    public static void setModuleType(Context context, String activeId, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit()
                .putInt(KEY_MODULE_TYPE + "_" + activeId, ModuleType.normalize(moduleType))
                .apply();
    }

    public static boolean isDeviceAuthSynced(
            Context context, String activeId, int moduleType, String locationId, String zoneId) {
        if (TextUtils.isEmpty(activeId) || TextUtils.isEmpty(locationId)
                || TextUtils.isEmpty(zoneId)) {
            return false;
        }
        int module = ModuleType.normalize(moduleType);
        String saved = getPrefs(context).getString(
                KEY_DEVICE_AUTH_SYNCED + activeId + "_m" + module, null);
        return buildDeviceAuthSyncKey(locationId, zoneId).equals(saved);
    }

    public static boolean isDeviceAuthSynced(
            Context context, String activeId, String locationId, String zoneId) {
        return isDeviceAuthSynced(context, activeId, ModuleType.PERSON, locationId, zoneId);
    }

    public static void setDeviceAuthSynced(
            Context context, String activeId, int moduleType, String locationId, String zoneId) {
        if (TextUtils.isEmpty(activeId) || TextUtils.isEmpty(locationId)
                || TextUtils.isEmpty(zoneId)) {
            return;
        }
        int module = ModuleType.normalize(moduleType);
        getPrefs(context).edit()
                .putString(KEY_DEVICE_AUTH_SYNCED + activeId + "_m" + module,
                        buildDeviceAuthSyncKey(locationId, zoneId))
                .apply();
    }

    public static void setDeviceAuthSynced(
            Context context, String activeId, String locationId, String zoneId) {
        setDeviceAuthSynced(context, activeId, ModuleType.PERSON, locationId, zoneId);
    }

    /**
     * 切换活动时清除该活动下的同步标记，以便重新选择位置/分区后拉取权限
     */
    public static void clearDeviceAuthSynced(Context context, String activeId) {
        if (TextUtils.isEmpty(activeId)) {
            return;
        }
        getPrefs(context).edit().remove(KEY_DEVICE_AUTH_SYNCED + activeId).apply();
    }

    private static String buildDeviceAuthSyncKey(String locationId, String zoneId) {
        return locationId.trim() + "|" + zoneId.trim();
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
                .remove(KEY_SELECTED_CERT_ZONE_PERMISSIONS)
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


