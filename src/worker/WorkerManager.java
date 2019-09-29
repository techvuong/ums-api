/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package worker;

import app.worker.JMicroWorker;
import app.worker.JMicroWorkerOptions;
import clients.models.OSyncStatus;
import com.mongodb.BasicDBObject;
import common.api.APIResponse;
import common.api.APIStatus;
import constants.UsermsConf;
import java.util.concurrent.TimeUnit;
import ultis.UserCommon;
import models.user.UserDB;
import org.apache.log4j.Logger;
import org.bson.Document;
import s.models.user.User;

/**
 *
 * @author ritte
 */
public class WorkerManager {

    private static final Logger LOGGER = Logger.getLogger("sync");
    
    public static void reSyncToOnework() {

        if (!UsermsConf.ONEWORK_CONF.isEnable()) {
            return;
        }

        JMicroWorkerOptions options = new JMicroWorkerOptions("Resync user to onework")
                .wait(2, TimeUnit.SECONDS)
                .repeat(5, TimeUnit.SECONDS).setTask(new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Run sync user to onework");
                BasicDBObject query = new BasicDBObject("sync_to_onework",
                        new BasicDBObject("$in",
                                new String[]{
                                    OSyncStatus.RE_SYNC, 
                                    OSyncStatus.FAILED, 
                                    OSyncStatus.PENDING}
                        ));
                long offset = 0;
                long limit = 100;
                APIResponse<User> apiResult = UserDB.getInstance()
                        .queryMongoDB(Document.parse(query.toJson()), offset, limit, false);
                while (apiResult != null && apiResult.status == APIStatus.OK && apiResult.data != null) {
                    for (User jobUse : apiResult.data) {
                        UserCommon.syncAccountToOnework(jobUse);
                    }
                    offset += apiResult.data.size();
                    apiResult = UserDB.getInstance()
                            .queryMongoDB(Document.parse(query.toJson()), offset, limit, false);
                }
            }
        });

        JMicroWorker.getInstance().launch(options);
    }
}
