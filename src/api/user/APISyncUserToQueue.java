package api.user;

import clients.rabbitmq.UserRabbitMQ;
import s.models.user.UToken;
import common.AuthCommon;
import common.api.APIRequest;
import common.api.APIResource;
import common.api.APIResponder;
import common.api.APIResponse;
import common.api.APIStatus;
import common.api.annotation.APIMethod;
import org.apache.log4j.Logger;

public class APISyncUserToQueue extends APIResource {

    private static final Logger LOGGER = Logger.getLogger(APISyncUserToQueue.class);

    private APISyncUserToQueue() {
        super("/putalltoqueue");
    }

    @APIMethod(name = "GET")
    public void onQuery(APIRequest req, APIResponder resp) {
        try {

            UToken uToken = (UToken) req.getAttribute("user");
            if (uToken == null || !AuthCommon.isAdmin(uToken)) {
                resp.respond(new APIResponse(APIStatus.UNAUTHORIZED, "Bạn không có quyền sử dụng tính năng này."));
                return;
            }

            String queue = req.getParams("queue_name");
            if (queue == null || queue.isEmpty()) {
                resp.respond(new APIResponse(APIStatus.INVALID, "Invalid {queue_name}."));
                return;
            }

            UserRabbitMQ.getInstance().syncAllToQueue(queue);
            resp.respond(new APIResponse(APIStatus.OK, "Successfull."));
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            resp.respond(new APIResponse(APIStatus.ERROR, ex.getMessage()));
        }
    }

    public static APISyncUserToQueue getInstance() {
        return APIPushAllUsersToQueueHolder.INSTANCE;
    }

    private static class APIPushAllUsersToQueueHolder {

        private static final APISyncUserToQueue INSTANCE = new APISyncUserToQueue();
    }

}
