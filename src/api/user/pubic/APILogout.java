/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api.user.pubic;

import caches.HzUser;
import clients.rabbitmq.UserRabbitMQ;
import s.models.session.Session;
import clients.authenms.SessionClient;
import clients.oneworkms.OneWorldClient;
import s.models.user.User;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import org.apache.log4j.Logger;
import constants.UsermsConf;
import ultis.UserCommon;

/**
 *
 * @author ritte
 */
public class APILogout extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APILogout.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APILogout() {
        super("logout");
    }

    class LogoutReq {

        public String stoken;
        public String token;
        public String ip;
        public String os;
        public String device;
        public String browser;
    }

    final int INVALID_TOKEN = 111;

    @APIMethod(name = "POST")
    public void onPost(APIRequest req, APIResponder resp) {
        try {

            String content = req.getContent();
            LogoutReq logoutReq = null;
            if (content != null) {
                try {
                    logoutReq = GSON.fromJson(content, LogoutReq.class);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

            if (logoutReq == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid data"));
                return;
            }

            if (logoutReq.token == null || logoutReq.token.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid {token}"));
                return;
            }

            APIResponse<Session> logoutResult = SessionClient.getInstance().logout(logoutReq.token);
            if (logoutResult != null && logoutResult.status == APIStatus.OK) {
                Session session = logoutResult.getFirst();
                if (session != null) {
                    
                    if (UsermsConf.ONEWORK_CONF.isEnable()) {
                        OneWorldClient.getInstance().kick(session.user_id);
                    }
                    APIResponse<User> uResult = HzUser.getInstance().get(session.user_id);
                    if (uResult != null && uResult.status == APIStatus.OK) {
                        User userUsed = uResult.getFirst();
                        //Send notify to vingaming
                        BasicDBObject message = new BasicDBObject();
                        message.put("username", userUsed.username);
                        message.put("user_id", userUsed.id);
                        UserCommon.sendNotifyToVin(GSON.toJson(message));
                        
                        session.browser = logoutReq.browser;
                        session.ip = logoutReq.ip;
                        session.os = logoutReq.os;
                        session.device = logoutReq.device;
                        UserRabbitMQ.getInstance().sendMessageToQueue("LOGOUT", session, userUsed.username, logoutReq.stoken);
                    }

                }

            }
            resp.respond(logoutResult);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APILogout getInstance() {
        return APILogoutHolder.INSTANCE;
    }

    private static class APILogoutHolder {

        private static final APILogout INSTANCE = new APILogout();
    }
}
