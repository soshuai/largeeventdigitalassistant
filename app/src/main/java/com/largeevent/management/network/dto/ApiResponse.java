package com.largeevent.management.network.dto;

/**
 * 通用 API 响应包装类。
 * <p>
 * 对应后端统一结构：{@code { "code": 200, "message"/"msg": "...", "data": T }}。
 * 业务成功判断请用 {@link #isSuccess()}（code == 200）。
 *
 * @param <T> data 字段的业务数据类型（如 {@link BasicInfoDTO}、列表、字符串等）
 */
public class ApiResponse<T> {
    /** 业务状态码，200 表示成功 */
    private int code;
    /** 提示信息（优先） */
    private String message;
    /** 提示信息（兼容字段，与 message 二选一） */
    private String msg;
    /** 业务数据体 */
    private T data;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        // 兼容 message 和 msg 两种字段
        return message != null ? message : msg;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
    
    /**
     * 判断接口调用是否成功
     */
    public boolean isSuccess() {
        return code == 200;
    }
}
