import clients.models.OMember;
import clients.oneworkms.OneWorldClient;
import com.google.gson.Gson;
import common.api.APIResponse;
import dbs.mongodb.MongoDB;
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
public class TestOneWorldClient {

    public static void main(String[] args) {
        String env = System.getProperty("appenv");
        if (env == null) {
            env = "dev";
        }

        SConfig.init("./conf/" + env + ".ini");

        //Init db
        MongoDB.init(SConfig.getString("mongodb.host"),
                SConfig.getInt("mongodb.port"), SConfig.getString("mongodb.name"),
                SConfig.getString("mongodb.user"), SConfig.getString("mongodb.pass"), SConfig.getString("mongodb.auth"));

        register();
    }

    public static void register() {
        OMember uowr = new OMember();
        uowr.vendor_member_id = "2";
        uowr.firstname = "Vuong";
        uowr.username = "vuong042";
        System.out.println(uowr);
        System.out.println(SConfig.getString("outbound-api.oneworldms"));
        APIResponse api = OneWorldClient.getInstance().register(uowr);
        System.out.println(new Gson().toJson(api));
    }

}
