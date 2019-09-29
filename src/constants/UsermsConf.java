/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package constants;

import common.utils.config.SConfig;

/**
 *
 * @author ritte
 */
public class UsermsConf {
    
    public static final int MAX_QUERY = SConfig.getInt("setting.max-query");

    public static class ONEWORK_CONF {
        
        public static boolean isEnable(){
            return "1".equals(SConfig.getString("setting.oneworld-enable"));
        }

    }
}
