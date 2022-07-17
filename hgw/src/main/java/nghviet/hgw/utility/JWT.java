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
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                jwt = reader.readLine();
                HttpHandler.getInstance().setJwtToken(jwt);
            } catch (Exception ex) {
                ex.printStackTrace();
                JWT.register("ann101","ann101");
                throw new IOException("File not found");
            }
        } else {
            JWT.register("ann101","ann101");
        };
    }

    public static int register(String username, String password) throws IOException {
        try {
            File file = new File(jwt_file);
            byte[] macAddress = new byte[0];
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaces.hasMoreElements()) {
                NetworkInterface inf = networkInterfaces.nextElement();
                byte[] mac = inf.getHardwareAddress();
                if(mac != null && !inf.getDisplayName().contains("Virtual")) {
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
            System.out.println(jwt);

            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(jwt);
            writer.flush();

            HttpHandler.getInstance().setJwtToken(jwt);

            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }
}
