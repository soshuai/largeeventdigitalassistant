package com.largeevent.management.data;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.largeevent.management.model.VerificationResult;
import com.largeevent.management.model.VerificationResultType;
import com.largeevent.management.network.dto.ActiveUserBaseVo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 新版人证通用验证规则（与核验文档一致）。
 * 顺序：注销 → 黑名单/背审/发布 → 未绑定(TP) → 有效期 → 权限 → 证件类型 → 激活(MP) → 人证合一(MP/芯片)。
 */
public final class PersonVerificationHelper {

    public static final String MAIN_MP = "01";
    public static final String MAIN_TP = "02";
    public static final String MAIN_VP = "03";

    public static final String EQP_HANDHELD = "手持式查验设备";
    public static final String EQP_CHANNEL = "通道设备";
    public static final String EQP_GATE = "闸机设备";

    /** 背审通过 */
    private static final String BS_PASS = "1";

    /** 证件已发布 */
    public static final int PUBLISH_PUBLISHED = 2;
    /** 证件已取消发布 */
    public static final int PUBLISH_CANCELLED = 3;

    private PersonVerificationHelper() {
    }

    public enum Step {
        PASS,
        CANCELED,
        BLACKLIST,
        BACKGROUND_FAIL,
        EXPIRED,
        PERMISSION_DENIED,
        DEVICE_NOT_CONFIGURED,
        VP_PASS,
        TP_UNBOUND,
        TP_PASS,
        NOT_ACTIVATED,
        NEED_FACE_VERIFY,
        FACE_SKIP_PASS
    }

    public static class StepResult {
        public final Step step;
        @Nullable public final String title;
        @Nullable public final String description;
        public final boolean bindingRequired;

        StepResult(Step step, @Nullable String title, @Nullable String description, boolean bindingRequired) {
            this.step = step;
            this.title = title;
            this.description = description;
            this.bindingRequired = bindingRequired;
        }

        static StepResult ok(Step step) {
            return new StepResult(step, null, null, false);
        }

        static StepResult fail(Step step, String title, String description) {
            return fail(step, title, description, false);
        }

        static StepResult fail(Step step, String title, String description, boolean bindingRequired) {
            return new StepResult(step, title, description, bindingRequired);
        }
    }

    /**
     * 通用验证（不含人证合一人脸比对，不含未识读/无效证件）。
     */
    public static StepResult runCommonRules(
            Context context,
            String activeId,
            ActiveUserBaseVo user,
            DevicePermissionHelper.PermissionSets devicePermissions) {
        if (user == null) {
            return StepResult.fail(Step.CANCELED, "未识读出证件", "证件信息为空");
        }

        if (isCanceled(user)) {
            return StepResult.fail(Step.CANCELED, "证件已注销", "证件已注销");
        }

        if (user.blackSign == 1) {
            return StepResult.fail(Step.BLACKLIST, "限制通行", "该人员已被列入黑名单");
        }

        if (!isBackgroundCheckPassed(user)) {
            return StepResult.fail(Step.BACKGROUND_FAIL, "限制通行", "背审未通过，禁止通行");
        }

        if (!isPublishOk(user)) {
            return StepResult.fail(Step.EXPIRED, "无效证件", "证件已取消发布");
        }

        // TP 未绑定：优先引导绑定，通过后再校验有效期与权限
        if (isTpPassType(user) && !isBound(user)) {
            return StepResult.fail(Step.TP_UNBOUND, "证件未绑定", "请先完成实名绑定", true);
        }

        if (!isValidityOk(user)) {
            String detail = isTpPassType(user) ? "日通行证不在有效期内" : "证件已失效";
            return StepResult.fail(Step.EXPIRED, "无效证件", detail);
        }

        if (!isDeviceConfigured(context, activeId)) {
            return StepResult.fail(Step.DEVICE_NOT_CONFIGURED, "无法核验",
                    "请先在活动设置中同时选择设备所在位置和设备所在分区");
        }

        DevicePermissionHelper.MatchResult permissionMatch =
                DevicePermissionHelper.evaluateCertificateMatch(
                        user.venuePrivileges,
                        user.areaPrivileges,
                        user.zonePrivileges,
                        user.sportPrivileges,
                        devicePermissions,
                        true,
                        true);
        if (!permissionMatch.matched) {
            String detail = !TextUtils.isEmpty(permissionMatch.failReason)
                    ? permissionMatch.failReason : "权限不足";
            return StepResult.fail(Step.PERMISSION_DENIED, "无权通行", detail);
        }

        String mainType = user.mainAppTypeCode;
        if (MAIN_VP.equals(mainType)) {
            return StepResult.ok(Step.VP_PASS);
        }

        if (isTpPassType(user)) {
            return StepResult.ok(Step.TP_PASS);
        }

        // MP 及未知类型走实名注册流程
        if (!isActivated(context, activeId, user)) {
            return StepResult.fail(Step.NOT_ACTIVATED, "证件未激活", "请先激活证件");
        }

        return StepResult.ok(Step.NEED_FACE_VERIFY);
    }

    public static boolean isCanceled(ActiveUserBaseVo user) {
        if (user.eventStatus == 6) {
            return true;
        }
        return "1".equals(user.cancelCard);
    }

    public static boolean isBackgroundCheckPassed(ActiveUserBaseVo user) {
        if (TextUtils.isEmpty(user.bsStatus)) {
            return true;
        }
        return BS_PASS.equals(user.bsStatus.trim());
    }

    /** bindStatus：1-已绑定，0-未绑定 */
    public static boolean isBound(ActiveUserBaseVo user) {
        return user != null && "1".equals(trimToEmpty(user.bindStatus));
    }

    /** passType=TP 或 mainAppTypeCode=02（日卡） */
    public static boolean isTpPassType(ActiveUserBaseVo user) {
        if (user == null) {
            return false;
        }
        if ("TP".equalsIgnoreCase(trimToEmpty(user.passType))) {
            return true;
        }
        return MAIN_TP.equals(user.mainAppTypeCode);
    }

    private static String trimToEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isActivated(Context context, String activeId, ActiveUserBaseVo user) {
        if (!AppPreferences.isActivationCheckEnabled(context, activeId)) {
            return user.eventStatus <= 5;
        }
        return user.eventStatus == 5;
    }

    /** 证件发布状态：仅 2-已发布 可通行；3-已取消发布 拒绝。 */
    public static boolean isPublishOk(ActiveUserBaseVo user) {
        if (user == null) {
            return false;
        }
        return user.cardPublishFlag == PUBLISH_PUBLISHED;
    }

    /**
     * 仅以 cardEffectiveDate / cardExpirationDate 判断是否在有效期内（与接口文档一致）。
     */
    public static boolean isValidityOk(ActiveUserBaseVo user) {
        if (user == null) {
            return false;
        }
        return isValidPeriod(
                ActiveUserParser.resolveCardEffectiveDate(user),
                ActiveUserParser.resolveCardExpirationDate(user));
    }

    /**
     * 设备是否已配置：位置与分区两项均必选（AND），缺一不可。
     * 通行权限勾选由 {@link DevicePermissionHelper#certificateMatchesDevice} 单独校验；
     * 人证场馆/区域/分区为空时跳过该维比对。
     */
    public static boolean isDeviceConfigured(Context context, String activeId) {
        return hasLocationAndZone(context, activeId);
    }

    public static boolean hasLocationAndZone(Context context, String activeId) {
        String locationId = AppPreferences.getSelectedLocationId(
                context, activeId, ModuleType.PERSON);
        String zoneId = AppPreferences.getSelectedZoneId(
                context, activeId, ModuleType.PERSON);
        return !TextUtils.isEmpty(locationId) && !TextUtils.isEmpty(zoneId);
    }

    /**
     * 是否必须做人证合一（人脸比对）。
     */
    public static boolean isFaceVerifyRequired(
            Context context, String activeId, boolean hasLivePhoto) {
        String eqpType = AppPreferences.getEqpType(context, activeId);
        if (EQP_CHANNEL.equals(eqpType) || EQP_GATE.equals(eqpType)) {
            return true;
        }
        // 手持：有拍照则比对，无拍照可跳过
        return hasLivePhoto;
    }

    public static boolean isFaceVerifyMandatory(Context context, String activeId) {
        String eqpType = AppPreferences.getEqpType(context, activeId);
        return EQP_CHANNEL.equals(eqpType) || EQP_GATE.equals(eqpType);
    }

    public static String faceMismatchTitle(Context context, String activeId) {
        String eqpType = AppPreferences.getEqpType(context, activeId);
        if (EQP_CHANNEL.equals(eqpType) || EQP_GATE.equals(eqpType)) {
            return "请检查证件";
        }
        return "人证不合一";
    }

    public static String faceMismatchDescription(Context context, String activeId) {
        String eqpType = AppPreferences.getEqpType(context, activeId);
        if (EQP_CHANNEL.equals(eqpType) || EQP_GATE.equals(eqpType)) {
            return "人证不合一";
        }
        return "人证不合一";
    }

    public static String passTitle() {
        return "请通行";
    }

    public static VerificationResult buildVerificationResult(
            StepResult stepResult,
            com.largeevent.management.model.CertificateInfo info,
            @Nullable String chipId) {
        switch (stepResult.step) {
            case VP_PASS:
            case TP_PASS:
            case FACE_SKIP_PASS:
            case PASS:
                return new VerificationResult(
                        VerificationResultType.PASS,
                        passTitle(),
                        "",
                        info);
            case CANCELED:
                return new VerificationResult(
                        VerificationResultType.CANCELED,
                        stepResult.title,
                        stepResult.description,
                        info);
            case BLACKLIST:
            case BACKGROUND_FAIL:
                return new VerificationResult(
                        VerificationResultType.BLACKLIST,
                        stepResult.title,
                        stepResult.description,
                        info);
            case EXPIRED:
                return new VerificationResult(
                        VerificationResultType.EXPIRED,
                        stepResult.title,
                        stepResult.description,
                        info);
            case PERMISSION_DENIED:
            case DEVICE_NOT_CONFIGURED:
                return new VerificationResult(
                        VerificationResultType.PERMISSION_DENIED,
                        stepResult.title,
                        stepResult.description,
                        info);
            case TP_UNBOUND:
                return new VerificationResult(
                        VerificationResultType.UNBOUND,
                        stepResult.title,
                        stepResult.description,
                        info,
                        stepResult.bindingRequired,
                        chipId);
            case NOT_ACTIVATED:
                return new VerificationResult(
                        VerificationResultType.NOT_ACTIVATED,
                        stepResult.title,
                        stepResult.description,
                        info);
            default:
                return new VerificationResult(
                        VerificationResultType.INVALID_CERT,
                        stepResult.title != null ? stepResult.title : "无效证件",
                        stepResult.description,
                        info);
        }
    }

    /**
     * 一人多证：优先芯片匹配，其次非注销且权限最佳。
     */
    @Nullable
    public static ActiveUserBaseVo selectBestUser(
            List<ActiveUserBaseVo> list,
            @Nullable String chipId,
            @Nullable String subUnitName) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<ActiveUserBaseVo> candidates = new ArrayList<>();
        for (ActiveUserBaseVo dto : list) {
            if (dto == null || isCanceled(dto)) {
                continue;
            }
            if (!TextUtils.isEmpty(subUnitName)
                    && !TextUtils.isEmpty(dto.activeUnit)
                    && !subUnitName.equals(dto.activeUnit.trim())) {
                continue;
            }
            if (!TextUtils.isEmpty(chipId)) {
                String c = chipId.trim();
                if (c.equalsIgnoreCase(trim(dto.chipid))
                        || c.equalsIgnoreCase(trim(dto.tagNo1))
                        || c.equalsIgnoreCase(trim(dto.tagNo2))) {
                    return dto;
                }
            }
            candidates.add(dto);
        }
        if (candidates.isEmpty()) {
            return list.get(0);
        }
        ActiveUserBaseVo best = candidates.get(0);
        for (ActiveUserBaseVo dto : candidates) {
            if (typePriority(dto.mainAppTypeCode) < typePriority(best.mainAppTypeCode)) {
                best = dto;
            }
        }
        return best;
    }

    private static int typePriority(@Nullable String mainType) {
        if (MAIN_MP.equals(mainType)) {
            return 0;
        }
        if (MAIN_TP.equals(mainType)) {
            return 1;
        }
        if (MAIN_VP.equals(mainType)) {
            return 2;
        }
        return 3;
    }

    @Nullable
    private static String trim(@Nullable String s) {
        return s == null ? null : s.trim();
    }

    /**
     * 按 cardEffectiveDate / cardExpirationDate 判断当前是否在有效期内。
     * 起止均为空视为永久有效；结束时刻含秒时按该秒末尾（含毫秒） inclusive 比较。
     */
    private static boolean isValidPeriod(@Nullable String validBegin, @Nullable String validEnd) {
        String beginRaw = trimToNull(validBegin);
        String endRaw = trimToNull(validEnd);
        if (beginRaw == null && endRaw == null) {
            return true;
        }
        Date now = new Date();
        if (beginRaw != null) {
            Date begin = parseDateTime(beginRaw, false);
            if (begin == null || now.before(begin)) {
                return false;
            }
        }
        if (endRaw != null) {
            Date end = parseDateTime(endRaw, true);
            end = inclusiveEnd(end, endRaw);
            if (end == null || now.after(end)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 含时间的结束时刻扩展到该秒末尾，避免 23:59:59.xxx 被误判过期。 */
    @Nullable
    private static Date inclusiveEnd(@Nullable Date end, @Nullable String raw) {
        if (end == null || raw == null) {
            return end;
        }
        if (raw.contains(":")) {
            return new Date(end.getTime() + 999L);
        }
        return end;
    }

    @Nullable
    private static Date parseDateTime(String raw, boolean treatDateOnlyAsEndOfDay) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "yyyyMMdd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
                sdf.setLenient(false);
                Date parsed = sdf.parse(raw);
                if (parsed == null) {
                    continue;
                }
                if (treatDateOnlyAsEndOfDay
                        && "yyyy-MM-dd".equals(pattern)
                        && raw.length() <= 10) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsed);
                    cal.set(Calendar.HOUR_OF_DAY, 23);
                    cal.set(Calendar.MINUTE, 59);
                    cal.set(Calendar.SECOND, 59);
                    cal.set(Calendar.MILLISECOND, 999);
                    return cal.getTime();
                }
                return parsed;
            } catch (Exception ignored) {
                // try next pattern
            }
        }
        return null;
    }
}
