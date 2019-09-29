package api.user.pubic;

import s.models.user.User;
import com.google.gson.Gson;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import models.user.UserDB;
import org.apache.log4j.Logger;

public class APIExistsUser extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIExistsUser.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APIExistsUser() {
        super("exists");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {

            String query = req.getParams("q");
            User userQuery = null;
            if (query != null) {
                try {
                    userQuery = GSON.fromJson(query, User.class);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }

            if (userQuery == null) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid query. Please put in JSON format."));
            }

            APIResponse<User> result = UserDB.getInstance().queryOne(userQuery);
            if (result == null || result.status != APIStatus.OK) {
                LOGGER.error("ERROR " + GSON.toJson(result));
                resp.respond(result);
                return;
            }
            
            result.getFirst().forPublic();
            resp.respond(result);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APIExistsUser getInstance() {
        return APIExistsUserHolder.INSTANCE;
    }

    private static class APIExistsUserHolder {

        private static final APIExistsUser INSTANCE = new APIExistsUser();
    }
}
