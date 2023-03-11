package ptq.mpga.pinance.api_module;

import java.util.Collections;

import ptq.mpga.pinance.api_module.request.Request;

public class View {

    //模拟情景：用户操作触发了“获取用户信息”的接口调用，此时需要根据userId去后端服务器获取用户数据
    public void getUserInfo() {
        int uid = 114514;

        Request.request(new Request.RequestArg("/user/info")
                .method(Request.Method.GET)
                .expectedCode(200)
                .body(Collections.singletonMap("userId", uid)))
                .onSuccess((msg, data) -> {
                    //成功则更新页面，展示用户信息
                    showUserInfo(data);
                })
                .onFailure((msg, data) -> {
                    //失败则提示用户获取失败
                    showHintMessage(msg);
                })
                .whatEver((msg, data) -> {
                    //有一些场景下，无论成功或者失败都需要进行一些后处理，可以在此进行
                });
    }

    //更新页面展示
    public void showUserInfo(Object data) {

    }

    //弹出提示消息
    public void showHintMessage(String msg) {

    }
}
