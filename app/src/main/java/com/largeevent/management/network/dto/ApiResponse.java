package com.largeevent.management.network.dto;

/**
 * 通用 API 响应包装类
 * @param <T> 数据类型
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private String msg;
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
