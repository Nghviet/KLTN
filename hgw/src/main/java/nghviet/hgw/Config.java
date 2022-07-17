package nghviet.hgw;

public class Config {
    private static Config instance = new Config();

    public static Config getInstance() {
        return instance;
    }

    public void loadConfig() {

    }

    public String mqtt_broker;
    public String register = "http://112.137.129.202:8080/API/register";
    public String ca_url;
    public String signer_url;


}
