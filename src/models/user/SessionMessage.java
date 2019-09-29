/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.user;

import s.models.session.Session;
import com.google.gson.Gson;
import common.utils.GsonUtils;
import java.util.Date;
import common.utils.config.SConfig;

/**
 *
 * @author ritte
 */
public class SessionMessage {

    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;
    public String action;
    public DataObj data;
    
    public class DataObj {
        public String _id;
        public Long user_id;
        public String username;
        public String token;
        public String stoken;
        public String ip;
        public String os;
        public String device;
        public String browser;
        public String partner_code;
        public Date expried;
    }
    
    public SessionMessage(String _action, Session session, String username, String stoken) {
        this.action = _action;
        this.data = GSON.fromJson(GSON.toJson(session), DataObj.class);
        this.data.partner_code = SConfig.getString("setting.code");
        this.data.stoken = stoken;
    }

    @Override
    public String toString() {
        return GSON.toJson(this);
    }
}
