package ptq.mpga.pinance.api_module;

import ptq.mpga.pinance.api_module.request.ServiceManager;

public class App {

    //在App启动时，会触发onCreate的调用
    public void onCreate() {
        ServiceManager.instance().init();
        ServiceManager.instance().registerService(new RetrofitServiceExample());
    }
}
