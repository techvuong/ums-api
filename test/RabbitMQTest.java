
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.inst.JMicroRabbit;
import com.rabbitmq.inst.RabbitConfig;
import com.rabbitmq.inst.RabbitInstance;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import common.utils.config.SConfig;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ritte
 */
public class RabbitMQTest {

    public static void main(String[] args) {
        try {
            String env = System.getProperty("appenv");
            if (env == null) {
                env = "dev";
            }

            SConfig.init("./conf/" + env + ".ini");

            new RabbitInstance()
                    .doConnect(new RabbitConfig(
                            SConfig.getString("rabbitmq.host"),
                            SConfig.getInt("rabbitmq.port"),
                            SConfig.getString("rabbitmq.user"),
                            SConfig.getString("rabbitmq.pass")))
                    .done();
            
            JMicroRabbit.getInstance().getChannel().getChannel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ConnectionFactory getConnectionFactory() {
        ConnectionFactory factory = new ConnectionFactory();
//        try {

        factory.setHost(SConfig.getString("rabbit.host"));
        factory.setPort(SConfig.getInt("rabbit.port"));
        factory.setUsername(SConfig.getString("rabbit.username"));
        factory.setPassword(SConfig.getString("rabbit.password"));
        factory.setVirtualHost("/");
        // Timeout for connection establishment: 5s
        factory.setConnectionTimeout(5000);
        // Configure automatic reconnections
        factory.setAutomaticRecoveryEnabled(true);
        // Recovery interval: 10s
        factory.setNetworkRecoveryInterval(10000);
        // Exchanges and so on should be redeclared if necessary
        factory.setTopologyRecoveryEnabled(true);

//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return factory;
    }

    private static final Logger LOGGER = Logger.getLogger(RabbitMQTest.class);

    public static void getMessageFromQueue() {

        try {
            ConnectionFactory factory = getConnectionFactory();
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.addShutdownListener(new ShutdownListener() {
                @Override
                public void shutdownCompleted(ShutdownSignalException cause) {
                    LOGGER.info("getMessageFromQueue():" + cause.getMessage(), cause);
                }
            });

            ((Recoverable) channel).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecoveryStarted(Recoverable recoverable) {
                }

                @Override
                public void handleRecovery(Recoverable recoverable) {
                    if (recoverable instanceof Channel) {
                        int channelNumber = ((Channel) recoverable).getChannelNumber();
                        System.out.println("Connection to channel #" + channelNumber + " was recovered.");
                    }

                }
            });

            String queueName = channel.queueDeclare().getQueue();
            System.out.println(queueName);

            channel.queueDeclare("test23", false, false, false, null);
//            channel.exchangeDeclare("exchange_user", BuiltinExchangeType.FANOUT, true);
            channel.queueBind(queueName, "exchange_user", "");

            System.out.println(" [*] Waiting for messages. ");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Received '" + message + "'");

            };
            channel.basicConsume("test23", true, deliverCallback, consumerTag -> {
            });
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Can not connect to RabbitMQ server : " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
}
