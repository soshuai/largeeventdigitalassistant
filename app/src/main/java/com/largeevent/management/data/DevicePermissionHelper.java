package com.largeevent.management.data;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

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
 * 约定（与 getBasicInfo 字典、getActiveUser、matrixAuth 一致）：
 * <ul>
 *   <li>场馆：venue / venuePrivileges ↔ venueInfoList</li>
 *   <li>区域：venueArea / areaPrivileges ↔ personCertAreaList</li>
 *   <li>分区：venuePartition / zonePrivileges ↔ personCertZoneList</li>
 * </ul>
 */
public final class DevicePermissionHelper {

    /** 后台「全部权限」通配码，与 ALL 等价 */
    public static final String CODE_ALL = "ALL";
    public static final String CODE_INF = "INF";
    private static final String TAG = "DevicePermission";

    private DevicePermissionHelper() {
    }

    public static final class MatchResult {
        public final boolean matched;
        @Nullable public final String failReason;

        MatchResult(boolean matched, @Nullable String failReason) {
            this.matched = matched;
            this.failReason = failReason;
        }

        static MatchResult ok() {
            return new MatchResult(true, null);
        }

        static MatchResult fail(String reason) {
            return new MatchResult(false, reason);
        }
    }

    public static class PermissionSets {
        public final Set<String> venueCodes = new HashSet<>();
        /** 分项 code（matrixAuth.sportProject） */
        public final Set<String> sportCodes = new HashSet<>();
        /** 区域权限 code（matrixAuth.venueArea → personCertAreaList / areaPrivileges） */
        public final Set<String> areaCodes = new HashSet<>();
        /** 分区权限 code（matrixAuth.venuePartition → personCertZoneList / zonePrivileges） */
        public final Set<String> partitionCodes = new HashSet<>();
        /** 停车通行码（matrixAuth.park → 车证 parkingCode） */
        public final Set<String> parkCodes = new HashSet<>();

        public boolean isEmpty() {
            return isPersonEmpty();
        }

        /** 人证设备权限：仅场馆 + 分区 + 区域（不含分项） */
        public boolean isPersonEmpty() {
            return venueCodes.isEmpty() && areaCodes.isEmpty() && partitionCodes.isEmpty();
        }

        /** 车证设备权限：仅场馆 + 停车通行码 */
        public boolean isCarEmpty() {
            return venueCodes.isEmpty() && parkCodes.isEmpty();
        }

        public boolean hasAnyConfigured() {
            return !isPersonEmpty();
        }

        public boolean hasPersonConfigured() {
            return !isPersonEmpty();
        }

        public boolean hasCarConfigured() {
            return !isCarEmpty();
        }
    }

    /**
     * 解析设备通行权限：仅使用注册成功后
     * {@code getMatrixAuthInfoList} / {@code getCarMatrixAuthList} 的缓存。
     * 不使用 getBasicInfo 中的场馆/分区/区域权限字典。
     */
    public static PermissionSets resolveDevicePermissions(
            Context context, String activeId, BasicInfo basicInfo, int moduleType) {
        if (TextUtils.isEmpty(activeId)) {
            return new PermissionSets();
        }
        return AppPreferences.getDeviceMatrixAuthCodes(
                context, activeId, ModuleType.normalize(moduleType));
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
            // 分项暂不参与人证核验，解析仍写入 sportCodes 便于以后打开
            addPrivilegeTokens(sets.sportCodes, auth.sportProject);
            addPrivilegeTokens(sets.sportCodes, auth.sportProjectVal);
            // 区域：venueArea；分区：venuePartition
            addPrivilegeTokens(sets.areaCodes, auth.venueArea);
            addPrivilegeTokens(sets.areaCodes, auth.venueAreaVal);
            addPrivilegeTokens(sets.partitionCodes, auth.venuePartition);
            addPrivilegeTokens(sets.partitionCodes, auth.venuePartitionVal);
            addPrivilegeTokens(sets.parkCodes, auth.park);
            addPrivilegeTokens(sets.parkCodes, auth.parkVal);
        }
        return sets;
    }

    /** 将后台权限字段拆成可匹配 token（支持逗号 / 波浪号，并保留展示名） */
    public static void addPrivilegeTokens(Set<String> target, @Nullable String raw) {
        if (target == null || TextUtils.isEmpty(raw)) {
            return;
        }
        String trimmed = raw.trim();
        if (isPlaceholderPrivilegeCode(trimmed)) {
            return;
        }
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
        // 区域字典 personCertAreaList；分区字典 personCertZoneList
        sets.areaCodes.addAll(resolveDictCodes(
                basicInfo.getPersonCertAreaList(),
                AppPreferences.getSelectedAreaPermissions(context, activeId)));
        sets.partitionCodes.addAll(resolveDictCodes(
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
            addPrivilegeTokens(sets.areaCodes, auth.venueArea);
            addPrivilegeTokens(sets.areaCodes, auth.venueAreaVal);
            addPrivilegeTokens(sets.partitionCodes, auth.venuePartition);
            addPrivilegeTokens(sets.partitionCodes, auth.venuePartitionVal);
            addPrivilegeTokens(sets.parkCodes, auth.park);
            addPrivilegeTokens(sets.parkCodes, auth.parkVal);
        }
        return sets;
    }

    /**
     * 校验证件是否满足设备权限（场馆/分项/区域/分区均需满足；未配置的类型视为不限制）。
     * <p>
     * @param areaPrivileges 区域权限（getActiveUser.areaPrivileges）
     * @param zonePrivileges 分区权限（getActiveUser.zonePrivileges）
     * @param strictEmptyDevice true 时设备未配置任何权限则拒绝（须先在设置页配置）
     */
    public static boolean certificateMatchesDevice(
            String venuePrivileges,
            String areaPrivileges,
            String zonePrivileges,
            @Nullable String sportPrivileges,
            PermissionSets device,
            boolean strictEmptyDevice) {
        return certificateMatchesDevice(
                venuePrivileges, areaPrivileges, zonePrivileges, sportPrivileges,
                device, strictEmptyDevice, false);
    }

    /**
     * @param skipEmptyCertPrivileges true 时证件场馆/区域/分区为空则跳过该维比对（视为有权限）
     */
    public static boolean certificateMatchesDevice(
            String venuePrivileges,
            String areaPrivileges,
            String zonePrivileges,
            @Nullable String sportPrivileges,
            PermissionSets device,
            boolean strictEmptyDevice,
            boolean skipEmptyCertPrivileges) {
        return evaluateCertificateMatch(
                venuePrivileges, areaPrivileges, zonePrivileges, sportPrivileges,
                device, strictEmptyDevice, skipEmptyCertPrivileges).matched;
    }

    /**
     * 与 {@link #certificateMatchesDevice} 相同比对，并返回失败原因（供核验页展示与日志）。
     */
    public static MatchResult evaluateCertificateMatch(
            String venuePrivileges,
            String areaPrivileges,
            String zonePrivileges,
            @Nullable String sportPrivileges,
            PermissionSets device,
            boolean strictEmptyDevice,
            boolean skipEmptyCertPrivileges) {
        if (device == null || device.isPersonEmpty()) {
            Log.w(TAG, "人证权限比对: 设备场馆/区域/分区均为空, strictEmptyDevice="
                    + strictEmptyDevice);
            return strictEmptyDevice
                    ? MatchResult.fail("设备未配置场馆、分区和区域权限")
                    : MatchResult.ok();
        }

        Set<String> certVenues = splitPrivileges(venuePrivileges);
        Set<String> certAreas = splitPrivileges(areaPrivileges);
        Set<String> certPartitions = splitPrivileges(zonePrivileges);
        //todo 分项暂不比对
        // Set<String> certSports = splitPrivileges(sportPrivileges);

        boolean venueOk = matchesPrivilegeDimension(
                certVenues, device.venueCodes, skipEmptyCertPrivileges);
        // boolean sportOk = matchesPrivilegeDimension(
        //         certSports, device.sportCodes, skipEmptyCertPrivileges);
        boolean areaOk = matchesPrivilegeDimension(
                certAreas, device.areaCodes, skipEmptyCertPrivileges);
        boolean partitionOk = matchesPrivilegeDimension(
                certPartitions, device.partitionCodes, skipEmptyCertPrivileges);

        List<String> failed = new ArrayList<>();
        if (!venueOk) {
            failed.add("场馆");
        }
        // if (!sportOk) {
        //     failed.add("分项");
        // }
        if (!areaOk) {
            failed.add("区域");
        }
        if (!partitionOk) {
            failed.add("分区");
        }

        String summary = "人证权限比对 " + (failed.isEmpty() ? "通过" : ("不通过，失败维度=" + failed))
                + " skipEmptyCert=" + skipEmptyCertPrivileges
                + "\n  设备场馆=" + device.venueCodes + " 证件场馆=[" + venuePrivileges + "] ok=" + venueOk
                // + "\n  设备分项=" + device.sportCodes + " 证件分项=[" + sportPrivileges + "] ok=" + sportOk
                + "\n  设备区域=" + device.areaCodes + " 证件区域=[" + areaPrivileges + "] ok=" + areaOk
                + "\n  设备分区=" + device.partitionCodes + " 证件分区=[" + zonePrivileges + "] ok=" + partitionOk;
        if (failed.isEmpty()) {
            Log.i(TAG, summary);
            return MatchResult.ok();
        }
        Log.w(TAG, summary);
        return MatchResult.fail(TextUtils.join("、", failed) + "权限不足");
    }

    /**
     * 车证权限：设备场馆 ↔ venueCodeChildren，停车通行码 ↔ parkingCode。
     * 场馆+停车都空则拒绝；某一维设备或证件为空则跳过该维。
     */
    public static MatchResult evaluateCarCertificateMatch(
            @Nullable String venueCodeChildren,
            @Nullable String parkingCode,
            PermissionSets device) {
        if (device == null || device.isCarEmpty()) {
            Log.w(TAG, "车证权限比对失败: 设备场馆和停车通行码均为空"
                    + " 证件场馆=[" + venueCodeChildren + "] 证件停车=[" + parkingCode + "]");
            return MatchResult.fail("设备未配置场馆和停车通行码");
        }

        Set<String> certVenues = splitPrivileges(venueCodeChildren);
        Set<String> certParks = splitPrivileges(parkingCode);
        boolean venueOk = matchesPrivilegeDimension(certVenues, device.venueCodes, true);
        boolean parkOk = matchesPrivilegeDimension(certParks, device.parkCodes, true);

        List<String> failed = new ArrayList<>();
        if (!venueOk) {
            failed.add("场馆");
        }
        if (!parkOk) {
            failed.add("停车通行码");
        }
        String summary = "车证权限比对 " + (failed.isEmpty() ? "通过" : ("不通过，失败维度=" + failed))
                + "\n  设备场馆=" + device.venueCodes + " 证件场馆=[" + venueCodeChildren + "] ok=" + venueOk
                + "\n  设备停车=" + device.parkCodes + " 证件停车=[" + parkingCode + "] ok=" + parkOk;
        if (failed.isEmpty()) {
            Log.i(TAG, summary);
            return MatchResult.ok();
        }
        Log.w(TAG, summary);
        return MatchResult.fail(TextUtils.join("、", failed) + "权限不足");
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
                if (!TextUtils.isEmpty(code) && !isPlaceholderPrivilegeCode(code)) {
                    set.add(code);
                }
            }
            return set;
        }
        for (String part : trimmed.split(",")) {
            String code = part.trim();
            if (!TextUtils.isEmpty(code) && !isPlaceholderPrivilegeCode(code)) {
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

    /** 后台占位空值，不展示、不参与比对 */
    public static boolean isPlaceholderPrivilegeCode(@Nullable String code) {
        if (TextUtils.isEmpty(code)) {
            return true;
        }
        String c = code.trim();
        if (c.isEmpty()) {
            return true;
        }
        return "NONE".equalsIgnoreCase(c)
                || "NULL".equalsIgnoreCase(c)
                || "NIL".equalsIgnoreCase(c)
                || "N/A".equalsIgnoreCase(c)
                || "NA".equalsIgnoreCase(c)
                || "UNDEFINED".equalsIgnoreCase(c)
                || "-".equals(c)
                || "--".equals(c);
    }

    /**
     * 设备该维未配置或为 ALL/INF 则不限制；证件该维为空且 skipEmptyCert 时视为有权限。
     */
    private static boolean matchesPrivilegeDimension(
            Set<String> certCodes, Set<String> deviceCodes, boolean skipEmptyCert) {
        if (deviceCodes == null || deviceCodes.isEmpty() || hasFullPrivilege(deviceCodes)) {
            return true;
        }
        if (skipEmptyCert && (certCodes == null || certCodes.isEmpty())) {
            return true;
        }
        return hasFullPrivilege(certCodes) || intersects(certCodes, deviceCodes);
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
        if (certSet == null || deviceSet == null || certSet.isEmpty() || deviceSet.isEmpty()) {
            return false;
        }
        for (String deviceCode : deviceSet) {
            if (TextUtils.isEmpty(deviceCode)) {
                continue;
            }
            for (String certCode : certSet) {
                if (!TextUtils.isEmpty(certCode) && deviceCode.equalsIgnoreCase(certCode)) {
                    return true;
                }
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
