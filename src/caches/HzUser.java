/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package caches;

import s.models.user.User;
import com.hazelcast.HzInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import common.api.APIResponse;
import common.api.APIStatus;
import models.user.UserDB;
import org.apache.log4j.Logger;

/**
 *
 * @author ritte
 */
public class HzUser {

    private static final Logger LOGGER = Logger.getLogger(HzUser.class);
    private static final HzInstance HZ_INSTANCE = HzInstance.getInstance();
    private static final UserDB USER_DB = UserDB.getInstance();

    private static IMap<Long, User> MAP_USER_ID;
    private static IMap<String, User> MAP_USER_BOPING_ID;
    private static IMap<String, User> MAP_USER_USERNAME;
    private static IMap<String, User> MAP_USER__ID;
    private static IMap<String, User> MAP_USER_BANK;
    private static IMap<String, User> MAP_FB_ID;

    private static IQueue<User> QUEUE_SYNC;

    private HzUser() {
        if (!HZ_INSTANCE.isNull()) {

            HzUser.MAP_USER_ID = HZ_INSTANCE.getMap("userms_user_id");
            HzUser.MAP_USER_BOPING_ID = HZ_INSTANCE.getMap("userms_user_boping_id");
            HzUser.MAP_USER_USERNAME = HZ_INSTANCE.getMap("userms_user_username");
            HzUser.MAP_USER__ID = HZ_INSTANCE.getMap("userms_user__id");
            HzUser.MAP_USER_BANK = HZ_INSTANCE.getMap("userms_user_bank");
            HzUser.MAP_FB_ID = HZ_INSTANCE.getMap("userms_user_fb_id");

            HzUser.QUEUE_SYNC = HZ_INSTANCE.getQueue("userms_user");
        }
    }

    public void clear() {
        if (!HZ_INSTANCE.isNull()) {
            HzUser.MAP_USER_ID.clear();
            HzUser.MAP_USER_BOPING_ID.clear();
            HzUser.MAP_USER_USERNAME.clear();
            HzUser.MAP_USER__ID.clear();
            HzUser.MAP_USER_BANK.clear();
            HzUser.QUEUE_SYNC.clear();
        }
    }

    public APIResponse<User> get(String _id) {
        try {
            User user = HZ_INSTANCE.isNull() ? null : MAP_USER__ID.get(_id);
            if (user == null) {
                APIResponse<User> uGet = USER_DB.get(_id);
                if (uGet != null && uGet.status == APIStatus.OK) {
                    user = uGet.getFirst();
                    if (!HZ_INSTANCE.isNull()) {
                        MAP_USER__ID.put(_id, user);
                    }
                }
                return uGet;
            }
            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get User successfully");
            apiResult.addContent(user);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internel error: " + e.getMessage());
        }
    }

    public APIResponse<User> get(long userId) {
        try {
            User user = HZ_INSTANCE.isNull() ? null : MAP_USER_ID.get(userId);
            if (user == null) {
                User query = new User();
                query.id = userId;
                APIResponse<User> uGet = USER_DB.queryOne(query);
                if (uGet != null && uGet.status == APIStatus.OK) {
                    user = uGet.getFirst();
                    if (!HZ_INSTANCE.isNull()) {
                        MAP_USER_ID.put(userId, user);
                    }
                }
                return uGet;
            }
            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get User successfully");
            apiResult.addContent(user);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internel error: " + e.getMessage());
        }
    }

    public APIResponse<User> getByUsername(String username) {
        try {
            username = username.toLowerCase();
            User user = HZ_INSTANCE.isNull() ? null : MAP_USER_USERNAME.get(username);
            if (user == null) {
                User query = new User();
                query.username = username;
                APIResponse<User> uGet = USER_DB.queryOne(query);
                if (uGet != null && uGet.status == APIStatus.OK) {
                    user = uGet.getFirst();
                    if (!HZ_INSTANCE.isNull()) {
                        MAP_USER_USERNAME.put(username, user);
                    }
                }
                return uGet;
            }
            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get User successfully");
            apiResult.addContent(user);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internel error: " + e.getMessage());
        }
    }

    public APIResponse<User> getByBopingId(String bopingId) {
        try {

            User user = HZ_INSTANCE.isNull() ? null : MAP_USER_BOPING_ID.get(bopingId);
            if (user == null) {
                User query = new User();
                query.boping_id = bopingId;
                APIResponse<User> uGet = USER_DB.queryOne(query);
                if (uGet != null && uGet.status == APIStatus.OK) {
                    user = uGet.getFirst();
                    if (!HZ_INSTANCE.isNull()) {
                        MAP_USER_BOPING_ID.put(bopingId, user);
                    }
                }
                return uGet;
            }
            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get User successfully");
            apiResult.addContent(user);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internel error: " + e.getMessage());
        }
    }

    public APIResponse<User> getByBankAccountNo(String bank) {
        try {

            User user = HZ_INSTANCE.isNull() ? null : MAP_USER_BANK.get(bank);
            if (user == null) {
                User query = new User();
                query.bank_account_no = bank;
                APIResponse<User> uGet = USER_DB.queryOne(query);
                if (uGet != null && uGet.status == APIStatus.OK) {
                    user = uGet.getFirst();
                    if (!HZ_INSTANCE.isNull()) {
                        MAP_USER_BANK.put(bank, user);
                    }
                }
                return uGet;
            }
            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get User successfully");
            apiResult.addContent(user);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internel error: " + e.getMessage());
        }
    }

    public APIResponse<User> getByFbId(String fbId) {
        try {

            User user = HZ_INSTANCE.isNull() ? null : MAP_FB_ID.get(fbId);
            if (user == null) {
                User query = new User();
                query.fb_id = fbId;
                APIResponse<User> uGet = USER_DB.queryOne(query);
                if (uGet != null && uGet.status == APIStatus.OK) {
                    user = uGet.getFirst();
                    if (!HZ_INSTANCE.isNull()) {
                        MAP_FB_ID.put(fbId, user);
                    }
                }
                return uGet;
            }
            APIResponse<User> apiResult = new APIResponse<>(APIStatus.OK, "Get User successfully");
            apiResult.addContent(user);
            return apiResult;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internel error: " + e.getMessage());
        }
    }

    public void put(User user) {
        try {

            if (HZ_INSTANCE.isNull()) {
                return;
            }

            MAP_USER_ID.put(user.id, user);
            if (user.boping_id != null && !user.boping_id.isEmpty()) {
                MAP_USER_BOPING_ID.put(user.boping_id, user);
            }
            if (user.fb_id != null && !user.fb_id.isEmpty()) {
                MAP_FB_ID.put(user.fb_id, user);
            }
            MAP_USER_USERNAME.put(user.username, user);
            MAP_USER__ID.put(user._id, user);
            if (user.bank_account_no != null && !user.bank_account_no.isEmpty()) {
                MAP_USER_BANK.put(user.bank_account_no, user);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void put(String _id) {
        try {
            if (HZ_INSTANCE.isNull()) {
                return;
            }
            APIResponse<User> gResult = USER_DB.get(_id);
            if (gResult != null && gResult.status == APIStatus.OK) {
                User user = gResult.getFirst();
                MAP_USER_ID.put(user.id, user);
                if (user.boping_id != null && !user.boping_id.isEmpty()) {
                    MAP_USER_BOPING_ID.put(user.boping_id, user);
                }
                MAP_USER_USERNAME.put(user.username, user);
                MAP_USER__ID.put(user._id, user);
                if (user.bank_account_no != null && !user.bank_account_no.isEmpty()) {
                    MAP_USER_BANK.put(user.bank_account_no, user);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void putToQueue(User user) {
        try {
            if (HZ_INSTANCE.isNull()) {
                return;
            }
            QUEUE_SYNC.add(user);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static HzUser getInstance() {
        return HzUserHolder.INSTANCE;
    }

    private static class HzUserHolder {

        private static final HzUser INSTANCE = new HzUser();
    }

}
