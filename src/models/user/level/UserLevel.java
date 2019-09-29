/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models.user.level;

import com.mongodb.api.APIModel;
import common.utils.GsonUtils;

/**
 *
 * @author ritte
 */
public class UserLevel extends APIModel<String> {

    public Long user_id;
    public Long date_index;
    public String level;
    public String status;
    public String create_by;

    @Override
    public String toString() {
        return GsonUtils.GSON_UTCDATE_NORMNUMBER.toJson(this);
    }
}
