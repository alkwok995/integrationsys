package com.gbr.integrationsys.exception;

public class ImportException extends Exception {

    //错误代码
    private String errorCode;

    //用于前台显示的信息
    private String shortMsg;

    public ImportException() {
        errorCode = "0";
    }

    public ImportException(String msg) {
        super(msg);
    }

    public ImportException(String errorCode, String msg, String shortMsg) {
        super(msg);
        this.errorCode = errorCode;
        this.shortMsg = shortMsg;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getShortMsg() {
        return shortMsg;
    }

    public void setShortMsg(String shortMsg) {
        this.shortMsg = shortMsg;
    }

    public ImportException setShortMessage(String shortMsg) {
        this.shortMsg = shortMsg;
        return this;
    }
}
