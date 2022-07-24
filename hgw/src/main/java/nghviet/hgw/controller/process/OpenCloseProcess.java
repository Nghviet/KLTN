package nghviet.hgw.controller.process;

import com.sonycsl.echo.EchoFrame;
import com.sonycsl.echo.EchoProperty;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.sensor.MicromotionSensor;
import com.sonycsl.echo.eoj.device.sensor.OpenCloseSensor;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OpenCloseProcess extends AbstractProcess<OpenCloseSensor> {
    public OpenCloseProcess(ScheduledThreadPoolExecutor threadPool) {
        super(threadPool);
        topic = "door";
        threadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(pending.size() != 0) {
                    pending.clear();
                }

                send();
                changed.clear();
                for(OpenCloseSensor sensor : devices) {
                    try {
                        EchoFrame frame = sensor.get().reqGetDegreeOfOpeniNgDetectionStatus1().send();
                        pending.add(new Package(sensor, frame));
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, 0,500, TimeUnit.MILLISECONDS);
    }

    @Override
    void setReceiver(OpenCloseSensor device) {
        device.setReceiver(new OpenCloseSensor.Receiver() {
            @Override
            protected void onGetOperationStatus(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetOperationStatus(eoj, tid, esv, property, success);
                received(eoj);
                if(!success) return;

                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "OperationStatus";

                String currentValue = "";
                if(property.edt[0] == (byte) 0x30) currentValue = "ON";
                if(property.edt[0] == (byte) 0x31) currentValue = "OFF";

                String previousValue = storage.get(ipAddress, instanceCode, epc);
//                if(!previousValue.equals(currentValue)) changedValue(ipAddress, instanceCode, epc);

                storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),"OperationStatus",currentValue);
            }

            @Override
            protected void onGetDegreeOfOpeniNgDetectionStatus1(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetDegreeOfOpeniNgDetectionStatus1(eoj, tid, esv, property, success);
                if(!success) return;
                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "Status";

                String currentValue = "";
                if(property.edt[0] == (byte) 0x30) currentValue = "OFF";
                if(property.edt[0] == (byte) 0x31) currentValue = "ON";

                if(!currentValue.equals("")) System.out.println("DOOR " + currentValue);

                String previousValue = storage.get(ipAddress, instanceCode, epc);
                if(previousValue == null || previousValue.equals("")) previousValue = "";
                if(!previousValue.equals(currentValue)) changedValue(ipAddress, instanceCode,epc);

                storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);
            }
        });
    }
}
