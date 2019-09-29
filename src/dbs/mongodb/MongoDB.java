/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dbs.mongodb;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import com.mongodb.client.MongoDatabase;
import com.mongodb.inst.DBInitHandler;
import com.mongodb.inst.MongoInstance;
import com.mongodb.utils.RESTClient;
import common.api.APIResponse;
import java.net.InetSocketAddress;
import java.net.Socket;
import models.user.UserDB;
import org.apache.log4j.Logger;
import common.utils.config.SConfig;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import models.user.level.UserLevelDB;

/**
 *
 * @author ritte
 */
public class MongoDB {
    
    private static final Logger LOGGER = Logger.getLogger(MongoDB.class);
    
    public static void init(String host, int port, String dbName, String user, String pass, String dbAuth) {
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress(host, port), 500);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        
        System.out.println("   Host=" + host + " Port=" + port + " DBName=" + dbName);
        MongoInstance inst = new MongoInstance(host, port, dbName, new DBInitHandler() {
            @Override
            public void onInitSuccess(MongoDatabase database) {
                System.out.println("[MongoDB Connection] Init " + dbName + " DB OK!");
                //Init collection
                UserDB.getInstance().initDB(database);
                UserLevelDB.getInstance().initDB(database);
            }
            
            @Override
            public void onInitError(APIResponse resp) {
                System.out.println("Fail to connect to DB: " + resp.status + " " + resp.message);
            }
        }, user, pass, dbAuth);
        
        inst.connectSynchonously();
        
        if (SConfig.getInt("mongodb.dblog-enable") == 1) {
            String dbLog = dbName + "_log";
            
            MongoInstance mgLog = new MongoInstance(host, port, dbLog,
                    new DBInitHandler() {
                
                @Override
                public void onInitSuccess(MongoDatabase database) {
                    System.out.println("[MongoDB Connection] Init " + dbLog + " DB OK!");
                    RESTClient.initLogDatabase(database);                    
                }
                
                @Override
                public void onInitError(APIResponse apir) {
                    System.out.println(apir);
                }
            }, user, pass, dbAuth);
            
            mgLog.connectSynchonously();
        }
    }
    
    public static long getTimestampDate(String date, String format) throws ParseException {
        Date dateFormat = new SimpleDateFormat(format).parse(date);        
        return dateFormat.getTime();
    }
    
    public static DBObject getDBObject_GTE_LT_ByDate_From_To(String field, String fromDate, String toDate) {
        try {            
            
            fromDate += " 00:00:00";
            toDate += " 23:59:00";            
            
            String dateFormat = "dd-MM-yyyy HH:mm:ss";
            Date _fromDate, _toDate;
            
            _fromDate = new Date(getTimestampDate(fromDate, dateFormat));
            _toDate = new Date(getTimestampDate(toDate, dateFormat));
            return QueryBuilder.start(field)
                    .greaterThanEquals(_fromDate)
                    .lessThanEquals(_toDate).get();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return null;
        }
    }
}
