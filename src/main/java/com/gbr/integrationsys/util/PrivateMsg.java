package com.gbr.integrationsys.util;

public class PrivateMsg {
    private String toWho;
    private String msg;
    private String success;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public PrivateMsg(String toWho, String msg) {
        this.toWho = toWho;
        this.msg = msg;
    }

    public String getToWho() {
        return toWho;
    }

    public void setToWho(String toWho) {
        this.toWho = toWho;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}