package nghviet.hgw.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MACAddress {
    private static MACAddress instance = null;

    private static Runtime runtime = Runtime.getRuntime();

    private ConcurrentMap<String, String> MACToIP = new ConcurrentHashMap<>();
    private ConcurrentMap<String, String> IPToMAC = new ConcurrentHashMap<>();

    public static synchronized MACAddress getInstance() {
        if(instance == null) instance = new MACAddress();
        return instance;
    }

    public void setIP(String IPAddress, String macAddress) {
        MACToIP.put(macAddress, IPAddress);
        IPToMAC.put(IPAddress, macAddress);
    }

    public String getIP(String macAddress) {
        if(!MACToIP.containsKey(macAddress)) update();
        return MACToIP.get(macAddress);
    }

    public String getMACAddress(String ipAddress) {
        if(!IPToMAC.containsKey(ipAddress)) update();
        return IPToMAC.get(ipAddress);
    }

    private Pattern ip_verification = Pattern.compile("^[^(]*[(]([0-9]+.[0-9]+.[0-9]+.[0-9]+)[)] at ([0-9a-z]+:[0-9a-z]+:[0-9a-z]+:[0-9a-z]+:[0-9a-z]+:[0-9a-z]+).*$");

    public void update() {
        try {
            Process process = runtime.exec("arp -a");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = ip_verification.matcher(line);
                if(matcher.matches()) {
                    setIP(matcher.toMatchResult().group(1), matcher.toMatchResult().group(2).replace(":","-"));
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}