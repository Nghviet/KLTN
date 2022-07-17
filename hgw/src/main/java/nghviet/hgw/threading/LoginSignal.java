package nghviet.hgw.threading;

public class LoginSignal {
    private static final LoginSignal instance = new LoginSignal();

    public static LoginSignal getInstance() { return instance; }

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