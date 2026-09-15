package com.largeevent.management.data;

import android.text.TextUtils;
import android.widget.TextView;

import com.largeevent.management.R;
import com.largeevent.management.network.dto.MatrixAuthInfoDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 将 getMatrixAuthInfoList 设备权限映射到活动设置页权限芯片。
 */
public final class MatrixAuthSelectionHelper {

    private MatrixAuthSelectionHelper() {
    }

    public static void applyMatrixAuthToCheckboxes(
            List<MatrixAuthInfoDTO> authList,
            List<TextView> venueChips,
            List<TextView> partitionChips,
            List<TextView> areaChips) {
        DevicePermissionHelper.PermissionSets sets =
                DevicePermissionHelper.fromMatrixAuthDtoList(authList);

        applyPermissionSets(venueChips, partitionChips, areaChips, sets);
        applyVenueWithFallback(venueChips, authList, sets.venueCodes);
    }

    /**
     * 按已缓存的权限 code 勾选展示。
     *
     * @param partitionChips 分区权限芯片（personCertZoneList）
     * @param areaChips      区域权限芯片（personCertAreaList）
     */
    public static void applyPermissionSets(
            List<TextView> venueChips,
            List<TextView> partitionChips,
            List<TextView> areaChips,
            DevicePermissionHelper.PermissionSets sets) {
        Set<String> venues = sets != null ? sets.venueCodes : null;
        Set<String> partitions = sets != null ? sets.partitionCodes : null;
        Set<String> areas = sets != null ? sets.areaCodes : null;
        applyByDictCode(venueChips, venues);
        applyByDictCode(partitionChips, partitions);
        applyByDictCode(areaChips, areas);
    }

    /** 选中：蓝底白字；未选中：灰底灰字。不依赖系统 CheckBox 勾选渲染。 */
    public static void setChipSelected(TextView chip, boolean selected) {
        if (chip == null) {
            return;
        }
        chip.setSelected(selected);
        chip.setBackgroundResource(selected
                ? R.drawable.bg_perm_chip_selected
                : R.drawable.bg_perm_chip_normal);
        chip.setTextColor(selected ? 0xFFFFFFFF : 0xFF4A4F63);
    }

    /**
     * 场馆：先按 dictCode 勾选；字典里没有的 code（如 SGG、INF）按 venueVal 与已有项文本匹配。
     */
    private static void applyVenueWithFallback(
            List<TextView> venueChips,
            List<MatrixAuthInfoDTO> authList,
            Set<String> venueCodes) {
        if (venueChips == null || venueCodes == null || venueCodes.isEmpty()) {
            return;
        }

        Map<String, String> codeToLabel = new HashMap<>();
        if (authList != null) {
            for (MatrixAuthInfoDTO auth : authList) {
                if (auth != null && !TextUtils.isEmpty(auth.venue)) {
                    for (String code : DevicePermissionHelper.splitPrivilegeTokens(auth.venue)) {
                        String label = !TextUtils.isEmpty(auth.venueVal) ? auth.venueVal.trim() : code;
                        codeToLabel.put(code, label);
                    }
                }
            }
        }

        for (TextView chip : venueChips) {
            if (chip == null || chip.isSelected()) {
                continue;
            }
            Object tag = chip.getTag();
            String dictCode = tag != null ? tag.toString().trim() : "";
            if (!TextUtils.isEmpty(dictCode) && containsIgnoreCase(venueCodes, dictCode)) {
                setChipSelected(chip, true);
                continue;
            }
            CharSequence text = chip.getText();
            if (text == null) {
                continue;
            }
            String label = text.toString().trim();
            for (Map.Entry<String, String> entry : codeToLabel.entrySet()) {
                if (containsIgnoreCase(venueCodes, entry.getKey())
                        && (label.equals(entry.getValue())
                        || label.contains(entry.getValue())
                        || entry.getValue().contains(label))) {
                    setChipSelected(chip, true);
                    break;
                }
            }
        }
    }

    private static void applyByDictCode(List<TextView> chips, Set<String> dictCodes) {
        if (chips == null) {
            return;
        }
        boolean selectAll = containsFullPrivilege(dictCodes);
        Set<String> normalized = normalizeCodes(dictCodes);
        for (TextView chip : chips) {
            if (chip == null) {
                continue;
            }
            if (selectAll) {
                setChipSelected(chip, true);
                continue;
            }
            Object tag = chip.getTag();
            String dictCode = tag != null ? tag.toString().trim() : "";
            CharSequence textCs = chip.getText();
            String label = textCs != null ? textCs.toString().trim() : "";
            boolean selected = (!TextUtils.isEmpty(dictCode) && containsIgnoreCase(normalized, dictCode))
                    || (!TextUtils.isEmpty(label) && containsIgnoreCase(normalized, label));
            setChipSelected(chip, selected);
        }
    }

    private static boolean containsFullPrivilege(Set<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return false;
        }
        for (String code : codes) {
            if (DevicePermissionHelper.isFullPrivilegeCode(code)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeCodes(Set<String> codes) {
        Set<String> normalized = new HashSet<>();
        if (codes == null) {
            return normalized;
        }
        for (String code : codes) {
            if (!TextUtils.isEmpty(code)) {
                normalized.add(code.trim());
            }
        }
        return normalized;
    }

    private static boolean containsIgnoreCase(Set<String> codes, String value) {
        if (codes == null || codes.isEmpty() || TextUtils.isEmpty(value)) {
            return false;
        }
        if (codes.contains(value)) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String code : codes) {
            if (code != null && code.toLowerCase(Locale.ROOT).equals(lower)) {
                return true;
            }
        }
        return false;
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
            for (String code : DevicePermissionHelper.splitPrivilegeTokens(auth.venue)) {
                if (dictVenueCodes != null && containsIgnoreCase(dictVenueCodes, code)) {
                    continue;
                }
                if (!missing.containsKey(code)) {
                    missing.put(code, !TextUtils.isEmpty(auth.venueVal) ? auth.venueVal.trim() : code);
                }
            }
        }
        return missing;
    }
}
