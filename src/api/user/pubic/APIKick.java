/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api.user.pubic;

import caches.HzUser;
import s.models.session.Session;
import clients.authenms.SessionClient;
import s.models.user.UToken;
import common.AuthCommon;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import common.utils.SNumberUtils;
import constants.UsermsCode;
import ultis.UserCommon;
import org.apache.log4j.Logger;
import s.models.user.User;

/**
 *
 * @author ritte
 */
public class APIKick extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIKick.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APIKick() {
        super("kick/{id}");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {

            String userId = req.getVar("id");
            if (userId == null || userId.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_USER_ID, "Invalid {user_id}"));
                return;
            }

            UToken userAction = (UToken) req.getAttribute("user");
            if (userAction == null || !AuthCommon.isSupport(userAction)) {
                LOGGER.error("ERROR " + GSON.toJson(userAction));
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
                return;
            }

            long uId = SNumberUtils.getLong(userId, 0);
            if(uId > 0){
                APIResponse<User> uResult = HzUser.getInstance().get(uId);
                if (uResult != null && uResult.status == APIStatus.OK) {
                    User userUsed = uResult.getFirst();
                    //Send notify to vingaming
                    BasicDBObject message = new BasicDBObject();
                    message.put("username", userUsed.username);
                    message.put("user_id", userUsed.id);
                    UserCommon.sendNotifyToVin(GSON.toJson(message));
                }
            }
            APIResponse<Session> kickResult = SessionClient.getInstance().kick(userId);
            resp.respond(kickResult);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APIKick getInstance() {
        return APIKickHolder.INSTANCE;
    }

    private static class APIKickHolder {

        private static final APIKick INSTANCE = new APIKick();
    }
}
