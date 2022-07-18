package nghviet.hgw;

import nghviet.hgw.anomaly.Anomaly;
import nghviet.hgw.controller.EchonetLiteController;
import nghviet.hgw.mqtt.MqttHandler;
import nghviet.hgw.security.SecurityHandler;
import nghviet.hgw.threading.LoginSignal;
import nghviet.hgw.utility.JWT;
import nghviet.hgw.utility.LoggerHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@SpringBootApplication
public class MainApplication {
	public static void main(String[] args) throws Exception {
		System.setProperty("java.net.preferIPv4Stack" , "true");
		SpringApplication.run(MainApplication.class, args);
		try {
			JWT.jwtInit();
		} catch (Exception ex) {
			System.out.println("JWT Failed, waiting");
			ex.printStackTrace();
			LoginSignal.getInstance().doWait();
		}
		System.out.println("System start");
		System.out.println(JWT.isAvailable());
		ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(4);
		SecurityHandler.getInstance();
		MqttHandler.getInstance();
		LoggerHandler.getInstance();
		Anomaly.getInstance();

		ArrayList<String> testData = new ArrayList<>();
		testData.add("micromotion/f4-5c-89-92-6c-8d/14_ON");
		testData.add("illuminance/f4-5c-89-92-6c-8d/15_4");
		testData.add("illuminance/f4-5c-89-92-6c-8d/14_6");
		testData.add("micromotion/f4-5c-89-92-6c-8d/9_ON");
		testData.add("micromotion/f4-5c-89-92-6c-8d/15_ON");
		testData.add("illuminance/f4-5c-89-92-6c-8d/11_2");
		testData.add("illuminance/f4-5c-89-92-6c-8d/12_3");
		testData.add("micromotion/f4-5c-89-92-6c-8d/11_ON");
		testData.add("illuminance/f4-5c-89-92-6c-8d/9_2");

		ArrayList<String> result = Anomaly.getInstance().anomalyDetection(testData);
		System.out.println("Anomaly detection result");
		for(String r : result) System.out.println(r);
//		MqttHandler.getInstance().enqueue("request/mining","");
//		EchonetLiteController controller = new EchonetLiteController(threadPool);
	}

}
