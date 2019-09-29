/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clients.boms;

import s.models.user.UToken;
import com.google.gson.Gson;
import common.CommonUtils;
import common.api.APIClient;
import common.api.APIFactory;
import common.api.APIOutRequest;
import common.api.APIProtocol;
import common.api.APIResponse;
import common.api.APIStatus;
import common.utils.GsonUtils;
import java.util.Map;
import org.apache.log4j.Logger;
import common.utils.config.SConfig;

/**
 *
 * @author ritte
 */
public class BOClient {

    private static final Logger LOGGER = Logger.getLogger(BOClient.class);
    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;

    private static final APIClient CLIENT = APIFactory.createAPIClient(APIProtocol.THRIFT,
            SConfig.getString("outbound-api.userms"), Object.class);

    private Map<String, String> setHeader(Map<String, String> header) {
        header.put("X-CLIENT-KEY", SConfig.getString("outbound-api.userms-client-key"));
        header.put("X-CLIENT-SECRET", SConfig.getString("outbound-api.userms-client-secret"));
        return header;
    }

    public APIResponse<UToken> verifyToken(String token) {
        try {

            APIOutRequest req = new APIOutRequest();
            setHeader(req.headers);
            req.method = "GET";
            req.resource = "userms/v1/session/" + token;
            APIResponse<UToken> resp = CLIENT.call(req);
            CommonUtils.parseAPIResponse(resp, UToken.class);
            return resp;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return new APIResponse<>(APIStatus.ERROR, "Internal error: " + e.getMessage());
        }
    }

    public static BOClient getInstance() {
        return BOClientHolder.INSTANCE;
    }

    private static class BOClientHolder {

        private static final BOClient INSTANCE = new BOClient();
    }
}
