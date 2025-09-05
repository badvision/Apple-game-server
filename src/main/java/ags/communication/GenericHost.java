package ags.communication;

import ags.controller.Launcher;
import ags.script.BadVariableValueException;
import ags.script.Variable;
import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generic host represents a common body of methods that can be used to
 * build any sort of communication program, specifically one that is capable of
 * interfacing with an apple computer at the standard applesoft basic prompt.
 *
 * Methods for error-resistant data transmission and advanced scripting support
 * are also provided for future reuse.
 *
 */
public class GenericHost {

    public static boolean LOG_OUTPUT = false;
    public static File LOG_FILE = null;

    /**
     * Enumeration of various flow control modes
     */
    public enum FlowControl {

        /**
         * No flow control -- ignore hardware flow control signals
         */
        none(SerialPort.FLOW_CONTROL_DISABLED),
        /**
         * XON/XOFF flow control mode
         */
        xon(SerialPort.FLOW_CONTROL_XONXOFF_IN_ENABLED),
        /**
         * Hardware flow control
         */
        hardware(SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED);
        /**
         * Value of flow control mode used by jSerialComm internally
         */
        private int val;

        /**
         * Constructor for enumeration
         *
         * @param v jSerialComm constant value
         */
        FlowControl(int v) {
            val = v;
        }

        /**
         * Get jSerialComm constant for this flow control mode
         *
         * @return desired flow control mode value
         */
        public int getConfigValue() {
            return val;
        }
    }
    /**
     * Active com port
     */
    private SerialPort port = null;
    /**
     * input stream for com port
     */
    InputStream in = null;
    /**
     * output stream for com port
     */
    OutputStream out = null;
    /**
     * Most recently set baud rate
     */
    int currentBaud = 1;
    /**
     * Current flow control mode being used
     */
    FlowControl currentFlow = FlowControl.none;
    /*
     * Legacy mode means we are talking to a much older ][ which doens't like lowercase.
     */
    static boolean legacyMode = false;

    public static void setLegacyMode(boolean mode) {
        legacyMode = mode;
    }

    public static boolean isLegacyMode() {
        return legacyMode;
    }
    /**
     * CTRL-X is used to cancel a line of input
     */
    public static char CANCEL_INPUT = 24; // CTRL-X cancels the input
    // When sending data to the apple in hex, this controls how many bytes are sent per line
    public static String HEX_BYTES_PER_LINE = "hexBytesPerLine";
    private boolean echoCheck;

    static {
        try {
            (new Variable(HEX_BYTES_PER_LINE)).setValue(String.valueOf(80));
        } catch (BadVariableValueException ex) {
            ex.printStackTrace();
        }
    }
    static GenericHost instance;
    String expectedPrompt = null;

    public String getExpectedPrompt() {
        return expectedPrompt;
    }

    public void setExpectedPrompt(String expectedPrompt) {
        this.expectedPrompt = expectedPrompt;
    }

    public GenericHost() {
        instance = this;
    }

    /**
     * Creates a new instance of GenericHost
     *
     * @param port Open serial port ready to use
     */
    public GenericHost(SerialPort port) {
        this();
        setEchoCheck(true);
        this.port = port;
        try {
            in = port.getInputStream();
            out = port.getOutputStream();
            this.port.setDTR();
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public static GenericHost getInstance() {
        return instance;
    }

    /**
     * Configure the port to the desired baud rate, 8-N-1
     *
     * @param baudRate Baud rate (e.g. 300, 1200, ...)
     */
    public boolean isEchoCheck() {
        return echoCheck;
    }

    public void setEchoCheck(boolean echoCheck) {
        System.out.println("echo check set to " + echoCheck);
        this.echoCheck = echoCheck;
    }

    public void setBaud(int baudRate) {
        try {
            port.setBaudRate(baudRate);
            port.setNumDataBits(8);
            port.setNumStopBits(1);
            port.setParity(SerialPort.NO_PARITY);
            port.setFlowControl(currentFlow.val);
            port.setDTR();
            currentBaud = baudRate;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setFlowControl(FlowControl f) {
        currentFlow = f;
        System.out.println("Local flow control set to " + f);
        setBaud(currentBaud);
    }

    /**
     * Open the desired binary file and send its contents to the remote apple as
     * hex digits This emulates how a user would manually type in a program via
     * keyboard.
     *
     * @param filename the path of the binary file to send
     * @param start the address (in hex) where the data should be placed in the
     * Apple's memory
     * @throws java.io.IOException if there was a problem opening or sending the
     * file data
     */
    public void typeHex(String filename, String start) throws IOException {
        System.out.println("Typing contents of binary file " + filename + ", starting at address " + start);
        byte[] data;
        try {
            data = DataUtil.getFileAsBytes(filename);
        } catch (IOException ex) {
            System.out.println("Got an error when trying to read binary file " + filename + ", please check that this file exists and, if necessary, its containing directory is in the classpath");
            throw ex;
        }
        int addr;
        try {
            addr = Integer.parseInt(start, 16);
        } catch (NumberFormatException ex) {
            throw new IOException("Bad starting address was passed in to the typeHex command: " + start);
        }
        setExpectedPrompt("*");
        int hexBytes = Integer.parseInt(Variable.getVariable(HEX_BYTES_PER_LINE).getValue());
        for (int pos = 0; pos < data.length; pos += hexBytes) {
            StringBuffer bytes = new StringBuffer();
            for (int i = 0; i < hexBytes && i + pos < data.length; i++) {
                if (i > 0) {
                    bytes.append(" ");
                }
                bytes.append(Integer.toString(data[pos + i] & 0x00ff, 16));
            }
            String out = Integer.toString(addr + pos, 16) + ":" + bytes + "\r";
            try {
                writeParanoid(out.toUpperCase());
            } catch (IOException ex) {
                System.out.println("Unable to write binary file: " + ex.getMessage());
                throw ex;
            } catch (Throwable ex) {
                System.out.println("Unable to write binary file: " + ex.getMessage());
            }
        }
        System.out.println("Finished writing file");
    }

    /**
     * Write a string to the remote host. If expectEcho == true, then data sent
     * is also expected to echo back to us. If the data is not echoed back in a
     * reasonable amount of time, the command to cancel the line is sent and the
     * process is retried a few times. If expectEcho == false, the data is sent
     * blindly and a calculated amount of time is elapsed before continuing on.
     *
     * @param s String to send
     * @throws java.io.IOException If the line could not be written
     */
    public void writeParanoid(String s) throws IOException {
        if (s == null || s.length() == 0) return;
        if (legacyMode) {
            s = s.toUpperCase();
        }
        byte bytes[] = s.getBytes();
        byte[] expect = new byte[1];
        if (!isEchoCheck()) {
            // We want to wait the right amount of time per character
            // So that will either be the amount of time it takes for the apple to process a character
            // Or it will be the amount of time it takes to send a character, whichever is slower.
            long waitTime = DataUtil.nanosPerCharAtSpeed(currentBaud);
            // Assume the apple takes so many cycles per character
            long cycleTime = DataUtil.cyclesToNanos(4000);
            waitTime = Math.max(waitTime, cycleTime) * 2;
//            waitTime += DataUtil.cyclesToNanos(100);
            writeSlowly(s);
            out.flush();
            // Add some additional wait time after every line, say 1000 cycles per character
            long lineWait = DataUtil.cyclesToNanos(1000 * (bytes.length + 1));
            DataUtil.nanosleep(lineWait);
        } else {
            // Purge input buffer first...
            String in = readString();
            while (in != null && in.length() > 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GenericHost.class.getName()).log(Level.SEVERE, null, ex);
                }
                in = readString();
            }
            boolean tooManyErrors = true;
            for (int errors = 0; errors < 3; errors++) {
                Launcher.checkRuntimeStatus();
                System.out.print("}>");
                try {
                    for (int i = 0; i < s.length(); i++) {
                        String o = s.substring(i, i+1);
                        writeQuickly(o);
                        System.out.print(o);
                        if (o.charAt(0) >= 32) {
                            expect(o, 500, false);
                        }
                    }
                    System.out.println("<{");
                    tooManyErrors = false;
                    break;
                } catch (IOException ex) {
                    System.out.println();
                    System.out.println("Failure writing line, retrying... (exception " + ex.getMessage() + ")");
                    cancelLine();      // control-x
                }
            }
            if (tooManyErrors) {
                throw new IOException("Cannot write " + s);
            }
        }
        expectPrompt();
    }

    /**
     * Sends a CTRL-X character, which cancels the current line on the remote
     * host
     *
     * @throws java.io.IOException If there is trouble sending data to the
     * remote host
     */
    public void cancelLine() throws IOException {
        writeOutput(new byte[]{(byte) CANCEL_INPUT});      // control-x
    }

    /**
     * Expect the desired prompt to be sent back (if expectEcho == true)
     *
     * @throws java.io.IOException If the monitor prompt is not sent to us
     */
    public void expectPrompt() throws IOException {
        if (expectedPrompt == null || expectedPrompt.equals("")) {
            return;
        }
        if (isEchoCheck()) {
            try {
                expect(expectedPrompt, 1000, false);
            } catch (IOException ex) {
                cancelLine();
                expect(expectedPrompt, 500, false);
            }
        } else {
            DataUtil.wait(500);
        }
    }

    /**
     * Expect a specific set of bytes to be sent in a given amount of time
     *
     * @param data data expected
     * @param timeout max time to wait for data
     * @throws java.io.IOException if there is a timeout waiting for the data
     * @return true
     */
    public boolean expectBytes(byte data[], int timeout)
            throws IOException {
        int length = Math.max(80, Math.max(inputAvailable(), data.length * 2));
        ByteBuffer bb = ByteBuffer.allocate(length);
//        System.out.println("setting receive buffer to "+length+" bytes");
        while (timeout > 0) {
            Launcher.checkRuntimeStatus();
            while (inputAvailable() == 0 && timeout > 0) {
                Launcher.checkRuntimeStatus();
                timeout -= 1;
                DataUtil.wait(1);
            }

            if (timeout > 0) {
                byte receivedData[] = readBytes();
//                StringBuffer test = new StringBuffer();
//                for (int i=0 ; i < receivedData.length; i++) {
//                    test.append((char) (receivedData[i]&0x07f));
//                    test.append("("+(receivedData[i] & 0x0ff)+") ");
//                }
//                System.out.println("read "+receivedData.length+" bytes" + test.toString());
                bb.put(receivedData);
                if (DataUtil.bufferContains(bb, data)) {
                    return true;
                }
            }
        }
        if (bb.position() == 0) {
            throw new IOException("expected " + Arrays.toString(data) + " but timed out");
        } else {
            throw new IOException("Expected " + Arrays.toString(data) + " but got " + Arrays.toString(bb.array()));
        }
    }

    /**
     * Expect a specific string to be sent in a given amount of time
     *
     * @param string data expected
     * @param timeout max time to wait for data
     * @param noConversion if true, string is treated as-is. If false, expected
     * data is converted to the high-order format that the apple sends back
     * natively
     * @return true
     * @throws java.io.IOException if there is a timeout waiting for the data
     */
    public boolean expect(String string, int timeout, boolean noConversion)
            throws IOException {
        StringBuffer searchString = new StringBuffer();
        while (timeout > 0) {
            Launcher.checkRuntimeStatus();
            for (; inputAvailable() == 0 && timeout > 0; timeout--) {
                DataUtil.wait(1);
            }
            String receivedString = readString();
            if (!noConversion) {
                receivedString = DataUtil.convertFromAppleText(receivedString);
            }
            searchString.append(receivedString);
            if (searchString.toString().contains(string)) {
                return true;
            }
        }
        if (searchString.equals("")) {
            throw new IOException("expected " + string + " but timed out");
        } else {
            throw new IOException("Expected " + string + " but got " + searchString.toString());
        }
    }

    /**
     * Get all avail. com input data as a string
     *
     * @throws java.io.IOException if there is a problem with the port
     * @return string of data
     */
    public String readString()
            throws IOException {
        return DataUtil.bytesToString(readBytes());
    }

    /**
     * Get all avail. com input data as an array of bytes
     *
     * @throws java.io.IOException if there is a problem with the port
     * @return data
     */
    public byte[] readBytes()
            throws IOException {
        byte data[] = new byte[inputAvailable()];
        readInput(data);
        return data;
    }

    /**
     * write a string out and sleep a little to let the buffer empty out
     *
     * @param s String to write out
     * @throws java.io.IOException If the data could not be sent
     */
    public void writeSlowly(String s) throws IOException {
        System.out.println(">>" + s);
        byte bytes[] = s.getBytes();
        // Calculate proper wait time based on actual character transmission time
        // Use 3x safety margin: actual char time * 3, with reasonable bounds
        long nanosPerChar = DataUtil.nanosPerCharAtSpeed(currentBaud);
        int waitTime = (int) Math.max(1, Math.min(100, (nanosPerChar * 3) / 1000000));
        
        // Special case for very slow baud rates
        if (currentBaud <= 300) {
            waitTime = 75;
        }
        for (int i = 0; i < bytes.length; i++) {
            waitToSend(100);
            writeOutput(bytes, i, 1);
            out.flush();
            DataUtil.wait(waitTime);
        }
    }

    /**
     * write a string out, faster than writeSlowly
     *
     * @param s String to write out
     * @throws java.io.IOException If the data could not be sent
     */
    public void writeQuickly(String s) throws IOException {
        byte bytes[] = s.getBytes();
        int waitTime = Math.min(5, Math.max(100, (115200 / currentBaud) * 10));
        for (int i = 0; i < bytes.length; i++) {
            waitToSend(50);
            writeOutput(bytes, i, 1);
            out.flush();
            DataUtil.wait(waitTime);
        }
    }

    //-------------------------------------
    //--- Raw i/o routines, isolated for better debugging and flow control support
    //-------------------------------------
    /**
     * Returns the number of bytes available in the input buffer
     *
     * @throws java.io.IOException If the port cannot be accessed
     * @return Number of available bytes of input in buffer
     */
    public int inputAvailable() throws IOException {
//        System.out.println("inputAvailable - start");
        int avail = 0;
        avail = in.available();
//        System.out.println("inputAvailable = "+avail);
        return avail;
    }

    /**
     * Fill the provided byte[] array with data if possible
     *
     * @param buffer buffer to fill
     * @throws java.io.IOException If data could not be read for any reason
     * @return Number of bytes read into buffer
     */
    int readInput(byte[] buffer) throws IOException {
//        System.out.println("Reading data");
        int size = in.read(buffer);
        if (size != buffer.length) {
            System.out.println("Buffer was of size " + buffer.length + " but we got back " + size);
        }
//        System.out.println("Read "+size+" bytes");
        return size;
    }

    /**
     * Write entire buffer to remote host
     *
     * @param buffer Buffer of data to send
     * @throws java.io.IOException If data could not be sent
     */
    public void writeOutput(byte... buffer) throws IOException {
        writeOutput(buffer, 0, buffer.length);
    }

    /**
     * Wait x number of milliseconds for remote host to allow us to send data
     * (if flow control == hardware mode)
     *
     * @param timeout Number of milliseconds to wait before throwing timeout
     * error
     * @throws java.io.IOException If we timed out waiting for "clear to send"
     * signal
     */
    void waitToSend(int timeout) throws IOException {
        if (currentFlow == FlowControl.hardware) {
//            System.out.println("Waiting for CTS");
            while (!port.getCTS() && timeout > 0) {
                DataUtil.wait(10);
                timeout -= 10;
            }
            if (timeout <= 0) {
                throw new IOException("Timed out waiting to send data to remote host!");
            }
//            System.out.println("Finished waiting for CTS");
        }
    }

    /**
     * Write data to host from provided buffer
     *
     * @param buffer Buffer of data to send
     * @param offset Starting offset in buffer to write
     * @param length Length of data to write
     * @throws java.io.IOException If there was trouble writing to the port
     */
    public void writeOutput(byte[] buffer, int offset, int length) throws IOException {
        if (LOG_OUTPUT) {
            FileOutputStream f = new FileOutputStream(LOG_FILE, true);
            f.write(buffer, offset, length);
            f.close();
        }
        if (buffer == null || offset >= buffer.length || buffer.length == 0 || length == 0) {
            return;
        }
        out.write(buffer, offset, length);
    }
}