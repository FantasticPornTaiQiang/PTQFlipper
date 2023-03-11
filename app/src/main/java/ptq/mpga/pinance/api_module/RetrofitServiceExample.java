package ptq.mpga.pinance.api_module;

import java.net.SocketException;
import java.util.Map;

import ptq.mpga.pinance.api_module.request.Retrofit;
import ptq.mpga.pinance.api_module.request.RetrofitService;

public class RetrofitServiceExample extends RetrofitService {

    public RetrofitServiceExample() {
        super("ServiceA");
    }

    @Override
    protected void init() {
        //配置retrofit，包括域名，超时时间，连接时间，网络协议，用户代理等等
        retrofit.configRetrofit(new Retrofit.Config("11.45.14.233", 1500));
    }

    //使用publicHeaders添加所有经此Service发送的公共请求头，例如这里给所有请求都加上Cookie
    @Override
    protected Map<String, Object> publicHeaders() {
        Map<String, Object> map = super.publicHeaders();
        map.put("Cookie", "xxxxxxxx");
        return map;
    }

    //处理异常
    @Override
    protected String errorFactory(Exception error) {
        if (error instanceof NullPointerException) {
            return "数据转换异常：" + error.getMessage();
        } else if (error instanceof SocketException) {
            return "网络连接异常：" + error.getMessage();
        }
        return super.errorFactory(error);
    }
}
