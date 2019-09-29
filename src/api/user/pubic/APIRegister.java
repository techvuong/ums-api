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
import common.utils.SUtils;
import ultis.UserCommon;
import org.apache.log4j.Logger;
import common.utils.config.SConfig;
import constants.UsermsCode;

/**
 *
 * @author ritte
 */
public class APIRegister extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIRegister.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APIRegister() {
        super("register");
    }

    public class StokenReq {

        public String stoken;
    }

    @APIMethod(name = "POST")
    public void onPost(APIRequest req, APIResponder resp) {
        try {

            String content = req.getContent();
            User userReq = null;
            Session sessionReq = null;
            StokenReq stokenReq = null;
            if (content != null) {
                try {
                    userReq = GSON.fromJson(content, User.class);
                    sessionReq = GSON.fromJson(content, Session.class);
                    stokenReq = GSON.fromJson(content, StokenReq.class);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }

            if (userReq == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid data. Please put in JSON format."));
                return;
            }

            if (userReq.username == null || userReq.username.isEmpty() || SUtils.checkSpecialChar(userReq.username)) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_USERNAME,
                        "Yêu cầu nhập tên tài khoản đăng nhập"));
                return;
            }

            if (userReq.username.length() < 6 || userReq.username.length() > 30) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.ERROR_USERNAME_INVALID_LESS_THAN_6_CHAR_OR_GREATER_THAN_30_CHAR,
                        "Tài khoản không hợp lệ, yêu cầu nhập nhiều hơn 6 ký tự và ít hơn 30 ký tự"));
                return;
            }

            if (userReq.fullname == null || userReq.fullname.length() < 3) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.ERROR_FULLNAME_INVALID_LESS_THAN_3_CHAR,
                        "Họ và tên không hợp lệ, yêu cầu nhập nhiều hơn 3 ký tự"));
                return;
            }

            if (userReq.password == null || userReq.password.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_PASSWORD,
                        "Yêu cầu nhập mật khẩu"));
                return;
            }

            if (userReq.password.length() < 6) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.ERROR_PASSWORD_INVALID_LESS_THAN_6_CHAR,
                        "Mật khẩu không hợp lệ, yêu cầu nhập nhiều hơn 6 ký tự"));
                return;
            }

            if (userReq.register_ip == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_REGISTER_IP,
                        "Invalid data. Required {register_ip}."));
                return;
            }

            if (sessionReq == null
                    || sessionReq.ip == null
                    || sessionReq.os == null
                    || sessionReq.device == null
                    || sessionReq.browser == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_IP_OS_DEVICE_BROWSER,
                        "Invalid data. Required {ip}, {os}, {device}, {browser}."));
                return;
            }

            String stoken = stokenReq == null || stokenReq.stoken == null ? null : stokenReq.stoken;
            if(userReq.username.contains("bot")){
                userReq.type = "BOT";
            }else if(userReq.username.contains("test")){
                userReq.type = "TESTER";
            }

            APIResponse<User> apiResult = UserCommon.register(userReq);
            if (apiResult == null || apiResult.status != APIStatus.OK) {

                LOGGER.error("ERROR REGISTER " + GSON.toJson(apiResult));
                String message = "Có lỗi xảy ra, vui lòng thử lại sau";
                if (apiResult != null && apiResult.status == APIStatus.EXISTED) {
                    message = "Tài khoản đã tồn tại";
                }
                resp.respond(new APIResponse(APIStatus.ERROR, message));
                return;
            }

            User user = apiResult.getFirst();
            UserRabbitMQ.getInstance().sendMessageToQueue("NEW", user, sessionReq, stoken);

            APIResponse<UToken> result = new APIResponse<>(APIStatus.OK, "Register successful");
            UToken userResp = user.parse();
            sessionReq.user_id = userResp.id;
            sessionReq.partner_code = SConfig.getString("setting.code");
            APIResponse<Session> sessionResult = SessionClient.getInstance().login(sessionReq);
            if (sessionResult == null || sessionResult.status != APIStatus.OK) {
                LOGGER.error("ERROR SESSION LOGIN " + GSON.toJson(sessionResult));
                resp.respond(sessionResult);
                return;
            }

            Session session = sessionResult.getFirst();
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

    public static APIRegister getInstance() {
        return APIRegisterHolder.INSTANCE;
    }

    private static class APIRegisterHolder {

        private static final APIRegister INSTANCE = new APIRegister();
    }
}
