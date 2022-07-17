package nghviet.hgw.controller;

import com.sonycsl.echo.Echo;
import com.sonycsl.echo.EchoProperty;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.DeviceObject;
import com.sonycsl.echo.eoj.device.housingfacilities.ElectricLock;
import com.sonycsl.echo.eoj.device.housingfacilities.GeneralLighting;
import com.sonycsl.echo.eoj.device.sensor.*;
import com.sonycsl.echo.eoj.profile.NodeProfile;
import com.sonycsl.echo.processing.defaults.DefaultController;
import com.sonycsl.echo.processing.defaults.DefaultNodeProfile;
import nghviet.hgw.controller.process.*;
import nghviet.hgw.utility.LoggerHandler;
import org.w3c.dom.Node;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EchonetLiteController {
    public HumidityProcess humidityProcess = null;
    public IlluminanceProcess illuminanceProcess = null;
    public MicromotionProcess micromotionProcess = null;
    public TemperatureProcess temperatureProcess = null;

    public OpenCloseProcess doorProcess = null;
    public EchonetLiteController(final ScheduledThreadPoolExecutor threadPool) {
        try {
            humidityProcess = new HumidityProcess(threadPool);
            illuminanceProcess = new IlluminanceProcess(threadPool);
            micromotionProcess = new MicromotionProcess(threadPool);
            temperatureProcess = new TemperatureProcess(threadPool);
            doorProcess = new OpenCloseProcess(threadPool);
            LoggerHandler.getInstance().info("System start");
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements()) {
                NetworkInterface inf = networkInterfaces.nextElement();
                byte[] mac = inf.getHardwareAddress();
                if(mac != null && !inf.getDisplayName().contains("Virtual")) {
                    Echo.start(new DefaultNodeProfile(), new DeviceObject[] {new DefaultController()}, inf);
                    break;
                }
            }
            threadPool.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        NodeProfile.informG().reqInformInstanceListNotification().send();
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, 10,1000, TimeUnit.MILLISECONDS);

            Echo.addEventListener(new Echo.EventListener() {
                @Override
                public void onNewHumiditySensor(HumiditySensor sensor) {
                    super.onNewHumiditySensor(sensor);
                    LoggerHandler.getInstance().info("On new Humidity Sensor at " + sensor.getNode().getAddressStr());
//                    humidityProcess.addDevice(sensor);
                }

                @Override
                public void onNewIlluminanceSensor(IlluminanceSensor sensor) {
                    super.onNewIlluminanceSensor(sensor);
                    LoggerHandler.getInstance().info("On new Illuminance Sensor at " + sensor.getNode().getAddressStr());
                    illuminanceProcess.addDevice(sensor);
                }

                @Override
                public void onNewMicromotionSensor(MicromotionSensor device) {
                    super.onNewMicromotionSensor(device);
//                    micromotionProcess.addDevice(device);
                }

                @Override
                public void onNewTemperatureSensor(TemperatureSensor sensor) {
                    super.onNewTemperatureSensor(sensor);
                    LoggerHandler.getInstance().info("On new Temperature Sensor at " + sensor.getNode().getAddressStr());
//                    temperatureProcess.addDevice(sensor);
                }

                @Override
                public void onNewOpenCloseSensor(OpenCloseSensor sensor) {
                    super.onNewOpenCloseSensor(sensor);
                    LoggerHandler.getInstance().info("On new Door Sensor at " + sensor.getNode().getAddressStr());
//                    doorProcess.addDevice(sensor);
                }
            });



        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}