package com.largeevent.management.data;

import androidx.annotation.Nullable;

import com.largeevent.management.network.dto.ActiveUserBaseDTO;

import java.util.List;

/** 从 getActiveUser 多条结果中选取当前应核验的证件。 */
public final class ActiveUserResolver {

    private ActiveUserResolver() {
    }

    @Nullable
    public static ActiveUserBaseDTO resolve(
            @Nullable List<ActiveUserBaseDTO> list,
            @Nullable String chipId,
            @Nullable String subUnitName) {
        return PersonVerificationHelper.selectBestUser(list, chipId, subUnitName);
    }
}
