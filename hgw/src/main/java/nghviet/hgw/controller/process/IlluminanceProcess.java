package nghviet.hgw.controller.process;

import com.sonycsl.echo.EchoFrame;
import com.sonycsl.echo.EchoProperty;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.sensor.IlluminanceSensor;
import nghviet.hgw.utility.LoggerHandler;
import nghviet.hgw.utility.Processing;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class IlluminanceProcess extends AbstractProcess<IlluminanceSensor> {

    public IlluminanceProcess(ScheduledThreadPoolExecutor threadPool) {
        super(threadPool);
        topic = "illuminance";

        threadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(pending.size() != 0) {
                    LoggerHandler.getInstance().info(pending.size() + "/" + devices.size() + " illuminance loss");
                    pending.clear();
                }

                send();
                changed.clear();
                for(IlluminanceSensor sensor : devices) {
                    try {
                        EchoFrame frame = sensor.get().reqGetOperationStatus().reqGetMeasuredIlluminanceValue1().send();
                        pending.add(new Package(sensor, frame));
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }

//                    threadPool.scheduleAtFixedRate(reliableChecking, 750,500,TimeUnit.MILLISECONDS);
                }
            }
        }, 0,2000, TimeUnit.MILLISECONDS);
    }

    @Override
    void setReceiver(IlluminanceSensor device) {
        device.setReceiver(new IlluminanceSensor.Receiver() {
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
//                    System.out.println(previousValue  + " " + currentValue);
                    storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);
                }
            }

            @Override
            protected void onGetMeasuredIlluminanceValue1(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetMeasuredIlluminanceValue1(eoj, tid, esv, property, success);
                if(!success) return;
//                System.out.println(Processing.convertShort(property.edt));

                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "MeasuredIlluminance";

                String previousValue = storage.get(ipAddress, instanceCode, epc);
                if(previousValue == null || previousValue.equals("")) previousValue = "0";
                String currentValue = Processing.convertShort(property.edt);
                    changedValue(ipAddress, instanceCode, epc);
//                    System.out.println(previousValue + " " + currentValue);
                    storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);


            }
        });
    }
}
