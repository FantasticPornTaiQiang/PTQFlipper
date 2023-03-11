package ptq.mpga.pinance.api_module.request;

import java.util.Collections;
import java.util.Map;

/**
 * 单例模式
 */
public class ServiceManager {

    private static class ServiceManager_ {
        private static final ServiceManager INSTANCE = new ServiceManager();
    }

    private ServiceManager() { }

    public static ServiceManager instance() {
        return ServiceManager_.INSTANCE;
    }

    public void init() { }

    /**
     * 管理所有Service
     */
    private final Map<String, RetrofitService> serviceMap = Collections.emptyMap();

    public void registerService(RetrofitService service) {
        service.init();
        String key = service.key;
        serviceMap.put(key, service);
    }

    RetrofitService getService(String serviceKey) {
        if (serviceMap.containsKey(serviceKey)) {
            return serviceMap.get(serviceKey);
        }
        return null;
    }
}
