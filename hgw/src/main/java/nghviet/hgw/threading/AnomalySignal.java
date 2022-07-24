package nghviet.hgw.threading;

public class AnomalySignal {
    private static final AnomalySignal instance = new AnomalySignal();

    public static AnomalySignal getInstance() { return instance; }

    private boolean signalled = false;

    public void doWait() {
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
