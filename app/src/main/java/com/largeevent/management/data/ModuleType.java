package com.largeevent.management.data;

/**
 * 人证 / 车证业务模块（与 getBasicInfo.locationInfoList.moduleType、注册 EqpType 对应）
 */
public final class ModuleType {

    public static final int PERSON = 1;
    public static final int VEHICLE = 2;

    public static final String LABEL_PERSON = "人证";
    public static final String LABEL_VEHICLE = "车证";

    private ModuleType() {
    }

    public static boolean isValid(int moduleType) {
        return moduleType == PERSON || moduleType == VEHICLE;
    }

    public static int normalize(int moduleType) {
        return isValid(moduleType) ? moduleType : PERSON;
    }

    public static String toLabel(int moduleType) {
        return moduleType == VEHICLE ? LABEL_VEHICLE : LABEL_PERSON;
    }

    public static int fromLabel(String label) {
        if (LABEL_VEHICLE.equals(label)) {
            return VEHICLE;
        }
        return PERSON;
    }

    /** location.moduleType 未标注(0)时两边都可用，兼容旧数据 */
    public static boolean matchesLocation(int locationModuleType, int selectedModule) {
        if (locationModuleType == 0) {
            return true;
        }
        return locationModuleType == selectedModule;
    }
}
