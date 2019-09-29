package api.user;

import clients.rabbitmq.UserRabbitMQ;
import s.models.user.User;
import s.models.user.UToken;
import common.AuthCommon;
import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import common.utils.GsonUtils;
import common.utils.SNumberUtils;
import common.utils.SUtils;
import constants.UsermsConf;
import dbs.mongodb.MongoDB;
import ultis.UserCommon;
import models.user.UserDB;
import org.apache.log4j.Logger;
import org.bson.Document;

public class APIUser extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIUser.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private APIUser() {
        super("");
    }

    private Document getDocument(User user) throws IllegalArgumentException, IllegalAccessException {
        Document doc = GSON.fromJson(user.toString(), Document.class);
        return doc;
    }

    private void setRegex(Document query, String field, String value) {

        BasicDBObject regex = new BasicDBObject();
        regex.put("$regex", value);
        regex.put("$options", "i");
        query.put(field, regex);
    }

    @APIMethod(name = "GET")
    public void onQuery(APIRequest req, APIResponder resp) {
        try {

            UToken uToken = (UToken) req.getAttribute("user");
            if (AuthCommon.isSupport(uToken) || AuthCommon.isBEToken(req)) {

                // get query information
                String query = req.getParams("q");
                String key = req.getParams("key");
                User userQuery;
                Document basicQuery = new Document();
                if (query != null) {
                    try {
                        userQuery = GSON.fromJson(query, User.class);
                        basicQuery = getDocument(userQuery);
                        if (userQuery != null) {

                            if (userQuery.username != null) {

                                this.setRegex(basicQuery, "username", userQuery.username);
                            }

                            if (userQuery.phone != null) {

                                this.setRegex(basicQuery, "phone", userQuery.phone);
                            }

                            if (userQuery.email != null) {

                                this.setRegex(basicQuery, "email", userQuery.email);
                            }

                            if (userQuery.register_ip != null) {

                                this.setRegex(basicQuery, "register_ip", userQuery.register_ip);
                            }

                            if (userQuery.bank_account_no != null) {

                                this.setRegex(basicQuery, "bank_account_no", userQuery.bank_account_no);
                            }
                        }
                    } catch (Exception ex) {
                        resp.respond(new APIResponse(APIStatus.INVALID, "Invalid query. Please put in JSON format."));
                        return;
                    }
                }

                String fromDate = req.getParams("from_date");
                String toDate = req.getParams("to_date");

                if (fromDate != null && !fromDate.isEmpty() && toDate != null && !toDate.isEmpty()) {
                    String field = "created_time";
                    DBObject objDb = MongoDB.getDBObject_GTE_LT_ByDate_From_To(field, fromDate, toDate);
                    basicQuery.append(field, objDb.get(field));
                }

                long offset = SNumberUtils.getLong(req.getParams("offset"), 0);
                long limit = SNumberUtils.getLong(req.getParams("limit"), 20);
                boolean reverse = req.getParams("reverse") != null;

                if (limit > UsermsConf.MAX_QUERY) {
                    limit = UsermsConf.MAX_QUERY;
                }

                APIResponse<User> result = UserDB.getInstance().queryMongoDB(basicQuery, offset, limit, reverse);
                resp.respond(result);
            } else {
                LOGGER.error("ERROR " + uToken);
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
            }

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, ex.getMessage()));
        }
    }

    @APIMethod(name = "POST")
    public void onPost(APIRequest req, APIResponder resp) {
        try {

            UToken uToken = (UToken) req.getAttribute("user");
            if (AuthCommon.isSupport(uToken) || AuthCommon.isBEToken(req)) {
                String content = req.getContent();
                User user = null;
                if (content != null) {
                    try {
                        user = GSON.fromJson(content, User.class);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }

                if (user == null) {
                    resp.respond(new APIResponse(APIStatus.INVALID, "Invalid data. Please put in JSON format."));
                    return;
                }

                if (user.username == null || user.username.isEmpty() || SUtils.checkSpecialChar(user.username)) {
                    resp.respond(new APIResponse(APIStatus.INVALID,
                            "Yêu cầu nhập tên tài khoản đăng nhập"));
                    return;
                }

                APIResponse<User> result = UserCommon.add(user, uToken);
                if (result != null && result.status == APIStatus.OK) {
                    User userUsed = result.getFirst();
                    UserRabbitMQ.getInstance().sendMessageToQueue("UPDATE", userUsed, null, null);
                }
                resp.respond(result);
            } else {
                LOGGER.error("ERROR " + uToken);
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
            }

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, ex.getMessage()));
        }
    }

    public static APIUser getInstance() {
        return APIUserHolder.INSTANCE;
    }

    private static class APIUserHolder {

        private static final APIUser INSTANCE = new APIUser();
    }

}
