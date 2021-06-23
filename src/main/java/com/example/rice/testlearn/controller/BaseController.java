package com.example.rice.testlearn.controller;

import com.example.rice.testlearn.common.MessageResponse;

public class BaseController {

    protected MessageResponse generateSuccessResult(Object data) {
        return generateResult(MessageResponse.CODE_SUCCESS, "ok", data);
    }

    protected MessageResponse generateFailureResult(int code, String msg) {
        return generateResult(code, msg, "");
    }

    private MessageResponse generateResult(int code, String msg, Object data) {
        return MessageResponse.buildSuccessResponse(code, msg, data);
    }

}

