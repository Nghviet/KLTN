package nghviet.hgw.controller.process;

import com.sonycsl.echo.EchoFrame;
import com.sonycsl.echo.EchoProperty;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.sensor.HumiditySensor;
import nghviet.hgw.utility.LoggerHandler;
import nghviet.hgw.utility.Processing;

import javax.swing.text.Utilities;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HumidityProcess extends AbstractProcess<HumiditySensor>{
    int counter = 0;

    public HumidityProcess(final ScheduledThreadPoolExecutor threadPool) {
        super(threadPool);
        topic = "humidity";

        threadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (pending.size() != 0) {
                    LoggerHandler.getInstance().info(Integer.toString(counter) + " : " + pending.size() + "/" + devices.size() + " humidity loss");
                    pending.clear();
                }
//                else LoggerHandler.getInstance().info(Integer.toString(counter) + " : received " + devices.size());
                send();
                changed.clear();
                counter++;
                for (HumiditySensor sensor : devices) {
                    try {
                        EchoFrame frame = sensor.get().reqGetOperationStatus().reqGetMeasuredValueOfRelativeHumidity().send();
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
    void setReceiver(HumiditySensor device) {
        System.out.println("Called");
        device.setReceiver(new HumiditySensor.Receiver() {
            @Override
            protected void onGetOperationStatus(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetOperationStatus(eoj, tid, esv, property, success);
                received(eoj);
                if(!success) {
                    System.out.println("System OP failed");
                    return;
                }

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
            protected void onGetMeasuredValueOfRelativeHumidity(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetMeasuredValueOfRelativeHumidity(eoj, tid, esv, property, success);
                if(!success) {
                    System.out.println("System humidity failed");
                    return;
                }
                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "Humidity";

                String previousValue = storage.get(ipAddress, instanceCode, epc);
                if(previousValue == null || previousValue.equals("")) previousValue = "0";
                String currentValue = String.valueOf(property.edt[0]);


                changedValue(ipAddress, instanceCode, epc);
                storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);

            }
        });
    }
}
