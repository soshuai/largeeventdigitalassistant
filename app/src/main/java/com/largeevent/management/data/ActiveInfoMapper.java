package com.largeevent.management.data;

import com.google.gson.Gson;
import com.largeevent.management.model.BasicInfo;
import com.largeevent.management.network.dto.ActiveInfoVo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 getActiveInfo 接口 DTO 转为本地 ActiveModel
 */
public final class ActiveInfoMapper {

    private ActiveInfoMapper() {
    }

    public static BasicInfo.ActiveModel toActiveModel(ActiveInfoVo dto) {
        if (dto == null) {
            return null;
        }
        return new BasicInfo.ActiveModel(
                dto.id,
                dto.status,
                dto.activeName,
                dto.activeType,
                dto.activeVenueId,
                dto.licenseUnit,
                dto.hostUnit,
                dto.activeScale,
                dto.securityPerson,
                dto.checkPerson,
                dto.sign,
                dto.avatar,
                dto.lineStatus,
                dto.ysActiveId,
                dto.accreditationCode,
                dto.supervisionId,
                dto.signType,
                new ArrayList<>(),
                new ArrayList<>(),
                dto.signStartTime,
                dto.signEndTime,
                dto.activeStartTime,
                dto.activeEndTime,
                dto.signStatus
        );
    }

    public static List<BasicInfo.ActiveModel> toActiveModels(List<ActiveInfoVo> dtos) {
        List<BasicInfo.ActiveModel> list = new ArrayList<>();
        if (dtos == null) {
            return list;
        }
        for (ActiveInfoVo dto : dtos) {
            BasicInfo.ActiveModel model = toActiveModel(dto);
            if (model != null) {
                list.add(model);
            }
        }
        return list;
    }

    /** 将接口 DTO 列表序列化为本地缓存用的 JSON 数组字符串 */
    public static String toJsonArrayString(List<ActiveInfoVo> dtos) throws JSONException {
        Gson gson = new Gson();
        JSONArray array = new JSONArray();
        if (dtos != null) {
            for (ActiveInfoVo dto : dtos) {
                if (dto != null) {
                    array.put(new JSONObject(gson.toJson(dto)));
                }
            }
        }
        return array.toString();
    }
}
