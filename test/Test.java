
import clients.models.OSyncStatus;
import s.models.user.User;
import common.UserType;
import com.google.gson.Gson;
import com.hazelcast.nio.serialization.Data;
import com.mongodb.BasicDBObject;
import common.api.APIResponse;
import common.api.AutobuildHelper;
import common.utils.GsonUtils;
import common.utils.config.SConfig;
import common.utils.security.SRandom;
import ultis.UserCommon;
import common.utils.security.SSecurity;
import dbs.mongodb.MongoDB;
import java.util.Date;
import java.util.Random;
import models.user.UserDB;
import org.bson.Document;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ritte
 */
public class Test {

    public static void main(String[] args) {
        SConfig.init("./conf_example/dev.ini");

        AutobuildHelper.writePidFile(); // Write Pid File

        System.out.println(new Date(1569282173463l));
    }
    
    public static void setFullname(){
        //Init db
        MongoDB.init(SConfig.getString("mongodb.host"),
                SConfig.getInt("mongodb.port"),
                SConfig.getString("mongodb.name"),
                SConfig.getString("mongodb.user"),
                SConfig.getString("mongodb.pass"),
                SConfig.getString("mongodb.auth"));

        Document doc = new Document();
        setRegex(doc, "fullname", "undefinedi");
        
        
        String[] names = "Long tứ, Thi thi, Phan na, Nhật cường, Cường vip, Nap pro, Hà đầu gấu, Tứ đại gia, Mỹ thu, Thu tiểu thư, Hùng đại ca, Hạo nam, Sói ca, Sôi ca, Vũ nhị đệ, Tam đệ, Hổ đại gia".split(",");
        Random random = new Random();
        int l = names.length;
                
                
        APIResponse<User> result = UserDB.getInstance().queryMongoDB(doc, 0, 1000, false);
        for (User u : result.data) {
            int randomNumber = random.nextInt(l - 0) + 0;
            User u1 = new User();
            u1.fullname = names[randomNumber];
            UserDB.getInstance().update(u._id, u1);
        }
        System.out.println(GsonUtils.GSON_NORMAL.toJson(result));
    }

    private static void setRegex(Document query, String field, String value) {

        BasicDBObject regex = new BasicDBObject();
        regex.put("$regex", value);
        regex.put("$options", "i");
        query.put(field, regex);
    }
}
