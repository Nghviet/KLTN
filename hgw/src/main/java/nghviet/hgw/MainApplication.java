package nghviet.hgw;

import com.google.gson.Gson;
import nghviet.hgw.anomaly.Anomaly;
import nghviet.hgw.controller.EchonetLiteController;
import nghviet.hgw.mqtt.MqttHandler;
import nghviet.hgw.security.SecurityHandler;
import nghviet.hgw.threading.AnomalySignal;
import nghviet.hgw.threading.LoginSignal;
import nghviet.hgw.utility.JWT;
import nghviet.hgw.utility.LoggerHandler;
import nghviet.hgw.utility.MACAddress;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
		ScheduledThreadPoolExecutor threadPool = new ScheduledThreadPoolExecutor(4);
		SecurityHandler.getInstance();
		MqttHandler.getInstance();
		MACAddress.getInstance().update();
		LoggerHandler.getInstance();
		MqttHandler.getInstance().enqueue("request/mining","");
		Anomaly.getInstance().init();
		Gson gson = new Gson();
		threadPool.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long startTime = System.nanoTime();
				ArrayList<String> result = Anomaly.getInstance().run();
				long endTime   = System.nanoTime();
				LoggerHandler.getInstance().info("Anomaly detection run in : " + (endTime - startTime) + " ns");
				if(result != null && !result.isEmpty()) {
					LoggerHandler.getInstance().warn("Result ---------------------");
					for(String r:result) LoggerHandler.getInstance().warn(r);
					try {
						MqttHandler.getInstance().enqueue("warning", gson.toJson(result));
					} catch (Exception e) {
						LoggerHandler.getInstance().warn(e.toString());
					}
					LoggerHandler.getInstance().warn("---------------------");
				}

			}
		}, 2,2, TimeUnit.MINUTES);

		EchonetLiteController controller = new EchonetLiteController(threadPool);
	}

}
