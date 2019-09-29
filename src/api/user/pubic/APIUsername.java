/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api.user.pubic;

import api.user.APIOneUser;
import caches.HzUser;
import s.models.user.User;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import org.apache.log4j.Logger;

/**
 *
 * @author ritte
 */
public class APIUsername extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIOneUser.class);

    private APIUsername() {
        super("username/{username}");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {
            String username = req.getVar("username");
            if (username == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid {username}."));
                return;
            }

            APIResponse<User> result = HzUser.getInstance().getByUsername(username);
            resp.respond(result);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APIUsername getInstance() {
        return APIUsernameHolder.INSTANCE;
    }

    private static class APIUsernameHolder {

        private static final APIUsername INSTANCE = new APIUsername();
    }
}
