/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clients.rabbitmq;

import app.worker.JMicroWorker;
import s.models.session.Session;
import s.models.user.User;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.rabbitmq.ChannelPool;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.inst.JMicroRabbit;
import common.api.APIResponse;
import common.api.APIStatus;

import common.utils.GsonUtils;
import java.util.concurrent.TimeoutException;
import models.user.SessionMessage;
import models.user.UserDB;
import models.user.UserMessage;
import common.utils.config.SConfig;
import constants.UsermsConf;

/**
 * Invoice RabbitMQ
 *
 * @author crhien
 * @since 21/01/2019
 *
 */
public class UserRabbitMQ {

    private static final Gson GSON = GsonUtils.GSON_UTCDATE_NORMNUMBER;
    private static final Logger LOGGER = Logger.getLogger(UserRabbitMQ.class);

    /**
     * Add message to Queue of RabbitMQ
     *
     * @param user
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public void sendMessageToQueueAfterChangeOddGroup(User user) throws IOException, TimeoutException, Exception {

        // RabbitMQ server is disabled
        if (!isServerEnable()) {
            return;
        }

        if (!UsermsConf.ONEWORK_CONF.isEnable()) {
            return;
        }

        Runnable rn = new Runnable() {
            @Override
            public void run() {
                try {

                    UserMessage userMessage = new UserMessage();
                    userMessage.action = "change_odd_group";
                    userMessage.setUser(user);
                    ChannelPool channelPool = JMicroRabbit.getInstance().getChannel();
                    Channel channel = channelPool.getChannel();
                    channel.basicPublish("", "user.odd_group", null, userMessage.toString().getBytes());
                    channelPool.returnChannel(channel);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        };

        JMicroWorker.getInstance().put(rn);
    }

    /**
     * Add message to Queue of RabbitMQ
     *
     * @param action
     * @param session
     * @param username
     * @param stoken
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public void sendMessageToQueue(String action, Session session, String username, String stoken) throws IOException, TimeoutException {

        try {
            // RabbitMQ server is disabled
            if (!isServerEnable()) {
                return;
            }

            Runnable rn = new Runnable() {
                @Override
                public void run() {
                    try {
                        SessionMessage sessionMessage = new SessionMessage(action, session, username, stoken);
                        final String EXCHANGE_NAME = "userms.authentication";
                        ChannelPool channelPool = JMicroRabbit.getInstance().getChannel();
                        Channel channel = channelPool.getChannel();
                        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true);
                        channel.basicPublish(EXCHANGE_NAME, "", null, sessionMessage.toString().getBytes());
                        channelPool.returnChannel(channel);
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            };

            JMicroWorker.getInstance().put(rn);

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Add message to Queue of RabbitMQ
     *
     * @param action
     * @param stoken
     * @param sessionReq
     * @param user
     * @throws java.io.IOException
     * @throws java.util.concurrent.TimeoutException
     */
    public void sendMessageToQueue(String action, User user, Session sessionReq, String stoken) throws IOException, TimeoutException, Exception {

        // RabbitMQ server is disabled
        if (!isServerEnable()) {
            return;
        }

        Runnable rn = new Runnable() {
            @Override
            public void run() {
                try {
                    UserMessage userMessage = new UserMessage();
                    userMessage.action = action;
                    userMessage.setDataObj(user, sessionReq, stoken);

                    final String EXCHANGE_NAME = "userms.user";
                    ChannelPool channelPool = JMicroRabbit.getInstance().getChannel();
                    Channel channel = channelPool.getChannel();
                    channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT, true);
                    channel.basicPublish(EXCHANGE_NAME, "", null, userMessage.toString().getBytes());
                    channelPool.returnChannel(channel);
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        };

        JMicroWorker.getInstance().put(rn);
    }

    public void syncAllToQueue(String queue) {
        try {

            if (!isServerEnable()) {
                return;
            }

            Runnable rn = new Runnable() {
                @Override
                public void run() {
                    try {
                        ChannelPool channelPool = JMicroRabbit.getInstance().getChannel();
                        long offset = 0;
                        long limit = 500;
                        User query = new User();
                        APIResponse<User> apiResult = UserDB.getInstance().query(query, offset, limit, false);
                        long total = 0;
                        while (apiResult != null && apiResult.status == APIStatus.OK && apiResult.data != null) {

                            for (User user : apiResult.data) {
                                UserMessage userMessage = new UserMessage();
                                userMessage.action = "NEW";
                                userMessage.setDataObj(user, null, null);

                                Channel channel = channelPool.getChannel();
                                channel.queueDeclare(queue, false, false, false, null);
                                channel.basicPublish("", queue, null, userMessage.toString().getBytes());
                                channelPool.returnChannel(channel);
                            }

                            offset += apiResult.data.size();
                            apiResult = UserDB.getInstance().query(query, offset, limit, false);
                            total = apiResult.total != null ? apiResult.total : 0;
                            LOGGER.info("QUERY USER " + offset + " " + limit + " " + total);
                        }
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                }
            };

            JMicroWorker.getInstance().put(rn);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Check RabbitMQ server is enable
     *
     * @return boolean true if enable , false if otherwise
     */
    private boolean isServerEnable() {
        return ("1".equals(SConfig.getString("rabbitmq.enable")));
    }

    public static UserRabbitMQ getInstance() {
        return RabbitMQUtilsHolder.INSTANCE;
    }

    private static class RabbitMQUtilsHolder {

        private static final UserRabbitMQ INSTANCE = new UserRabbitMQ();
    }
}
