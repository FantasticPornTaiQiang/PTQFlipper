package ptq.mpga.pinance.api_module.request;

public class Result {
    private int code = 0;
    private String msg = "";
    private Object data;
    private final int expectedCode;
    private boolean isNull = false;

    private Result(int expectedCode) {
        this.expectedCode = expectedCode;
    }

    public Result(int code, String msg, Object data, int expectedCode) {
        this(expectedCode);
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Result(String msg, int expectedCode) {
        this(-1, msg, null, expectedCode);
    }

    /**
     * 上层手动标记结果已被消费，不再继续向更上层分发（更上层将无法对此条结果进行处理，onSuccess等回调不会执行）
     */
    public Result setNull() {
        isNull = true;
        return this;
    }

    /**
     * 当结果未被标记已消费，无论成功与否，都触发处理事件block
     */
    public Result whatEver(OnResult onResult) {
        if (!isNull) {
            onResult.block(msg, data);
        }
        return this;
    }

    /**
     * 当结果未被标记已消费，且预期的code与实际code相等，则认为成功，触发成功的处理事件block
     */
    public Result onSuccess(OnResult onResult) {
        if (expectedCode == code && !isNull) {
            onResult.block(msg, data);
        }
        return this;
    }

    /**
     * 当结果未被标记已消费，且预期的code与实际code不相等，则认为失败，触发失败的处理事件block
     */
    public Result onFailure(OnResult onResult) {
        if (expectedCode != code && !isNull) {
            onResult.block(msg, data);
        }
        return this;
    }



    public boolean isSuccess() {
        return expectedCode == code && !isNull;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getExpectedCode() {
        return expectedCode;
    }

    public boolean isNull() {
        return isNull;
    }

    public void setNull(boolean aNull) {
        isNull = aNull;
    }

    public interface OnResult {
         void block(String msg, Object data);
    }

}
