package nghviet.hgw.mqtt;

import nghviet.hgw.anomaly.Anomaly;
import nghviet.hgw.security.SecurityHandler;
import nghviet.hgw.threading.AnomalySignal;
import nghviet.hgw.threading.LoginSignal;
import nghviet.hgw.utility.LoggerHandler;
import org.eclipse.paho.client.mqttv3.*;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.LinkedBlockingQueue;

public class MqttHandler {
    private static MqttHandler instance = null;
    private MqttClient client;
    private Callback callback = new Callback();
    private String id = null;

    private static class MqttPayload {
        private String topic;
        private String message;
        private int qos;
        private boolean retained;

        MqttPayload(String topic, String message, int qos, boolean retained) {
            this.topic = topic;
            this.message = message;
            this.qos = qos;
            this.retained = retained;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getQos() {
            return qos;
        }

        public void setQos(int qos) {
            this.qos = qos;
        }

        public boolean isRetained() {
            return retained;
        }

        public void setRetained(boolean retained) {
            this.retained = retained;
        }
    }

    private static class Callback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            System.out.println(topic + " : " + message);
            if(topic.equals(instance.id + "/response/mining")) {
                LoggerHandler.getInstance().info(topic + " : " + message);
                Anomaly.getInstance().init();
                AnomalySignal.getInstance().doNotify();
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    }

    private LinkedBlockingQueue<MqttPayload> queue;

    public static synchronized MqttHandler getInstance() {
        try {
            if(instance == null) instance = new MqttHandler();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return instance;
    }



    private SSLSocketFactory getSocketFactory() throws Exception {
        KeyStore caKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        caKeyStore.load(null, null);
        caKeyStore.setCertificateEntry("certificate", SecurityHandler.getInstance().getCACertificate());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(caKeyStore);

        X509Certificate signed_crt = SecurityHandler.getInstance().getSignedCert();

        String name = signed_crt.getSubjectX500Principal().getName();
        id = name.substring(name.lastIndexOf("=") + 1);
        System.out.println("MQTT NAME: " + id);

        KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeyStore.load(null, null);
        clientKeyStore.setCertificateEntry("certificate", signed_crt);

        clientKeyStore.setKeyEntry("private-key",SecurityHandler.getInstance().getPrivateKey(), "".toCharArray(), new Certificate[]{signed_crt});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, "".toCharArray());

        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        return context.getSocketFactory();
    }

    public void enqueue(String topic, String message) throws Exception {
        queue.put(new MqttPayload(topic, message, 0, false));

    }

    private MqttHandler() throws Exception {
        String broker = "ssl://112.137.129.202:8883";

        client = new MqttClient(broker, "" );

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setSocketFactory(getSocketFactory());
        opts.setCleanSession(true);

        IMqttToken token = client.connectWithResult(opts);
        if(client.isConnected()) {
            System.out.println("MQTT Connected");
        }
        else {
            client = null;
            return;
        }
        client.setCallback(callback);
        System.out.println(token.getUserContext());
        client.subscribe(id + "/response/#");
        queue = new LinkedBlockingQueue<>();
        System.out.println("Client ID : " + client.getClientId());

        Runnable sender = new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        MqttPayload payload = queue.take();
                        client.publish(id + "/" + payload.getTopic(), payload.getMessage().getBytes(StandardCharsets.UTF_8), payload.getQos(), payload.isRetained());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        };

        Thread senderThread = new Thread(sender);
        senderThread.start();
    }
}
