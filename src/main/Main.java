/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import api.user.pubic.APIUsername;
import api.user.APIOneUser;
import api.user.APISyncUserToQueue;
import api.user.APIUser;
import api.user.APIUserLevel;
import api.user.APIUserSyncToOnework;
import api.user.pubic.APIBank;
import api.user.pubic.APIExistsUser;
import api.user.pubic.APIFbLogin;
import api.user.pubic.APIKick;
import api.user.pubic.APILogin;
import api.user.pubic.APILogout;
import api.user.pubic.APIRegister;
import api.user.pubic.APISession;
import app.worker.JMicroWorker;
import caches.HzUser;
import com.hazelcast.HzClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.rabbitmq.inst.RabbitConfig;
import com.rabbitmq.inst.RabbitInstance;
import s.models.user.UToken;
import common.api.APIActionHandler;
import common.api.APIBlueprint;
import common.api.APIContainer;
import common.api.APIFactory;
import common.api.APIProtocol;
import common.api.APIResponse;
import common.api.APIServer;
import common.api.APIStatus;
import common.api.AutobuildHelper;
import dbs.mongodb.MongoDB;
import java.io.IOException;
import ultis.UserCommon;
import common.utils.config.SConfig;
import worker.WorkerManager;

/**
 *
 * @author ritte
 */
public class Main {

    public static void main(String[] args) throws IOException {

        String env = System.getProperty("appenv");
        if (env == null) {
            env = "dev";
        }

        SConfig.init("./conf/" + env + ".ini");

        AutobuildHelper.writePidFile(); // Write Pid File

        //Init db
        MongoDB.init(SConfig.getString("mongodb.host"),
                SConfig.getInt("mongodb.port"),
                SConfig.getString("mongodb.name"),
                SConfig.getString("mongodb.user"),
                SConfig.getString("mongodb.pass"),
                SConfig.getString("mongodb.auth"));

        //Init Hazelcast
        String addrs = SConfig.getString("hazelcast.address");
        if (addrs != null && !addrs.isEmpty()) {
            ClientConfig cf = new ClientConfig();
            ClientNetworkConfig nw = cf.getNetworkConfig();
            for (String addr : addrs.split(",")) {
                nw.addAddress(addr);
            }
            HzClient hzClient = new HzClient();
            hzClient.setConfig(cf);
            hzClient.start();
            HzUser.getInstance().clear();
        }

        //Init RabbitMQ
        if ("1".equals(SConfig.getString("rabbitmq.enable"))) {
            new RabbitInstance()
                    .doConnect(new RabbitConfig(
                            SConfig.getString("rabbitmq.host"),
                            SConfig.getInt("rabbitmq.port"),
                            SConfig.getString("rabbitmq.user"),
                            SConfig.getString("rabbitmq.pass")
                    ))
                    .done();
        }

        //Init Async
        JMicroWorker.getInstance().setPollSize(10).launchAsyncQueue();

        //Create User Admin
//        UserCommon.addAdmin(); 
        //init container
        APIContainer container = new APIContainer();
        initRouting(container);

        //HTTP Server
        APIServer server = APIFactory.createAPIServer(APIProtocol.HTTP, "USERMS Service");
        server.setConfiguration(SConfig.getString("app.host"), SConfig.getInt("app.port"));
        server.setBeforeRequest(beforeRequest());
        server.setContainer(container);
        server.start();

        //Thrift Server
        APIServer thriftServer = APIFactory.createAPIServer(APIProtocol.THRIFT, "USERMS Service");
        thriftServer.setConfiguration(SConfig.getString("app.host"), SConfig.getInt("app.port-thrift"));
        thriftServer.setBeforeRequest(beforeRequest());
        thriftServer.setContainer(container);
        thriftServer.start();

        //Worker
        WorkerManager.reSyncToOnework();

    }

    private static void initRouting(APIContainer container) {

        APIBlueprint pb = new APIBlueprint(SConfig.getString("api-path.root"));

        //Login
        pb.addResource(APILogin.getInstance());
        pb.addResource(APIFbLogin.getInstance());

        pb.addResource(APILogout.getInstance());
        pb.addResource(APIKick.getInstance());
        pb.addResource(APIRegister.getInstance());
        pb.addResource(APISession.getInstance());
        pb.addResource(APISyncUserToQueue.getInstance());
        container.addBlueprint(pb);

        pb = new APIBlueprint(SConfig.getString("api-path.user"));
        pb.addResource(APIUsername.getInstance());
        pb.addResource(APIExistsUser.getInstance());
        pb.addResource(APIBank.getInstance());
        pb.addResource(APIOneUser.getInstance());
        pb.addResource(APIUser.getInstance());
        pb.addResource(APIUser.getInstance());
        pb.addResource(APIUserLevel.getInstance());
        pb.addResource(APIUserSyncToOnework.getInstance());
        container.addBlueprint(pb);
    }

    private static APIActionHandler beforeRequest() {
        return (req, resp) -> {

//            String ips = SConfig.getString("client.ips");
//            String ip = req.getClientIP();
//            if (ips == null || (!ips.equals("*") && !Arrays.asList(ips.split(",")).contains(ip))) {
//                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, String.format("Permision deny {%s} !", ip)));
//                return;
//            }
            String token = req.getHeader("X-TOKEN");
            if (token != null) {
                APIResponse<UToken> apiResult = UserCommon.authToken(token);
                if (apiResult == null || apiResult.status != APIStatus.OK) {
                    resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Permision deny, token " + token + " invalid!"));
                    return;
                }
                req.putAttribute("user", apiResult.getFirst());
            }

        };
    }
}
