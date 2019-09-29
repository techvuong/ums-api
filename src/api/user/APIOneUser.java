package api.user;

import caches.HzUser;
import clients.rabbitmq.UserRabbitMQ;
import s.models.user.User;
import s.models.user.UToken;
import common.AuthCommon;
import com.google.gson.Gson;
import common.UserStatus;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import common.utils.SNumberUtils;
import constants.UserLevelConstant;
import constants.UsermsCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ultis.UserCommon;
import org.apache.log4j.Logger;

public class APIOneUser extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIOneUser.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;
    private static final HzUser HZ_USER = HzUser.getInstance();

    private APIOneUser() {
        super("/{id}");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {

            UToken uToken = (UToken) req.getAttribute("user");
            String ids = req.getVar("id");
            String type = req.getParams("type");

            if ("boping".equals(type)) {
                APIResponse<User> apiResult = HZ_USER.getByBopingId(ids);
                resp.respond(apiResult);
                return;
            }

            if (ids == null || ids.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid {id}."));
                return;
            }

            List<User> users = new ArrayList<>();
            for (String _id : ids.split(",")) {
                Long id = SNumberUtils.getLong(_id, 0);
                APIResponse<User> result;
                if (id > 0) {
                    result = HZ_USER.get(id);
                } else {
                    result = HZ_USER.get(_id);
                }

                if (result != null && result.status == APIStatus.OK) {
                    User user = result.getFirst();
                    users.add(user);
                }
            }

            if (users.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.NOT_FOUND, "Not found user {" + ids + "}."));
                return;
            }

            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get user successful {" + ids + "}");
            apiResult.setContent(users);
            resp.respond(apiResult);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public class StokenReq {

        public String stoken;
    }

    @APIMethod(name = "PUT")
    public void onPut(APIRequest req, APIResponder resp) {
        try {

            UToken userAction = (UToken) req.getAttribute("user");
            String content = req.getContent();

            LOGGER.info("PUT " + content);

            String _id = req.getVar("id");
            if (_id == null || _id.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_ID, "Invalid id."));
                return;
            }

            long id = SNumberUtils.getLong(_id, 0);
            if (id > 0) {
                APIResponse<User> apiResult = HZ_USER.get(id);
                if (apiResult == null || apiResult.status != APIStatus.OK) {
                    LOGGER.error("ERROR " + GSON.toJson(apiResult));
                    APIStatus status = apiResult == null ? APIStatus.ERROR : apiResult.status;
                    String message = apiResult == null ? "Hệ thống quản lý đang bận, không tìm thấy tải khoản." : apiResult.message;
                    resp.respond(new APIResponse(status, message));
                    return;
                }
                _id = apiResult.getFirst()._id;
            }

            if (AuthCommon.isBEToken(req) || userAction != null && (AuthCommon.isSupport(userAction) || AuthCommon.isOwner(userAction, _id))) {

                User user = null;
                StokenReq stokenReq = null;
                if (content != null) {
                    try {
                        user = GSON.fromJson(content, User.class);
                        stokenReq = GSON.fromJson(content, StokenReq.class);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }

                if (user == null) {
                    resp.respond(new APIResponse(APIStatus.INVALID, "Invalid data. Please put in JSON format."));
                    return;
                }

                String stoken = stokenReq == null || stokenReq.stoken == null ? null : stokenReq.stoken;

                if (user.level != null) {
                    String[] levels = new String[]{
                        UserLevelConstant.LEVEL0,
                        UserLevelConstant.LEVEL1,
                        UserLevelConstant.LEVEL2,
                        UserLevelConstant.LEVEL3,};
                    if (!Arrays.asList(levels).contains(user.level)) {
                        LOGGER.error("ERROR INVALID LEVEL " + user);
                        resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_LEVEL, "Invalid {level}"));
                        return;
                    }

                    if (!AuthCommon.isSupport(userAction)) {
                        LOGGER.error("ERROR UNAUTHORIZED " + GSON.toJson(userAction));
                        resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
                    }
                }

                APIResponse<User> result = UserCommon.update(_id, user, userAction);
                if (result != null && result.status == APIStatus.OK) {
                    User userUsed = result.getFirst();
                    UserRabbitMQ.getInstance().sendMessageToQueue("UPDATE", userUsed, null, stoken);
                }
                resp.respond(result);
            } else {
                LOGGER.error("ERROR UNAUTHORIZED " + GSON.toJson(userAction));
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
            }

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    @APIMethod(name = "DELETE")
    public void onDelete(APIRequest req, APIResponder resp) {
        try {

            UToken userAction = (UToken) req.getAttribute("user");
            APIResponse authResult = AuthCommon.authOwner(userAction, null);
            if (authResult == null || authResult.status != APIStatus.OK) {
                resp.respond(authResult);
                return;
            }

            String id = req.getVar("id");
            if (id == null || id.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, UsermsCode.INVALID_ID, "Invalid id."));
                return;
            }

            User update = new User();
            update.status = UserStatus.DELETE;

            APIResponse<User> result = UserCommon.update(id, update, userAction);
            resp.respond(result);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, "Internal error: " + ex.getMessage()));
        }
    }

    public static APIOneUser getInstance() {
        return APIOneUserHolder.INSTANCE;
    }

    private static class APIOneUserHolder {

        private static final APIOneUser INSTANCE = new APIOneUser();
    }
}
