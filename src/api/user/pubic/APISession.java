/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api.user.pubic;

import api.user.APIOneUser;
import caches.HzUser;
import s.models.session.Session;
import clients.authenms.SessionClient;
import s.models.user.User;
import clients.boms.BOClient;
import common.UserStatus;
import s.models.user.UToken;
import com.google.gson.Gson;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import org.apache.log4j.Logger;
import constants.UsermsCode;

/**
 *
 * @author ritte
 */
public class APISession extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIOneUser.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APISession() {
        super("session/{token}");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {

            String token = req.getVar("token");
            if (token == null || token.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid {token}."));
                return;
            }
            APIResponse<Session> sessionResult = SessionClient.getInstance().get(token);
            if (sessionResult == null || sessionResult.status != APIStatus.OK) {
                LOGGER.error("ERROR " + token + " " + GSON.toJson(sessionResult));
                resp.respond(sessionResult);
                return;
            }

            Session session = sessionResult.getFirst();
            if ("BO-ADMIN".equals(session.partner_code)) {
                APIResponse<UToken> result = BOClient.getInstance().verifyToken(token);
                resp.respond(result);
                return;
            }

            APIResponse<User> userResult = HzUser.getInstance().get(session.user_id);
            if (userResult == null || userResult.status != APIStatus.OK) {
                LOGGER.error("ERROR " + GSON.toJson(sessionResult));
                resp.respond(userResult);
                return;
            }

            User user = userResult.getFirst();
            if (UserStatus.BANNED_FOREVER.equals(user.status) || UserStatus.DELETE.equals(user.status)) {
                resp.respond(new APIResponse<>(APIStatus.NOT_FOUND, "Data not found. Not found token " + token + "!"));
                return;
            }

            if (UserStatus.BANNED.equals(user.status)) {
                resp.respond(new APIResponse<>(APIStatus.ERROR, UsermsCode.ERROR_USER_IS_LOCKED, "User is locked!"));
                return;
            }

            APIResponse<UToken> result = new APIResponse<>(APIStatus.OK, "Get token successful");
            UToken userResp = user.parse();

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

    public static APISession getInstance() {
        return APISessionlder.INSTANCE;
    }

    private static class APISessionlder {

        private static final APISession INSTANCE = new APISession();
    }
}
