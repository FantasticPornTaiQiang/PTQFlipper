package ptq.mpga.pinance.api_module.request;

import java.util.Collections;
import java.util.Map;

public abstract class RetrofitService {
    protected final Retrofit retrofit = new Retrofit();
    protected final String key;

    protected RetrofitService(String key) {
        this.key = key;
    }
    /**
     * 初始化时进行的操作，一般是对Retrofit进行域名、Cookie、连接时间、超时时间等配置
     */
    protected abstract void init();
    /**
     * 公共请求头，经由此RetrofitService发送的所有请求将在请求前被拦截，并添加公共请求头，下同
     */
    protected Map<String, Object> publicHeaders() {
        return Collections.emptyMap();
    }
    protected Map<String, Object> publicQueries() {
        return Collections.emptyMap();
    }
    protected Map<String, Object> publicBodies() {
        return Collections.emptyMap();
    }
    /**
     * 可对请求结果进行加工
     */
    protected Map<String, Object> responseFactory(Map<String, Object> dataMap) {
        return dataMap;
    }
    /**
     * 所有可能发生的错误类型，以及对应的错误提示，例如如果发生了SocketExpection，被捕获后，应该返回“网络连接异常”
     */
    protected String errorFactory(Exception error) {
        return "";
    }
}
