package api.user;

import s.models.user.UToken;
import common.AuthCommon;
import com.google.gson.Gson;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import common.utils.SNumberUtils;
import constants.UsermsConf;
import models.user.level.UserLevel;
import models.user.level.UserLevelDB;
import org.apache.log4j.Logger;

public class APIUserLevel extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIUserLevel.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APIUserLevel() {
        super("level");
    }

    @APIMethod(name = "GET")
    public void onQuery(APIRequest req, APIResponder resp) {
        try {

            UToken uToken = (UToken) req.getAttribute("user");
            if (uToken == null || (!AuthCommon.isSupport(uToken) && !AuthCommon.isBEToken(req))) {
                LOGGER.error("ERROR " + uToken);
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
            }

            // get query information
            String q = req.getParams("q");
            UserLevel query = null;
            if (q != null) {
                try {
                    query = GSON.fromJson(q, UserLevel.class);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }

            if (query == null) {
                query = new UserLevel();
            }

            long offset = SNumberUtils.getLong(req.getParams("offset"), 0);
            long limit = SNumberUtils.getLong(req.getParams("limit"), 20);
            boolean reverse = req.getParams("reverse") != null;

            if (limit > UsermsConf.MAX_QUERY) {
                limit = UsermsConf.MAX_QUERY;
            }

            APIResponse<UserLevel> result = UserLevelDB.getInstance().query(query, offset, limit, reverse);
            resp.respond(result);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, ex.getMessage()));
        }
    }

    public static APIUserLevel getInstance() {
        return APIUserLevelHolder.INSTANCE;
    }

    private static class APIUserLevelHolder {

        private static final APIUserLevel INSTANCE = new APIUserLevel();
    }

}
