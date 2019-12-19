package com.intel.dai.result;

public class Result {

    private long returnCode;
    private String message;

    public Result(long returnCode, String message) {
        this.returnCode = returnCode;
        this.message = message;
    }

    public Result() {
        this(0, "");
    }

    public long getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(long returnCode) {
        this.returnCode = returnCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
