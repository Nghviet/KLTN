package nghviet.hgw.controller.process;

import com.sonycsl.echo.EchoFrame;
import com.sonycsl.echo.EchoProperty;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.sensor.HumiditySensor;
import com.sonycsl.echo.eoj.device.sensor.TemperatureSensor;
import nghviet.hgw.utility.LoggerHandler;
import nghviet.hgw.utility.Processing;

import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TemperatureProcess extends AbstractProcess<TemperatureSensor> {
    public TemperatureProcess(ScheduledThreadPoolExecutor threadPool) {
        super(threadPool);
        topic = "temperature";

        threadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (pending.size() != 0) {
                    LoggerHandler.getInstance().info(pending.size() + "/" + devices.size() + " temperature loss");
                    pending.clear();
                }
//                else LoggerHandler.getInstance().info(Integer.toString(counter) + " : received " + devices.size());
                send();
                changed.clear();
                for (TemperatureSensor sensor : devices) {
                    try {
                        EchoFrame frame = sensor.get().reqGetOperationStatus().reqGetMeasuredTemperatureValue().send();
                        pending.add(new Package(sensor, frame));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                threadPool.schedule(reliableChecking, 1250, TimeUnit.MILLISECONDS);
                threadPool.schedule(reliableChecking, 1750, TimeUnit.MILLISECONDS);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);

    }

    @Override
    void setReceiver(TemperatureSensor device) {
        device.setReceiver(new TemperatureSensor.Receiver() {
            @Override
            protected void onGetOperationStatus(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetOperationStatus(eoj, tid, esv, property, success);
                received(eoj);
                if(!success) return;

                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "OperationStatus";

                String currentValue;
                if(property.edt[0] == (byte) 0x30) currentValue = "ON"; else currentValue = "OFF";

                String previousValue = storage.get(ipAddress, instanceCode, epc);
                if(!previousValue.equals(currentValue)) {
                    changedValue(ipAddress, instanceCode, epc);
                    storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);
                }
            }

            @Override
            protected void onGetMeasuredTemperatureValue(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetMeasuredTemperatureValue(eoj,tid,esv,property,success);
                if(!success) return;
                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "Temperature";

                String previousValue = storage.get(ipAddress, instanceCode, epc);
                if(previousValue == null || previousValue.equals("")) previousValue = "0";
                String currentValue = Processing.convertShort(property.edt);
//                currentValue = Short.toString((short) (Short.parseShort(currentValue) / 10));

                    changedValue(ipAddress, instanceCode, epc);
                    storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);

            }
        });
    }
}
