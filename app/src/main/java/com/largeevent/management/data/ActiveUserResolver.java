package com.largeevent.management.data;

import androidx.annotation.Nullable;

import com.largeevent.management.network.dto.ActiveUserBaseVo;

import java.util.List;

/** 从 getActiveUser 多条结果中选取当前应核验的证件。 */
public final class ActiveUserResolver {

    private ActiveUserResolver() {
    }

    @Nullable
    public static ActiveUserBaseVo resolve(
            @Nullable List<ActiveUserBaseVo> list,
            @Nullable String chipId,
            @Nullable String subUnitName) {
        return PersonVerificationHelper.selectBestUser(list, chipId, subUnitName);
    }
}
