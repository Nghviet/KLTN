package nghviet.hgw.controller.process;

import com.google.gson.Gson;
import com.sonycsl.echo.EchoFrame;
import com.sonycsl.echo.eoj.EchoObject;
import com.sonycsl.echo.eoj.device.DeviceObject;
import nghviet.hgw.mqtt.MqttHandler;
import nghviet.hgw.utility.MACAddress;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public abstract class AbstractProcess<T extends EchoObject> {

    // Tracking system
    protected List<T> devices = new ArrayList<>();

    // Reliable system
    protected static class Package {
        EchoObject device;
        EchoFrame frame;

        Package(EchoObject device, EchoFrame frame) {
            this.device = device;
            this.frame = frame;
        }
    }

    protected List<Package> pending = Collections.synchronizedList(new ArrayList<Package>());

    protected ScheduledThreadPoolExecutor threadPool = null;

    protected Runnable reliableChecking = null;

    // Core system
    protected EchoObject.Receiver receiver = null;

    //Data storage
    protected String topic = null;

    protected static class CentralStorage {
        ConcurrentHashMap<String , ConcurrentHashMap<String,String>> values = new ConcurrentHashMap<>();

        public void clear() {
            values.clear();
        }

        public synchronized void put(String ipAddress, String instanceCode, String epc, String value) {
            if(MACAddress.getInstance().getMACAddress(ipAddress) == null) {
                synchronized (MACAddress.getInstance()) {
                    MACAddress.getInstance().update();
                }
            }
            String key = MACAddress.getInstance().getMACAddress(ipAddress) + "/" + instanceCode;
//            System.out.println("PUT " + key + " " + instanceCode + " " + epc + " " + value);
            if(!values.containsKey(key)) values.put(key, new ConcurrentHashMap<String, String>());
            values.get(key).put(epc, value);
        }

        public synchronized String get(String ipAddress, String instanceCode, String epc) {
            try {
                if(MACAddress.getInstance().getMACAddress(ipAddress) == null) {
                    synchronized (MACAddress.getInstance()) {
                        MACAddress.getInstance().update();
                    }
                }
                String key = MACAddress.getInstance().getMACAddress(ipAddress) + "/" + instanceCode;
//                System.out.println("GET " + key + " " + instanceCode + " " + epc + " " + values.get(key).get(epc));
                return values.get(key).get(epc);
            } catch(Exception ex) {
                return "";
            }
        }
    }

    protected class DeviceKeyEpc {
        private String ipAddress, instanceCode, epc;

        private String key;

        public DeviceKeyEpc(String ipAddress, String instanceCode, String epc) {
            this.ipAddress = ipAddress;
            this.instanceCode = instanceCode;
            this.epc = epc;
            key = MACAddress.getInstance().getMACAddress(ipAddress) + "/" + instanceCode;
        }
    }

    protected CentralStorage storage = new CentralStorage();

    protected ArrayList<DeviceKeyEpc> changed = new ArrayList<>();

    private Gson gson = new Gson();

    void changedValue(String ipAddress, String instanceCode, String epc) {
        changed.add(new DeviceKeyEpc(ipAddress,instanceCode,epc));
    }

    private String generateMessage() {
        HashMap<String, HashMap<String, String>> values = new HashMap<>();

        for(DeviceKeyEpc keyEpc: changed) {
            if(!values.containsKey(keyEpc.key)) values.put(keyEpc.key, new HashMap<String ,String>());
            String value = storage.get(keyEpc.ipAddress, keyEpc.instanceCode,keyEpc.epc);
            values.get(keyEpc.key).put(keyEpc.epc, value);
        }

        return gson.toJson(values);
    }

    private Date date = new Date();

    //Impl

    protected AbstractProcess(ScheduledThreadPoolExecutor threadPool) {
        this.threadPool = threadPool;
        reliableChecking = new Runnable() {
            int counter = 0;
            @Override
            public void run() {
                try {
                    for(int i=0;i<pending.size();i++) {
                        Package p = pending.get(i);
                        p.device.get().send(p.frame);
                        //    System.out.println(p.device.getNode().getAddressStr() + " resent " + p.frame.getTID());

                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public void addDevice(T device) {
        devices.add(device);
        setReceiver(device);
    }

    abstract void setReceiver(T device);

    public synchronized void received(EchoObject device) {
        for(int i=0;i<pending.size();i++) if(pending.get(i).device.equals(device)) {
            pending.remove(i);
            break;
        }
    }

    protected boolean send() {

        if(topic == null) return false;
        if(changed.size() == 0) return true;
        StringBuilder message = new StringBuilder();
        Date date = new Date();
        message.append("{\n");

        message.append("\t" + '"' + "value" +'"' + " : ").append(generateMessage()).append(",\n");
//        System.out.println(generateMessage());
        message.append("\t").append('"').append("timestamp").append('"').append(" : ").append(date.getTime()).append("\n");
        message.append("}");

        System.out.println(message);

        return false;


//
//        try {
//            MqttHandler.getInstance().enqueue("data/" + topic, message.toString());
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
    }
}
