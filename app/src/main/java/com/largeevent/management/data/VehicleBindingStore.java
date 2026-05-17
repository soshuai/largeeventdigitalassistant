package com.largeevent.management.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VehicleBindingStore {

    private static final Map<String, String> CHIP_PLATE_MAP = new ConcurrentHashMap<>();

    public static void bind(String chipId, String plate) {
        if (chipId == null || plate == null) {
            return;
        }
        CHIP_PLATE_MAP.put(chipId, plate);
    }

    public static boolean isBound(String chipId) {
        return chipId != null && CHIP_PLATE_MAP.containsKey(chipId);
    }

    public static String getPlate(String chipId) {
        return CHIP_PLATE_MAP.get(chipId);
    }
}



