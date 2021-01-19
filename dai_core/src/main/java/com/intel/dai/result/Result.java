package com.intel.dai.result;

public class Result<T> {

    private long returnCode;
    private T message;

    public Result(long returnCode, T message) {
        this.returnCode = returnCode;
        this.message = message;
    }

    public Result() {
        this(0, null);
    }

    public long getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(long returnCode) {
        this.returnCode = returnCode;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}
