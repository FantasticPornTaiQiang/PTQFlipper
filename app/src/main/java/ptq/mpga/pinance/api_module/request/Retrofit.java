package ptq.mpga.pinance.api_module.request;

import java.util.Collections;
import java.util.Map;

/**
 * 模拟retrofit
 */
public class Retrofit {

    public Response post(Request.RequestArg requestArg) {
        return new Response();
    }

    public Response delete(Request.RequestArg requestArg) {
        return new Response();
    }

    public Response put(Request.RequestArg requestArg) {
        return new Response();
    }

    public Response get(Request.RequestArg requestArg) {
        return new Response();
    }

    public void configRetrofit(Config config) {

    }

    public static class Config {
        public String baseUrl;
        public int socketTime;

        public Config(String baseUrl, int socketTime) {
            this.baseUrl = baseUrl;
            this.socketTime = socketTime;
        }
    }

    public static class Response {
        public Object data;
        public Map<String, Object> headers = Collections.emptyMap();
        public int statusCode = 200;
        public String statusMsg = "";
        public boolean isRedirect = false;

        public static Response errorResponse() {
            return new Response();
        }
    }

}
