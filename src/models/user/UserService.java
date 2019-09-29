/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.user;

import common.api.APIResponse;
import s.models.user.User;

/**
 *
 * @author ritte
 */
public class UserService {
    public static APIResponse<User> update(String _id, User u){
        
        u.processed = Boolean.FALSE;
        return UserDB.getInstance().update(_id, u);
    }
    
    public static APIResponse<User> create(User u){
        
        u.processed = Boolean.FALSE;
        return UserDB.getInstance().create(u);
    }
}
