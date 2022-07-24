package nghviet.hgw.utility;

import nghviet.hgw.Config;
import nghviet.hgw.http.HttpHandler;

import java.io.*;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

public class JWT {
    private static final String jwt_file = ".jwt";
    private static String jwt = "";
    public static void jwtInit() throws IOException {
        File file = new File(jwt_file);
        if (file.canRead()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                jwt = reader.readLine();
                HttpHandler.getInstance().setJwtToken(jwt);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw ex;
            }
        } 
        else throw new IOException("File not found");
    }

    public static int register(String username, String password, boolean export) throws IOException {
        try {
            File file = new File(jwt_file);
            byte[] macAddress = new byte[0];
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements()) {
                NetworkInterface inf = networkInterfaces.nextElement();
                byte[] mac = inf.getHardwareAddress();
                if(mac != null && !inf.getDisplayName().contains("Virtual") && (inf.getDisplayName().contains("wlan") || inf.getDisplayName().contains("Ethernet"))) {
                    System.out.println(inf);
                    macAddress = mac;
                    break;
                }
            }

            if(macAddress.length == 0) return 1;

            String[] hexadecimalFormat = new String[macAddress.length];
            for (int i = 0; i < macAddress.length; i++)
                hexadecimalFormat[i] = String.format("%02X", macAddress[i]);
            String MAC = String.join("-", hexadecimalFormat);
            System.out.println(MAC);
            HashMap<String, String> body = new HashMap<>();
            String put = body.put("username", username);
            body.put("password", password);
            body.put("mac", MAC);

            ArrayList<String> response = HttpHandler.getInstance().request("POST", Config.getInstance().register, body, false);
            if (response == null) {
                System.out.println("Cannot register");
                return 1;
            }
            jwt = response.get(0);

            if(export) {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write(jwt);
                writer.flush();

                HttpHandler.getInstance().setJwtToken(jwt);
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static boolean isAvailable() {
        if(jwt.length() == 0) return false;
        return true;
    }
}
