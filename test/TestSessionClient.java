
import s.models.session.Session;
import clients.authenms.SessionClient;
import com.google.gson.Gson;
import common.api.APIResponse;
import common.utils.config.SConfig;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ritte
 */
public class TestSessionClient {

    public static void main(String[] args) {

        SConfig.init("./conf/staging.ini");

        //Init db
//        MongoDB.init(SConfig.getString("mongodb.host"),
//                SConfig.getInt("mongodb.port"), SConfig.getString("mongodb.name"),
//                SConfig.getString("mongodb.user"), SConfig.getString("mongodb.pass"), SConfig.getString("mongodb.auth"));

//        User q = new User();
//        q.id = 140l;
//        APIResponse<User> api = UserDB.getInstance().queryOne(q);
//        User user = api.getFirst();
//        user.password2 = null;
//        user.str = null;
//        user.type = null;
//        user.sync_to_onework = null;
//        user._id = null;
//
//        System.out.println(GsonUtils.GSON_UTCDATE_NORMNUMBER.toJson(user));
//        String tmp = "{\"fullname\":\"testukm7\",\"username\":\"testukm7\",\"phone\":\"0909887887\",\"password\":\"a2ed7ecbd20b20240bb5a1d0d887c818\",\"last_login\":\"Nov 7, 2018 11:01:41 AM\",\"register_ip\":\"161.202.99.140\",\"login_count\":3,\"plan_id\":256,\"odd_group\":\"B\",\"bank_code\":\"VCB\",\"bank_account_no\":\"101010198735\",\"boping_id\":\"fabet_dev2140\",\"boping_username\":\"fabet_dev2testukm7\",\"status\":\"ACTIVE\",\"id\":140,\"created_time\":\"Oct 31, 2018 7:39:25 PM\",\"last_updated_time\":\"Nov 7, 2018 11:01:41 AM\"}";
        login();
    }

    public static void getSession() {
        APIResponse<Session> apiResult = SessionClient.getInstance().get("9c23c2d86a7bc12f26cc7fe824af4b87");
        System.out.println(new Gson().toJson(apiResult));
    }

    public static void login() {
        Session session = new Session();
        session.user_id = 7L;
        session.ip = "127.0.0.1";
        session.partner_code = "Five88";
        session.browser = "chrome";
        session.device = "iphone";
        session.os = "ios";
        APIResponse<Session> apiResult = SessionClient.getInstance().login(session);
        System.out.println(new Gson().toJson(apiResult));
    }
}
