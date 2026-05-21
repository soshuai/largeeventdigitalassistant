package com.largeevent.management.data;

import android.text.TextUtils;
import android.widget.CheckBox;

import com.largeevent.management.network.dto.MatrixAuthInfoDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 getMatrixAuthInfoList 设备权限映射到活动设置页勾选框。
 */
public final class MatrixAuthSelectionHelper {

    private MatrixAuthSelectionHelper() {
    }

    public static void applyMatrixAuthToCheckboxes(
            List<MatrixAuthInfoDTO> authList,
            List<CheckBox> venueCheckboxes,
            List<CheckBox> partitionCheckboxes,
            List<CheckBox> certZoneCheckboxes) {
        DevicePermissionHelper.PermissionSets sets =
                DevicePermissionHelper.fromMatrixAuthDtoList(authList);

        applyByDictCode(venueCheckboxes, sets.venueCodes);
        applyVenueWithFallback(venueCheckboxes, authList, sets.venueCodes);

        applyByDictCode(partitionCheckboxes, sets.partitionCodes);
        applyByDictCode(certZoneCheckboxes, sets.zoneCodes);
    }

    /**
     * 场馆：先按 dictCode 勾选；字典里没有的 code（如 SGG、INF）按 venueVal 与已有项文本匹配。
     */
    private static void applyVenueWithFallback(
            List<CheckBox> venueCheckboxes,
            List<MatrixAuthInfoDTO> authList,
            Set<String> venueCodes) {
        if (venueCheckboxes == null || venueCodes == null || venueCodes.isEmpty()) {
            return;
        }

        Map<String, String> codeToLabel = new HashMap<>();
        if (authList != null) {
            for (MatrixAuthInfoDTO auth : authList) {
                if (auth != null && !TextUtils.isEmpty(auth.venue)) {
                    String code = auth.venue.trim();
                    String label = !TextUtils.isEmpty(auth.venueVal) ? auth.venueVal.trim() : code;
                    codeToLabel.put(code, label);
                }
            }
        }

        Set<String> matchedCodes = new HashSet<>();
        for (CheckBox checkBox : venueCheckboxes) {
            if (checkBox == null) {
                continue;
            }
            Object tag = checkBox.getTag();
            String dictCode = tag != null ? tag.toString() : null;
            if (!TextUtils.isEmpty(dictCode) && venueCodes.contains(dictCode)) {
                checkBox.setChecked(true);
                matchedCodes.add(dictCode);
                continue;
            }
            CharSequence text = checkBox.getText();
            if (text == null) {
                continue;
            }
            String label = text.toString().trim();
            for (Map.Entry<String, String> entry : codeToLabel.entrySet()) {
                if (venueCodes.contains(entry.getKey())
                        && (label.equals(entry.getValue()) || label.contains(entry.getValue())
                        || entry.getValue().contains(label))) {
                    checkBox.setChecked(true);
                    matchedCodes.add(entry.getKey());
                    break;
                }
            }
        }
    }

    private static void applyByDictCode(List<CheckBox> checkboxes, Set<String> dictCodes) {
        if (checkboxes == null || dictCodes == null || dictCodes.isEmpty()) {
            return;
        }
        for (CheckBox checkBox : checkboxes) {
            if (checkBox == null) {
                continue;
            }
            Object tag = checkBox.getTag();
            String dictCode = tag != null ? tag.toString() : null;
            checkBox.setChecked(!TextUtils.isEmpty(dictCode) && dictCodes.contains(dictCode));
        }
    }

    /** 收集设备权限里存在、但 venueInfoList 字典中不存在的场馆 code → 展示名 */
    public static Map<String, String> collectVenueCodesNotInDict(
            List<MatrixAuthInfoDTO> authList, Set<String> dictVenueCodes) {
        Map<String, String> missing = new HashMap<>();
        if (authList == null) {
            return missing;
        }
        for (MatrixAuthInfoDTO auth : authList) {
            if (auth == null || TextUtils.isEmpty(auth.venue)) {
                continue;
            }
            String code = auth.venue.trim();
            if (dictVenueCodes != null && dictVenueCodes.contains(code)) {
                continue;
            }
            if (!missing.containsKey(code)) {
                missing.put(code, !TextUtils.isEmpty(auth.venueVal) ? auth.venueVal.trim() : code);
            }
        }
        return missing;
    }
}
