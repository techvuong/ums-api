/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api.user.pubic;

import api.user.APIOneUser;
import caches.HzUser;
import s.models.user.User;
import com.google.gson.Gson;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import org.apache.log4j.Logger;

/**
 *
 * @author ritte
 */
public class APIBank extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APIOneUser.class);
    private static final Gson GSON = new Gson();

    private APIBank() {
        super("bank/{id}");
    }

    @APIMethod(name = "GET")
    public void onGet(APIRequest req, APIResponder resp) {
        try {
            String bankAccountNo = req.getVar("id");
            if (bankAccountNo == null || bankAccountNo.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid {bank_account_no}."));
                return;
            }

            APIResponse<User> result = HzUser.getInstance().getByBankAccountNo(bankAccountNo);
            resp.respond(result);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, ex.getMessage()));
        }
    }

    public static APIBank getInstance() {
        return APIBankHolder.INSTANCE;
    }

    private static class APIBankHolder {

        private static final APIBank INSTANCE = new APIBank();
    }
}
