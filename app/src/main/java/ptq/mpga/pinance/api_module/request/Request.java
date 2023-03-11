package ptq.mpga.pinance.api_module.request;

import static ptq.mpga.pinance.api_module.request.Util.decodeJSON;

import java.util.Collections;
import java.util.Map;

public class Request {
    public enum Method {
        POST, PUT, DELETE, GET
    }

    public static class RequestArg {
        public RequestArg(String api) {
            this.api = api;
        }

        public Method method = Method.POST;
        public final String api;
        public Map<String, Object> query = Collections.emptyMap();
        public Map<String, Object> body = Collections.emptyMap();
        public Map<String, Object> header = Collections.emptyMap();
        public int expectedCode = 0;
        public String service = "default";        //使用哪个服务发送

        public RequestArg method(Method method) {
            this.method = method;
            return this;
        }

        public RequestArg query(Map<String, Object> query) {
            this.query = query;
            return this;
        }

        public RequestArg body(Map<String, Object> body) {
            this.body = body;
            return this;
        }

        public RequestArg header(Map<String, Object> header) {
            this.header = header;
            return this;
        }

        public RequestArg expectedCode(int expectedCode) {
            this.expectedCode = expectedCode;
            return this;
        }

        public RequestArg service(String service) {
            this.service = service;
            return this;
        }
    }

    public static Result request(RequestArg requestArg) {
        RetrofitService retrofitService = ServiceManager.instance().getService(requestArg.service);
        if (retrofitService == null) {
            return new Result("服务未注册", requestArg.expectedCode);
        }

        Retrofit retrofit = retrofitService.retrofit;
        Map<String, Object> interceptorHeaders = retrofitService.publicHeaders();
        Map<String, Object> interceptorBodies = retrofitService.publicBodies();
        Map<String, Object> interceptorQueries = retrofitService.publicQueries();
        Map<String, Object> requestHeader = requestArg.header;
        Map<String, Object> requestBody = requestArg.body;
        Map<String, Object> requestQuery = requestArg.query;

        requestHeader.putAll(interceptorHeaders);
        requestBody.putAll(interceptorBodies);
        requestQuery.putAll(interceptorQueries);

        //发送请求，获取回应数据
        try {
            Retrofit.Response response;
            switch (requestArg.method) {
                case GET:
                    response = retrofit.get(requestArg);
                    break;
                case PUT:
                    response = retrofit.put(requestArg);
                    break;
                case DELETE:
                    response = retrofit.delete(requestArg);
                    break;
                case POST:
                    response = retrofit.post(requestArg);
                    break;
                default:
                    response = Retrofit.Response.errorResponse();
            }

            if (response != null && response.data != null) {
                String json = (String) response.data;
                Map<String, Object> jsonObject = retrofitService.responseFactory(decodeJSON(json));
                int code = (int) jsonObject.get("code");
                String msg = (String) jsonObject.get("msg");
                Object data = jsonObject.get("data");

                return new Result(code, msg, data, requestArg.expectedCode);
            } else {
                return new Result("请求错误", requestArg.expectedCode);
            }
        } catch (Exception e) {
            //如果在请求远程服务器中发生异常，例如SocketException，或者实体类转换时的NullPointerException等，会经由errorFactory预处理
            return new Result(retrofitService.errorFactory(e), requestArg.expectedCode);
        }
    }

}
