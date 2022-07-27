package nghviet.hgw.threading;

import com.sonycsl.echo.Echo;
import nghviet.hgw.utility.LoggerHandler;

public class AnomalySignal {
    private static final AnomalySignal instance = new AnomalySignal();

    public static AnomalySignal getInstance() { return instance; }

    private boolean signalled = false;

    public void doWait() {
        LoggerHandler.getInstance().info("Waiting anomaly reply");
        synchronized (instance) {
            while(!signalled) {
                try {
                    instance.wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void doNotify() {
        synchronized (instance) {
            signalled = true;
            instance.notify();
        }
    }
}
