/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.controller;

import ags.communication.TCPTransferHost;
import ags.communication.TransferHost;
import ags.controller.Configurable.CATEGORY;
import ags.game.Game;
import ags.game.GameUtil;
import ags.script.Script;
import ags.ui.HiresBufferedScreen;
import ags.ui.HiresScreen;
import ags.ui.IVirtualScreen;
import ags.ui.TextScreen40;
import ags.ui.gameSelector.GameSelectorApplication;
import ags.ui.host.Main;
import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

/**
 *
 * @author brobert
 */
public class Launcher implements Runnable {


    static private class OutputStreamImpl extends OutputStream {
        private PrintStream out;
        public OutputStreamImpl(PrintStream out) {
            this.out = out;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            out.write(b);
            JTextArea disp = Main.instance.logDisplay;
            disp.append("" + (char) b);
            disp.setCaretPosition(disp.getText().length());
        }
    }
    
    private static String START_PROGRAM = "Start Program";
    private static String STOP_PROGRAM = "Stop Program";
    private static PrintStream outLogRedirect = null;
    private static PrintStream errLogRedirect = null;

    private static void attachLogViewer(JTextArea logDisplay) {
        logDisplay.setVisible(true);
        logDisplay.setText("");
        if (outLogRedirect == null) {
            outLogRedirect = new PrintStream(new OutputStreamImpl(System.out), true);
            errLogRedirect = new PrintStream(new OutputStreamImpl(System.err), true);
            System.setOut(outLogRedirect);
            System.setErr(errLogRedirect);
        }
    }

    public static enum MACHINE_TYPES {
        Apple2, Apple2e, Apple2c, Apple2gs
    };

    public static enum PORT_TYPES {
        SERIAL, TCP
    };

    public static enum DISPLAY_TYPES {
        Text_40col, Hires, Hires_Buffered
    };
    @Configurable(category = CATEGORY.COM, isRequired = true)
    public static MACHINE_TYPES MACHINE_TYPE = MACHINE_TYPES.Apple2e;
    @Configurable(category = CATEGORY.COM, isRequired = true)
    public static PORT_TYPES PORT_TYPE = PORT_TYPES.TCP;
    @Configurable(category = CATEGORY.COM, isRequired = true)
    public static String HOST = "localhost";
    @Configurable(category = CATEGORY.COM, isRequired = true)
    public static String PORT = "1977";
    @Configurable(category = CATEGORY.COM, isRequired = false)
    public static boolean DEBUG_BOOTSTRAP = false;
    @Configurable(category = CATEGORY.ADVANCED, isRequired = false)
    public static DISPLAY_TYPES DISPLAY_TYPE = DISPLAY_TYPES.Hires_Buffered;
    static Thread activeThread = null;
    private static boolean TERMINATE_PROGRAM = false;

    public static void checkRuntimeStatus() {
        if (TERMINATE_PROGRAM) {
            throw new RuntimeException("Program terminated by user intervention");
        }
        if (Main.instance != null) {
            Main.instance.setTitle("AGS 3.1 - " + activeThread.getName());
        }
    }

    private static void finished() {
        activeThread = null;
        Main.instance.startStopButton.setText(START_PROGRAM);
        Main.instance.startStopButton.setEnabled(true);
        Main.instance.setTitle("AGS 3.1 - Not running");
    }

    public static void startStop() {
        TERMINATE_PROGRAM = true;
        Main.instance.startStopButton.setEnabled(false);
        if (!TERMINATE_PROGRAM) {
            stop();
        } else {
            Main.instance.startStopButton.setText(STOP_PROGRAM);
            start();
        }
    }

    private static void stop() {
        TERMINATE_PROGRAM = true;

    }

    private static void start() {
        attachLogViewer(Main.instance.logDisplay);
        if (activeThread != null && activeThread.isAlive()) {
            return;
        }
        TERMINATE_PROGRAM = false;
        activeThread = new Thread(new Launcher());
        activeThread.setPriority(Thread.NORM_PRIORITY + 2);
        activeThread.start();
        Main.instance.startStopButton.setEnabled(true);
    }
    public TransferHost host;
    private SerialPort port;
    private List<Game> games;

//    @Override
    public void run() {
        try {
            validateRequirements();
            initPort();
            bootstrap();
            main();
        } catch (Throwable ex) {
            showError(ex.getMessage());
            Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            shutdown();
        }
    }

    private void showError(String message) {
        String showMessage = "";
        int len = 0;
        for (int i = 0; i < message.length(); i++) {
            showMessage += message.charAt(i);
            len++;
            if (len > 60) {
                char c = message.toLowerCase().charAt(i);
                if (c < 'a' || c > 'z') {
                    showMessage += "\n";
                    len = 0;
                }
            }
        }
        JOptionPane.showMessageDialog(null, "An error has occurred: \n" + showMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }


    private void validateRequirements() throws RuntimeException {
        games = GameUtil.readGames();
        if (games == null) {
            throw new RuntimeException("No games found.  Check your games directory is properly set and contains games.xml");
        }

    }

    public void initPort() throws Throwable {
        switch (PORT_TYPE) {
            case SERIAL:
                try {
                    port = SerialPort.getCommPort(PORT);
                    if (port.openPort()) {
                        System.out.println("Opened port: " + port.getSystemPortName());
                    } else {
                        port = null;
                        throw new IOException("Failed to open port '" + PORT + "'!");
                    }
                } catch (Throwable t) {
                    System.out.println("Error opening port: " + PORT);
                    t.printStackTrace();
                    port = null;
                    throw new IOException("Port '" + PORT + "' is not available!");
                }

                host = new TransferHost(port);
                break;

            case TCP:
                int portnum = Integer.parseInt(PORT);
                host =
                        new TCPTransferHost(HOST, portnum);
                break;

        }
    }

    public void bootstrap() throws Throwable {
        Script.USE_TARGET = MACHINE_TYPE.toString();
        if (DEBUG_BOOTSTRAP) {
            Script.USE_TARGET += "_debug";
        }

        host.init();
    }

    private void main() throws Throwable {
        try {
            Thread.currentThread().setName("Starting launcher");
            IVirtualScreen screen = null;
            switch (DISPLAY_TYPE) {
                case Hires:
                    screen = new HiresScreen();
                    break;
                case Hires_Buffered:
                    screen = new HiresBufferedScreen();
                    break;
                case Text_40col:
                    screen = new TextScreen40();
                    break;
            };
            GameSelectorApplication app = new GameSelectorApplication(screen);
            app.setGames(games);
            Thread.currentThread().setName("Launcher running");
            app.mainLoop(host);
        } catch (Throwable ex) {
            if (ex.getCause() != null) {
                ex = ex.getCause();
            }

            ex.printStackTrace();
            throw ex;
        }

    }

    public void shutdown() {
        if (port != null) {
            port.clearDTR();
            port.closePort();
        }

        Launcher.finished();
    }
}