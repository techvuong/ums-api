/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.user.level;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.model.MongoModel;
import org.bson.Document;

/**
 *
 * @author ritte
 */
public class UserLevelDB extends MongoModel<UserLevel> {

    public String _name = "user_level";

    public UserLevelDB() {
        super("UserLevel", UserLevel.class);
    }

    @Override
    public void initDB(MongoDatabase db) {

        try {
            MongoCollection<Document> col = db.getCollection(_name);
            if (col == null) {
                db.createCollection(_name);
                col = db.getCollection(_name);
            }

            col.createIndex(new BasicDBObject("user_id", 1));
            col.createIndex(new BasicDBObject("date_index", 1));
            col.createIndex(new BasicDBObject("user_id", 1).append("status", 1));

            this.setCollection(col);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public static UserLevelDB getInstance() {
        return UserLevelDBHolder.INSTANCE;
    }

    private static class UserLevelDBHolder {

        private static final UserLevelDB INSTANCE = new UserLevelDB();
    }
}
