/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api.user.pubic;

import clients.rabbitmq.UserRabbitMQ;
import s.models.session.Session;
import clients.authenms.SessionClient;
import s.models.user.User;
import s.models.user.UToken;
import com.google.gson.Gson;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import ultis.UserCommon;
import org.apache.log4j.Logger;
import common.utils.config.SConfig;
import constants.UsermsCode;

/**
 *
 * @author ritte
 */
public class APILogin extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APILogin.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APILogin() {
        super("login");
    }
    
    public class LoginReq {

        public String stoken;
        public String username;
        public String password;
        public String ip;
        public String os;
        public String device;
        public String browser;
    }

    @APIMethod(name = "POST")
    public void onPost(APIRequest req, APIResponder resp) {
        try {

            String content = req.getContent();
            LoginReq loginReq = null;
            Session sessionReq = null;
            if (content != null) {
                try {
                    loginReq = GSON.fromJson(content, LoginReq.class);
                    sessionReq = GSON.fromJson(content, Session.class);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }

            if (loginReq == null || sessionReq == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid data. Please put in JSON format."));
                return;
            }

            if (loginReq.username == null || loginReq.username.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_USERNAME, "Yêu cầu nhập tên tài khoản"));
                return;
            }

            if (loginReq.password == null || loginReq.password.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_PASSWORD, "Yêu cầu nhập mật khẩu"));
                return;
            }

            if (sessionReq.ip == null || sessionReq.os == null || sessionReq.device == null || sessionReq.browser == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_IP_OS_DEVICE_BROWSER,
                        "Invalid data. Required {ip}, {os}, {device}, {browser}."));
                return;
            }

            APIResponse<User> apiResult = UserCommon.login(loginReq.username, loginReq.password);
            if (apiResult == null || apiResult.status != APIStatus.OK) {
                String message = apiResult == null ? "Có lỗi xảy ra, vui lòng thử lại sau" : apiResult.message;
                if (apiResult != null && apiResult.status == APIStatus.NOT_FOUND) {
                    message = "Không tìm thấy người dùng";
                }

                resp.respond(new APIResponse(apiResult == null ? APIStatus.ERROR : apiResult.status, message));
                return;
            }
            
            APIResponse<UToken> result = new APIResponse<>(APIStatus.OK, "Đăng nhập thành công");
            
            User user = apiResult.getFirst();
            UToken userResp = user.parse();
            sessionReq.user_id = userResp.id;
            sessionReq.partner_code = SConfig.getString("setting.code");
            APIResponse<Session> sessionResult = SessionClient.getInstance().login(sessionReq);
            
            if (sessionResult == null || sessionResult.status != APIStatus.OK) {
                LOGGER.error("ERROR " + GSON.toJson(sessionReq) + " " + GSON.toJson(sessionResult));
                resp.respond(sessionResult);
                return;
            }

            Session session = sessionResult.getFirst();

            UserRabbitMQ.getInstance().sendMessageToQueue("LOGIN", session, userResp.username, loginReq.stoken);

            userResp.token = session.token;
            userResp.ip = session.ip;
            userResp.os = session.os;
            userResp.device = session.device;
            userResp.browser = session.browser;
            userResp.expried = session.expried;
            userResp.partner_code = session.partner_code;
            result.addContent(userResp);

            resp.respond(result);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APILogin getInstance() {
        return APILoginHolder.INSTANCE;
    }

    private static class APILoginHolder {

        private static final APILogin INSTANCE = new APILogin();
    }
}
