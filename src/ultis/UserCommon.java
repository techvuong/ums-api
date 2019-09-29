/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ultis;

import app.worker.JMicroWorker;
import caches.HzUser;
import clients.rabbitmq.UserRabbitMQ;
import s.models.session.Session;
import clients.authenms.SessionClient;
import s.models.user.User;
import clients.boms.BOClient;
import clients.models.OMember;
import clients.models.OMemberResp;
import clients.models.OSyncStatus;
import clients.oneworkms.OneWorldClient;
import common.UserStatus;
import common.UserType;
import s.models.user.UToken;
import com.google.gson.Gson;
import common.api.APIClient;
import common.api.APIFactory;
import common.api.APIOutRequest;
import common.api.APIProtocol;
import common.api.APIResponse;
import common.api.APIStatus;
import common.utils.SUtils;
import java.util.Date;
import models.user.UserDB;
import org.apache.log4j.Logger;
import common.utils.config.SConfig;
import common.utils.security.SRandom;
import common.utils.security.SSecurity;
import constants.UserLevelConstant;
import constants.UserLevelStatusConstant;
import constants.UsermsCode;
import constants.UsermsConf;
import java.util.Calendar;
import java.util.GregorianCalendar;
import models.user.FbReq;
import models.user.UserService;
import models.user.level.UserLevel;
import models.user.level.UserLevelDB;

/**
 *
 * @author ritte
 */
public class UserCommon {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = Logger.getLogger(UserCommon.class);
    private static final HzUser HZ_USER = HzUser.getInstance();

    public static APIResponse<User> login(String _username, String _password) {
        try {

            APIResponse<User> apiResult = HZ_USER.getByUsername(_username);
            if (apiResult == null || apiResult.status != APIStatus.OK) {
                LOGGER.error("ERROR GET USER " + GSON.toJson(apiResult));
                return apiResult;
            }

            User user = apiResult.getFirst();
            String password = SUtils.md5(SUtils.md5(_password) + user.str);

            if (UserStatus.BANNED_FOREVER.equals(user.status) || UserStatus.DELETE.equals(user.status)) {
                LOGGER.error("ERROR " + _username + " " + user);
                return new APIResponse<>(APIStatus.NOT_FOUND, "Không tìm thấy người dùng " + _username + "!");
            }

            if (user.password == null || !user.password.equals(password)) {
                LOGGER.error("ERROR " + _username + " " + _password + " " + user);
                LOGGER.error(SSecurity.decrypt(user.password2, user.str) + " " + password);
                return new APIResponse<>(APIStatus.ERROR, UsermsCode.ERROR_USERNAME_PASSWORD_INCORRECT, "Tên đăng nhập và mật khẩu không đúng");
            }

            if (UserStatus.BANNED.equals(user.status)) {
                LOGGER.error("ERROR " + _username + " " + _password + " " + user);
                return new APIResponse<>(APIStatus.ERROR, UsermsCode.ERROR_USER_IS_BANNED, "Tên đăng nhập và mật khẩu không đúng");
            }

            user.last_login = new Date();
            user.login_count = user.login_count != null ? user.login_count + 1 : 1;

            User u = new User();
            u.last_login = user.last_login;
            u.login_count = user.login_count;
            UserService.update(user._id, u);
            HzUser.getInstance().put(user);

            APIResponse<User> loginResult = new APIResponse<>(APIStatus.OK, "Đăng nhập thành công");
            loginResult.addContent(user);
            return loginResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }

    public static APIResponse<User> fbLogin(FbReq fbReq) {
        try {

            APIResponse<User> apiResult = HZ_USER.getByFbId(fbReq.fb_id);
            if (apiResult == null || (apiResult.status != APIStatus.OK && apiResult.status != APIStatus.NOT_FOUND)) {
                LOGGER.error("ERROR GET USER " + GSON.toJson(apiResult));
                return apiResult;
            }

            if (apiResult.status == APIStatus.NOT_FOUND) {
                
                User user = new User();
                user.fb_id = fbReq.fb_id;
                user.username = fbReq.fb_id;
                user.fullname = fbReq.fullname;
                user.aff_id = fbReq.aff_id;
                user.email = fbReq.email;
                user.phone = fbReq.phone;
                return register(user);
            }

            User user = apiResult.getFirst();

            if (UserStatus.BANNED_FOREVER.equals(user.status) || UserStatus.DELETE.equals(user.status)) {
                LOGGER.error("ERROR " + fbReq.fb_id + " " + user);
                return new APIResponse<>(APIStatus.NOT_FOUND, "Không tìm thấy người dùng " + fbReq.fb_id + "!");
            }

            if (UserStatus.BANNED.equals(user.status)) {
                LOGGER.error("ERROR " + fbReq.fb_id + " " + user);
                return new APIResponse<>(APIStatus.ERROR, UsermsCode.ERROR_USER_IS_BANNED, "Tên đăng nhập và mật khẩu không đúng");
            }

            user.last_login = new Date();
            user.login_count = user.login_count != null ? user.login_count + 1 : 1;

            User u = new User();
            u.last_login = user.last_login;
            u.login_count = user.login_count;
            UserService.update(user._id, u);
            HzUser.getInstance().put(user);

            APIResponse<User> loginResult = new APIResponse<>(APIStatus.OK, "Đăng nhập thành công");
            loginResult.addContent(user);
            return loginResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }

    public static APIResponse<User> register(User userReq) {
        try {
            userReq.last_login = new Date();
            userReq.login_count = 1;
            userReq.status = UserStatus.ACTIVE;
            APIResponse<User> apiResult = add(userReq);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }

    public static APIResponse<User> add(User userReq) {
        return add(userReq, null);
    }

    public static APIResponse<User> add(User userReq, UToken userAction) {
        try {

            userReq.username = userReq.username.toLowerCase();
            if (userReq.email != null) {
                userReq.email = userReq.email.toLowerCase();
            }

            APIResponse<User> getResult = HZ_USER.getByUsername(userReq.username);
            if (getResult == null || (getResult.status != APIStatus.OK && getResult.status != APIStatus.NOT_FOUND)) {
                LOGGER.error("ERROR " + GSON.toJson(getResult));
                return getResult;
            }

            if (getResult.status == APIStatus.OK) {
                return new APIResponse<>(APIStatus.EXISTED, String.format("User {%s} existed", userReq.username));
            }

            if (userAction != null) {
                userReq.created_by = userAction.id + ":" + userAction.fullname + ":" + userAction.partner_code;
            }

            String password = userReq.password;

            if (password != null) {
                String str = new SRandom(10).nextString();
                userReq.str = str;
                userReq.password = SUtils.md5(SUtils.md5(password) + str);
                userReq.password2 = SSecurity.encrypt(password, str);
            }

            userReq.type = userReq.type == null ? UserType.USER : userReq.type;
            if (UsermsConf.ONEWORK_CONF.isEnable()) {
                userReq.sync_to_onework = OSyncStatus.PENDING;
            }

            userReq.level = UserLevelConstant.LEVEL0;
            APIResponse<User> apiResult = UserService.create(userReq);
            if (apiResult != null && apiResult.status == APIStatus.OK) {
                User user = apiResult.getFirst();
                HZ_USER.put(user);
                putLevel(user, userAction);
            }
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }

    public static APIResponse<User> update(String id, User userReq) {
        return update(id, userReq, null);
    }

    public static APIResponse<User> update(String _id, User userReq, UToken userAction) {
        try {

            APIResponse<User> userResult = HZ_USER.get(_id);
            if (userResult == null || userResult.status != APIStatus.OK) {
                LOGGER.error("ERROR GET " + GSON.toJson(userResult));
                return userResult;
            }

            User u = userResult.getFirst();
            if (UserStatus.BANNED_FOREVER.equals(u.status) || UserStatus.DELETE.equals(u.status)) {
                return new APIResponse(APIStatus.NOT_FOUND, "Not foud user " + _id + "!");
            }

            if (userReq.username != null) {
                userReq.username = null;
            }

            if (userReq.password != null) {
                String password = userReq.password;
                String str = new SRandom(10).nextString();
                userReq.str = str;
                userReq.password = SUtils.md5(SUtils.md5(password) + str);
                userReq.password2 = SSecurity.encrypt(password, str);
            }

            if (userReq.status != null && userReq.status.isEmpty()) {
                userReq.status = null;
            }
            
            if(userReq.phone != null){
                userReq.is_phone_active = true;
            }

            APIResponse<User> apiResult = UserService.update(_id, userReq);
            if (apiResult != null && apiResult.status == APIStatus.OK) {

                userReq.id = u.id;

                if (userReq.sync_to_onework != null && userReq.sync_to_onework.equals(OSyncStatus.RE_SYNC)) {
                    syncAccountToOnework(u);
                    HZ_USER.put(u._id);
                    return apiResult;
                }

                APIResponse<User> getResult = UserDB.getInstance().get(_id);
                User userUsed = getResult.getFirst();
                if (userReq.group != null && !userReq.group.isEmpty()
                        && !userReq.group.equals(u.group)) {
                    UserRabbitMQ.getInstance().sendMessageToQueueAfterChangeOddGroup(userUsed);
                }
                apiResult.addContent(userUsed);
                HZ_USER.put(userUsed);
                if (userReq.level != null) {
                    putLevel(userUsed, userAction);
                }
            }
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Có lỗi xảy ra, vui lòng thử lại sau");
        }
    }

    public static void putLevel(User user, UToken userAction) {
        try {
            Runnable rn = new Runnable() {
                @Override
                public void run() {
                    try {
                        long offset = 0;
                        long limit = 100;
                        UserLevel query = new UserLevel();
                        query.user_id = user.id;
                        query.status = UserLevelStatusConstant.ACTIVE;
                        APIResponse<UserLevel> levelQuery = UserLevelDB.getInstance().query(query, offset, limit, true);
                        while (levelQuery != null && levelQuery.status == APIStatus.OK) {
                            for (UserLevel levelUsed : levelQuery.data) {
                                UserLevel lUpdate = new UserLevel();
                                lUpdate.status = UserLevelStatusConstant.CANCEL;
                                UserLevelDB.getInstance().update(levelUsed._id, lUpdate);
                            }
                            offset += levelQuery.data.size();
                            levelQuery = UserLevelDB.getInstance().query(query, offset, limit, true);
                        }

                        UserLevel level = new UserLevel();
                        level.user_id = user.id;
                        level.level = user.level;
                        level.status = UserLevelStatusConstant.ACTIVE;
                        GregorianCalendar cal = new GregorianCalendar();
                        cal.setTime(new Date());
                        level.date_index = (long) (cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH));
                        level.create_by = user.id + ":" + user.fullname + ":" + SConfig.getString("setting.code");
                        if (userAction != null) {
                            level.create_by = userAction.id + ":" + userAction.fullname + ":" + userAction.partner_code;
                        }

                        UserLevelDB.getInstance().create(level);
                    } catch (Exception ex) {
                        LOGGER.error(ex.getMessage(), ex);
                    }
                }
            };
            JMicroWorker.getInstance().put(rn);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static APIResponse<UToken> authToken(String token) {
        try {

            APIResponse<Session> sessionResult = SessionClient.getInstance().get(token);
            if (sessionResult == null || sessionResult.status != APIStatus.OK) {
                LOGGER.equals("ERROR " + GSON.toJson(sessionResult));
                String message = sessionResult != null ? sessionResult.message : "An error occurred, please try again later";
                return new APIResponse<>(APIStatus.ERROR, message);
            }

            Session session = sessionResult.getFirst();
            if ("BO-ADMIN".equals(session.partner_code)) {
                APIResponse<UToken> result = BOClient.getInstance().verifyToken(token);
                return result;
            }

            APIResponse<User> userResult = HZ_USER.get(session.user_id);
            if (userResult == null || userResult.status != APIStatus.OK) {
                String message = userResult != null ? userResult.message : "An error occurred, please try again later";
                return new APIResponse<>(APIStatus.ERROR, message);
            }
            APIResponse<UToken> apiResult = new APIResponse<>(APIStatus.OK, "Auth successful");
            UToken userResp = GSON.fromJson(GSON.toJson(userResult.getFirst()), UToken.class);

            userResp.token = session.token;
            userResp.ip = session.ip;
            userResp.os = session.os;
            userResp.device = session.device;
            userResp.browser = session.browser;
            userResp.expried = session.expried;
            userResp.partner_code = session.partner_code;
            apiResult.addContent(userResp);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse(APIStatus.ERROR, "Internal error: " + e.getMessage());
        }
    }

    public static void addAdmin() {
        String code = SConfig.getString("setting.code");
        if (code.toUpperCase().contains("BO-ADMIN")) {
            User user = new User();
            user.username = "admin";
            user.fullname = "Admin";
            user.email = "admin@mic.com";
            user.phone = "0909889789";
            user.password = "hihihi";
            user.type = UserType.ADMIN;

            APIResponse<User> apiResult = HZ_USER.getByUsername(user.username);
            if (apiResult != null && apiResult.status == APIStatus.NOT_FOUND) {
                APIResponse<User> apir = UserCommon.add(user, null);
            }
        }
    }

    public static void syncAccountToOnework(User user) {
        try {

            if (!UsermsConf.ONEWORK_CONF.isEnable()) {
                return;
            }

            Runnable rn = new Runnable() {
                @Override
                public void run() {

                    OMember uowr = new OMember();
                    uowr.vendor_member_id = "" + user.id;
                    uowr.username = user.username;
                    uowr.firstname = user.fullname;
                    APIResponse<OMemberResp> result = OneWorldClient.getInstance().register(uowr);
                    User _user = new User();
                    _user.sync_to_onework = OSyncStatus.SUCCESS;
                    if (result == null || (result.status != APIStatus.OK && result.status != APIStatus.EXISTED)) {

                        String message = result == null ? "System error" : result.message;
                        _user.sync_to_onework = result != null && result.status == APIStatus.MAINTENANCE
                                ? OSyncStatus.RE_SYNC : OSyncStatus.FAILED;
                        _user.note = message;
                    } else {
                        OMemberResp member = result.getFirst();
                        _user.boping_id = member.boping_id;
                        _user.boping_username = member.boping_username;
                    }
                    UserCommon.update(user._id, _user);
                }
            };

            JMicroWorker.getInstance().put(rn);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    private static final APIClient<Object> CLIENT_WSMS = APIFactory.createAPIClient(APIProtocol.THRIFT,
            SConfig.getString("outbound-api.swms"), Object.class, 100);

    public static void sendNotifyToVin(String content) {
        try {
            APIOutRequest req = new APIOutRequest();
            req.method = "POST";
            req.resource = "vingame/notify";
            req.params.put("type", "LOGOUT");
            req.content = content;
            CLIENT_WSMS.call(req);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
