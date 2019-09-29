package api.user;

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

public class APIUserSyncToOnework extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIUserSyncToOnework.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APIUserSyncToOnework() {
        super("ow/sync");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {

            UToken uToken = (UToken) req.getAttribute("user");
            if (uToken == null) {
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Permision deny"));
                return;
            }

            if (uToken.boping_id == null || uToken.boping_id.isEmpty()) {
                
                UserCommon.syncAccountToOnework(uToken);
            }

            resp.respond(new APIResponse(APIStatus.OK, "Sync data to Onework successful"));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APIUserSyncToOnework getInstance() {
        return APIUserSyncToOneworkHolder.INSTANCE;
    }

    private static class APIUserSyncToOneworkHolder {

        private static final APIUserSyncToOnework INSTANCE = new APIUserSyncToOnework();
    }
}
