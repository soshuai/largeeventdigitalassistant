package com.largeevent.management.data;

import android.content.Context;
import android.text.TextUtils;

import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.model.BasicInfo.MatrixAuthInfo;
import com.largeevent.management.model.BasicInfo.VenueInfo;
import com.largeevent.management.network.dto.MatrixAuthInfoDTO;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 设备侧通行权限（场馆 / 区域 / 分区）解析与证件比对。
 * <p>
 * 约定（与 getBasicInfo 字典及后台 matrixAuth 实际数据一致）：
 * <ul>
 *   <li>场馆：venue / venuePrivileges ↔ venueInfoList</li>
 *   <li>区域：venuePartition / zonePrivileges ↔ personCertZoneList（dictCode 1~9）</li>
 *   <li>分区：venueArea / areaPrivileges ↔ personCertAreaList（红/白/蓝）</li>
 * </ul>
 * 注：字段名 venueArea/venuePartition 与中文「区域/分区」字面相反，以字典列表为准。
 */
public final class DevicePermissionHelper {

    /** 后台「全部权限」通配码，与 ALL 等价 */
    public static final String CODE_ALL = "ALL";
    public static final String CODE_INF = "INF";

    private DevicePermissionHelper() {
    }

    public static class PermissionSets {
        public final Set<String> venueCodes = new HashSet<>();
        /** 分项 code（matrixAuth.sportProject） */
        public final Set<String> sportCodes = new HashSet<>();
        /** 区域权限 code（matrixAuth.venuePartition → personCertZoneList） */
        public final Set<String> zoneCodes = new HashSet<>();
        /** 分区权限 code（matrixAuth.venueArea → personCertAreaList） */
        public final Set<String> partitionCodes = new HashSet<>();

        public boolean isEmpty() {
            return venueCodes.isEmpty() && sportCodes.isEmpty()
                    && zoneCodes.isEmpty() && partitionCodes.isEmpty();
        }

        public boolean hasAnyConfigured() {
            return !isEmpty();
        }
    }

    /**
     * 解析设备通行权限：按人证/车证模块取 getMatrixAuthInfoList 缓存，
     * 再回退 getBasicInfo.matrixAuthInfoList。
     */
    public static PermissionSets resolveDevicePermissions(
            Context context, String activeId, BasicInfo basicInfo, int moduleType) {
        int module = ModuleType.normalize(moduleType);
        if (!TextUtils.isEmpty(activeId)) {
            PermissionSets cached = AppPreferences.getDeviceMatrixAuthCodes(context, activeId, module);
            if (!cached.isEmpty()) {
                return cached;
            }
        }
        if (basicInfo != null) {
            return fromMatrixAuthInfoList(basicInfo.getMatrixAuthInfoList());
        }
        return new PermissionSets();
    }

    public static PermissionSets resolveDevicePermissions(
            Context context, String activeId, BasicInfo basicInfo) {
        return resolveDevicePermissions(context, activeId, basicInfo, ModuleType.PERSON);
    }

    /** 从 getMatrixAuthInfoList 接口数据解析设备权限 code 集合 */
    public static PermissionSets fromMatrixAuthDtoList(@Nullable List<MatrixAuthInfoDTO> authList) {
        PermissionSets sets = new PermissionSets();
        if (authList == null) {
            return sets;
        }
        for (MatrixAuthInfoDTO auth : authList) {
            if (auth == null) {
                continue;
            }
            addPrivilegeTokens(sets.venueCodes, auth.venue);
            addPrivilegeTokens(sets.venueCodes, auth.venueVal);
            addPrivilegeTokens(sets.sportCodes, auth.sportProject);
            addPrivilegeTokens(sets.sportCodes, auth.sportProjectVal);
            // 后台数据：venueArea=红/白/蓝(分区)，venuePartition=1~9(区域)
            addPrivilegeTokens(sets.partitionCodes, auth.venueArea);
            addPrivilegeTokens(sets.partitionCodes, auth.venueAreaVal);
            addPrivilegeTokens(sets.zoneCodes, auth.venuePartition);
            addPrivilegeTokens(sets.zoneCodes, auth.venuePartitionVal);
        }
        return sets;
    }

    /** 将后台权限字段拆成可匹配 token（支持逗号 / 波浪号，并保留展示名） */
    public static void addPrivilegeTokens(Set<String> target, @Nullable String raw) {
        if (target == null || TextUtils.isEmpty(raw)) {
            return;
        }
        String trimmed = raw.trim();
        target.add(trimmed);
        target.addAll(splitPrivileges(trimmed));
    }

    public static PermissionSets fromSavedPreferences(
            Context context, String activeId, BasicInfo basicInfo) {
        PermissionSets sets = new PermissionSets();
        if (basicInfo == null || TextUtils.isEmpty(activeId)) {
            return sets;
        }
        sets.venueCodes.addAll(resolveDictCodes(
                basicInfo.getVenueInfoList(),
                AppPreferences.getSelectedVenuePermissions(context, activeId)));
        sets.partitionCodes.addAll(resolveDictCodes(
                basicInfo.getPersonCertAreaList(),
                AppPreferences.getSelectedAreaPermissions(context, activeId)));
        sets.zoneCodes.addAll(resolveDictCodes(
                basicInfo.getPersonCertZoneList(),
                AppPreferences.getSelectedCertZonePermissions(context, activeId)));
        return sets;
    }

    public static PermissionSets fromMatrixAuthInfoList(List<MatrixAuthInfo> authList) {
        PermissionSets sets = new PermissionSets();
        if (authList == null) {
            return sets;
        }
        for (MatrixAuthInfo auth : authList) {
            if (auth == null) {
                continue;
            }
            addPrivilegeTokens(sets.venueCodes, auth.venue);
            addPrivilegeTokens(sets.venueCodes, auth.venueVal);
            addPrivilegeTokens(sets.sportCodes, auth.sportProject);
            addPrivilegeTokens(sets.sportCodes, auth.sportProjectVal);
            addPrivilegeTokens(sets.partitionCodes, auth.venueArea);
            addPrivilegeTokens(sets.partitionCodes, auth.venueAreaVal);
            addPrivilegeTokens(sets.zoneCodes, auth.venuePartition);
            addPrivilegeTokens(sets.zoneCodes, auth.venuePartitionVal);
        }
        return sets;
    }

    /**
     * 校验证件是否满足设备权限（场馆/分项/分区/区域均需满足；未配置的类型视为不限制）。
     * 证件某一维度的 ALL/INF 仅在该维度且设备已配置该维度时视为满足，不会豁免其它维度。
     *
     * @param strictEmptyDevice true 时设备未配置任何权限则拒绝（须先在设置页配置）
     */
    public static boolean certificateMatchesDevice(
            String venuePrivileges,
            String areaPrivileges,
            String zonePrivileges,
            @Nullable String sportPrivileges,
            PermissionSets device,
            boolean strictEmptyDevice) {
        if (device == null) {
            return !strictEmptyDevice;
        }
        if (device.isEmpty()) {
            return !strictEmptyDevice;
        }

        Set<String> certVenues = splitPrivileges(venuePrivileges);
        Set<String> certPartitions = splitPrivileges(areaPrivileges);
        Set<String> certZones = splitPrivileges(zonePrivileges);
        Set<String> certSports = splitPrivileges(sportPrivileges);

        // 各维度独立校验：证件在「场馆」为 ALL 不代表「区域」也满足设备要求的 Z01 等
        boolean venueOk = device.venueCodes.isEmpty()
                || hasFullPrivilege(certVenues)
                || intersects(certVenues, device.venueCodes);
        boolean sportOk = device.sportCodes.isEmpty()
                || hasFullPrivilege(certSports)
                || intersects(certSports, device.sportCodes);
        boolean zoneOk = device.zoneCodes.isEmpty()
                || hasFullPrivilege(certZones)
                || intersects(certZones, device.zoneCodes);
        boolean partitionOk = device.partitionCodes.isEmpty()
                || hasFullPrivilege(certPartitions)
                || intersects(certPartitions, device.partitionCodes);

        return venueOk && sportOk && zoneOk && partitionOk;
    }

    /** @deprecated 使用带 sportPrivileges 的重载 */
    public static boolean certificateMatchesDevice(
            String venuePrivileges,
            String areaPrivileges,
            String zonePrivileges,
            PermissionSets device) {
        return certificateMatchesDevice(
                venuePrivileges, areaPrivileges, zonePrivileges, null, device, false);
    }

    /**
     * 将证件权限 code 列表转为展示名称（逗号分隔的 dictValue），未匹配则保留原 code。
     */
    @Nullable
    public static String resolvePrivilegeDisplayNames(
            @Nullable String privilegeCodes, @Nullable List<VenueInfo> dictList) {
        if (TextUtils.isEmpty(privilegeCodes)) {
            return null;
        }
        String trimmed = privilegeCodes.trim();
        if (isFullPrivilegeCode(trimmed)) {
            return "全部";
        }
        List<String> names = new ArrayList<>();
        for (String code : splitPrivileges(privilegeCodes)) {
            if (isFullPrivilegeCode(code)) {
                names.add("全部");
                continue;
            }
            String name = lookupDictValue(dictList, code);
            names.add(!TextUtils.isEmpty(name) ? name : code);
        }
        if (names.isEmpty()) {
            return null;
        }
        return TextUtils.join("、", names);
    }

    @Nullable
    private static String lookupDictValue(@Nullable List<VenueInfo> dictList, String code) {
        if (dictList == null || TextUtils.isEmpty(code)) {
            return null;
        }
        for (VenueInfo item : dictList) {
            if (item != null && code.equals(item.dictCode) && !TextUtils.isEmpty(item.dictValue)) {
                return item.dictValue.trim();
            }
        }
        return null;
    }

    /**
     * 解析证件权限 code 串。后台常见格式：
     * <ul>
     *   <li>波浪号分隔：{@code ~2~3~4}（zonePrivileges / areaPrivileges 等）</li>
     *   <li>逗号分隔：{@code BQG,LQZX}</li>
     *   <li>ALL / INF（全部权限）</li>
     * </ul>
     */
    public static Set<String> splitPrivilegeTokens(@Nullable String raw) {
        return splitPrivileges(raw);
    }

    private static Set<String> splitPrivileges(String raw) {
        Set<String> set = new LinkedHashSet<>();
        if (TextUtils.isEmpty(raw)) {
            return set;
        }
        String trimmed = raw.trim();
        if (isFullPrivilegeCode(trimmed)) {
            set.add(trimmed.toUpperCase(Locale.ROOT));
            return set;
        }
        if (trimmed.indexOf('~') >= 0) {
            for (String part : trimmed.split("~")) {
                String code = part.trim();
                if (!TextUtils.isEmpty(code)) {
                    set.add(code);
                }
            }
            return set;
        }
        for (String part : trimmed.split(",")) {
            String code = part.trim();
            if (!TextUtils.isEmpty(code)) {
                set.add(code);
            }
        }
        return set;
    }

    /** INF、ALL 均表示该维度或证件具备全部权限 */
    public static boolean isFullPrivilegeCode(@Nullable String code) {
        if (TextUtils.isEmpty(code)) {
            return false;
        }
        String c = code.trim();
        return CODE_ALL.equalsIgnoreCase(c) || CODE_INF.equalsIgnoreCase(c);
    }

    private static boolean hasFullPrivilege(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return false;
        }
        for (String code : codes) {
            if (isFullPrivilegeCode(code)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersects(Set<String> certSet, Set<String> deviceSet) {
        for (String code : deviceSet) {
            if (certSet.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> resolveDictCodes(List<VenueInfo> dictList, Set<String> savedSelections) {
        Set<String> codes = new HashSet<>();
        if (dictList == null || savedSelections == null || savedSelections.isEmpty()) {
            return codes;
        }
        for (VenueInfo item : dictList) {
            if (item == null) {
                continue;
            }
            String code = item.dictCode != null ? item.dictCode.trim() : "";
            String value = item.dictValue != null ? item.dictValue.trim() : "";
            if (!TextUtils.isEmpty(code) && savedSelections.contains(code)) {
                codes.add(code);
            } else if (!TextUtils.isEmpty(value) && savedSelections.contains(value)
                    && !TextUtils.isEmpty(code)) {
                codes.add(code);
            }
        }
        return codes;
    }
}
