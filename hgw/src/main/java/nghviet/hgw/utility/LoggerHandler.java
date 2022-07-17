package nghviet.hgw.utility;

import nghviet.hgw.MainApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerHandler {
    private static Logger logger = LoggerFactory.getLogger(MainApplication.class);

    public static Logger getInstance() {
        return logger;
    }
}
