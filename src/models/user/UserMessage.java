/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.user;

import s.models.user.User;
import s.models.session.Session;
import com.google.gson.Gson;
import common.utils.GsonUtils;
import java.util.Date;
import common.utils.config.SConfig;

/**
 *
 * @author ritte
 */
public class UserMessage {

    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    public String action;
    public DataObj data;
    public User user;

    public class DataObj {

        public Long id;
        public String partner_code;
        public String fullname;
        public String username;
        public String email;
        public String phone;
        public String address;
        public Date birthday;

        public String register_ip;

        public Long plan_id;
        public String odd_group;
        public String bank_code;
        public String bank_account_no;

        public String custominfo1;
        public String custominfo2;
        public String custominfo3;
        public String custominfo4;

        public String boping_id;
        public String boping_username;
        public String source;
        public String aff_id;

        public String create_by;
        public String note;
        public String status;
        public String sync_to_onework;
        public String stoken;
        
        public String ip;
        public String os;
        public String device;
        public String browser;
    
        public Date created_time;
        public Date last_updated_time;
    }

    public void setDataObj(User user, Session sessionReq, String stoken) {
        
        this.data = GSON.fromJson(GSON.toJson(user), DataObj.class);
        if(sessionReq != null){
            this.data.ip = sessionReq.ip;
            this.data.os = sessionReq.os;
            this.data.browser = sessionReq.browser;
            this.data.device = sessionReq.device;
        }
        this.data.stoken = stoken;
        this.data.partner_code = SConfig.getString("setting.code");
    }

    public void setUser(User userReq) {
        User _user = GSON.fromJson(GSON.toJson(userReq), User.class);
        _user.password = null;
        _user.password2 = null;
        _user.reset_key = null;
        _user.str = null;
        _user.last_login = null;
        _user.login_count = null;
        this.user = _user;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}
