package com.example.rice.testlearn.common;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 消息响应
 *
 * @author luowei
 * @date 2018/12/8 9:50
 **/
@Data
public class MessageResponse<T extends Object> {

    /**
     * 通用成功状态
     */
    public static final int CODE_SUCCESS = 0;
    /**
     * 通用失败状态
     */
    public static final int CODE_FAIL = 1;

    /**
     * 状态码
     */
    private int code;
    /**
     * 消息内容
     */
    private String msg = "";
    /**
     * 数据
     */
    private T data;

    /**
     * 创建成功响应对象
     *
     * @param code 状态码
     * @param msg  消息内容
     * @param data 数据对象
     * @return
     */
    public static <T extends Object> MessageResponse buildSuccessResponse(int code, String msg, T data) {
        MessageResponse response = new MessageResponse();
        response.code = code;
        response.msg = StringUtils.isEmpty(msg) ? "" : msg;
        response.data = data;
        return response;
    }
}
