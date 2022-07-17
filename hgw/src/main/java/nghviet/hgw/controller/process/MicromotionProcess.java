package nghviet.hgw.controller.process;

import com.sonycsl.echo.EchoFrame;
import com.sonycsl.echo.EchoProperty;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.sensor.IlluminanceSensor;
import com.sonycsl.echo.eoj.device.sensor.MicromotionSensor;
import nghviet.hgw.utility.Processing;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MicromotionProcess extends AbstractProcess<MicromotionSensor> {
    public MicromotionProcess(ScheduledThreadPoolExecutor threadPool) {
        super(threadPool);
        topic = "micromotion";
        threadPool.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(pending.size() != 0) {
                    System.out.println(pending.size() + "/" + devices.size() + " illuminance loss");
                    pending.clear();
                }

                send();
                storage.clear();
                for(MicromotionSensor sensor : devices) {
                    try {
                        EchoFrame frame = sensor.get().reqGetOperationStatus().reqGetMicromotionDetectionStatus().send();
                        pending.add(new Package(sensor, frame));
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, 0,2000, TimeUnit.MILLISECONDS);
    }

    @Override
    void setReceiver(MicromotionSensor device) {
        device.setReceiver(new MicromotionSensor.Receiver() {
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
                if(!previousValue.equals(currentValue)) changedValue(ipAddress, instanceCode, epc);

                storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),"OperationStatus",currentValue);
            }

            @Override
            protected void onGetMicromotionDetectionStatus(EchoObject eoj, short tid, byte esv, EchoProperty property, boolean success) {
                super.onGetMicromotionDetectionStatus(eoj, tid, esv, property, success);
                if(!success) return;
                String ipAddress = eoj.getNode().getAddressStr();
                String instanceCode = Byte.toString(eoj.getInstanceCode());
                String epc = "Motion";
                String currentValue;
                if(property.edt[0] == (byte) 0x41) currentValue = "ON"; else currentValue = "OFF";

                String previousValue = storage.get(ipAddress, instanceCode, epc);
                if(previousValue == null || previousValue.equals("")) previousValue = "0";
                if(!previousValue.equals(currentValue)) changedValue(ipAddress, instanceCode,epc);
                storage.put(eoj.getNode().getAddressStr(), Byte.toString(eoj.getInstanceCode()),epc,currentValue);
            }
        });
    }
}
