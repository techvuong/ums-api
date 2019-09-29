/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.user;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.model.MongoCounter;
import com.mongodb.model.MongoModel;
import org.bson.Document;
import s.models.user.User;

/**
 *
 * @author ritte
 */
public class UserDB extends MongoModel<User>{
    
    public String _name = "user";
    
    public UserDB() {
        super("User", User.class);
    }

    @Override
    public void initDB(MongoDatabase db) {
        
        try {
            MongoCollection<Document> col = db.getCollection(_name);
            if(col == null){
               db.createCollection(_name); 
               col = db.getCollection(_name);
            }
            
            col.createIndex(new BasicDBObject("username", 1));    
            col.createIndex(new BasicDBObject("email", 1));    
            col.createIndex(new BasicDBObject("phone", 1));
            col.createIndex(new BasicDBObject("id", 1));
            col.createIndex(new BasicDBObject("boping_id", 1));
            
            this.setCollection(col).setCounter(new MongoCounter(db. getCollection("counter")));
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }
    
    public static UserDB getInstance() {
        return UserDBHolder.INSTANCE;
    }
    
    private static class UserDBHolder {
        private static final UserDB INSTANCE = new UserDB();
    }
}
