package com.largeevent.management.nfc;

/**
 * NFC回调接口
 */
public interface NfcCallback {
    /**
     * 标签检测到回调
     * @param tagId 标签ID
     * @param techList 技术列表
     */
    void onTagDetected(String tagId, String[] techList);
}