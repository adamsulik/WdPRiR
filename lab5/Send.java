package Zad5;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Send implements Runnable {
    private static final String EXCHANGE_NAME = "messenger";
    private Connection connection;
    private Channel channel;
    private String senderName;

    Send(String senderNameOut) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
        this.senderName = senderNameOut;
    }

    public void run() {
        Scanner input = new Scanner(System.in);
        while(true) {
            String message = input.nextLine();
            String messageOut = senderName + ": " + message;
            try {
                channel.basicPublish(EXCHANGE_NAME, "", null, messageOut.getBytes("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
