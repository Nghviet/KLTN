
package com.sonycsl.echo;

import com.sonycsl.echo.protocol.EchoProtocol;
import com.sonycsl.echo.protocol.EchoTCPProtocol;
import com.sonycsl.echo.protocol.EchoUDPProtocol;
import nghviet.hgw.utility.LoggerHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.concurrent.LinkedBlockingQueue;

public final class EchoSocket {
    @SuppressWarnings("unused")
    private static final String TAG = EchoSocket.class.getSimpleName();

    public static final String SELF_ADDRESS = "127.0.0.1";
    public static final String MULTICAST_ADDRESS = "224.0.23.0";

    protected static LinkedBlockingQueue<EchoProtocol.Task> sTaskQueue = new LinkedBlockingQueue<EchoProtocol.Task>();

    private static synchronized EchoProtocol.Task retrive() throws InterruptedException {
        return sTaskQueue.take();
    }


    public static synchronized void enqueueTask(EchoProtocol.Task task) {
        sTaskQueue.offer(task);
    }

    private static EchoUDPProtocol sUDPProtocol = new EchoUDPProtocol();
    private static EchoTCPProtocol sTCPProtocol = new EchoTCPProtocol();

    private static Thread udpThread;
    private static Thread sTaskPerformerThread;
    private static short sNextTID = 0;
    private static boolean fPerformActive;

    private static Logger logger = LoggerHandler.getInstance();

    private EchoSocket() {
    }

    public static void openSocket() throws IOException {
        sUDPProtocol.openUDP();
        sTCPProtocol.openTCP();
        startReceiverThread();
    }

    public static void openSocket(NetworkInterface nwif) throws IOException {
        sUDPProtocol.openUDP(nwif);
        sTCPProtocol.openTCP();
        startReceiverThread();
    }

    private static void startReceiverThread() {
        if (udpThread == null) {
            udpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (sUDPProtocol.isOpened()) {
                        // System.out.println("UDP receive");
                        sUDPProtocol.receive();
                    }
                }
            });
            udpThread.start();
        }

        if (sTaskPerformerThread == null) {
            fPerformActive = true;
            sTaskPerformerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (fPerformActive) {
                        try {
                            sTaskQueue.take().perform();
                        } catch (InterruptedException e) {
                            // e.printStackTrace();
                        }
                    }
                }
            });
            sTaskPerformerThread.start();

//            Thread sub1 = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while (fPerformActive) {
//                        try {
//                            retrive().perform();
//                        } catch (InterruptedException e) {
//                            // e.printStackTrace();
//                        }
//                    }
//                }
//            });
//            sub1.start();
        }
    }

    public static void closeSocket() throws IOException {
        sTCPProtocol.closeTCP();
        sUDPProtocol.closeUDP();
        stopReceiverThread();
    }

    private static void stopReceiverThread() {
        if (udpThread != null) {
            udpThread.interrupt();
            try {
                udpThread.join();
            } catch (Exception e) {
            }
            udpThread = null;
        }

        if (sTaskPerformerThread != null) {
            fPerformActive = false;
            sTaskPerformerThread.interrupt();
            try {
                sTaskPerformerThread.join();
            } catch (Exception e) {
            }
            sTaskPerformerThread = null;
        }
    }

    public static void resumeReceiverThread() {

    }

    public static void pauseReceiverThread() {

    }

    public static synchronized short nextTID() {
        short ret = sNextTID;
        sNextTID += 1;
        // Echo::getStorage().get()->setNextTID(sNextTID);
        return ret;
    }

    public static short getNextTIDNoIncrement() {
        return sNextTID;
    }

    public static void sendUDPFrame(EchoFrame frame) throws IOException {
        sUDPProtocol.sendUDP(frame);
    }

    public static void sendTCPFrame(EchoFrame frame) throws IOException {
        sTCPProtocol.sendTCP(frame);
    }
}
