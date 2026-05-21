package com.largeevent.management.data;

import android.text.TextUtils;

import com.largeevent.management.model.CertificateInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class CertificateParser {

    static List<CertificateInfo> parse(String payload) throws JSONException {
        List<CertificateInfo> result = new ArrayList<>();
        if (TextUtils.isEmpty(payload)) {
            return result;
        }

        JSONObject root;
        JSONArray dataArray;
        try {
            root = new JSONObject(payload);
            dataArray = root.optJSONArray("certificates");
            if (dataArray == null) {
                dataArray = root.optJSONArray("data");
            }
            if (dataArray == null) {
                // maybe root itself is array
                dataArray = new JSONArray(payload);
            }
        } catch (JSONException ignore) {
            dataArray = new JSONArray(payload);
        }

        if (dataArray == null) {
            return result;
        }

        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject item = dataArray.optJSONObject(i);
            if (item == null) continue;
            CertificateInfo.Builder builder = new CertificateInfo.Builder()
                    .setName(item.optString("name"))
                    .setNumber(item.optString("number"))
                    .setDocumentType(item.optString("documentType", item.optString("docType")))
                    .setIdentityDocumentType(item.optString("identityDocumentType"))
                    .setIdentityDocumentNumber(item.optString("identityDocumentNumber"))
                    .setChipId(item.optString("chipId", item.optString("chip_id")))
                    .setCardSerial(item.optString("cardNo", item.optString("cardSerial")))
                    .setValidFrom(item.optString("validFrom", item.optString("startTime")))
                    .setValidTo(item.optString("validTo", item.optString("endTime")))
                    .setNeedBinding(item.optBoolean("needBinding", false))
                    .setBound(item.optBoolean("bound", true))
                    .setRealNameRequired(item.optBoolean("realNameRequired", true));

            JSONArray zones = item.optJSONArray("zones");
            if (zones != null) {
                for (int j = 0; j < zones.length(); j++) {
                    builder.addPermission(zones.optString(j));
                }
            } else {
                String zone = item.optString("permission");
                if (!TextUtils.isEmpty(zone)) {
                    builder.addPermission(zone);
                }
            }

            CertificateInfo info = builder.build();
            if (!TextUtils.isEmpty(info.chipId)) {
                result.add(info);
            }
        }
        return result;
    }
}



