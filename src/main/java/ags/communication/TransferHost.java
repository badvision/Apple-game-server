package ags.communication;

import ags.controller.Configurable;
import ags.controller.Configurable.CATEGORY;
import ags.controller.FileType;
import ags.controller.Launcher;
import ags.disk.Disk33;
import ags.disk.Drive;
import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import ags.game.Game;
import ags.game.Game.Disk;
import ags.game.Part;
import ags.game.GameBase;
import static ags.game.GameUtil.*;
import ags.script.Engine;
import ags.script.Target;
import ags.script.Variable;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Communicates with apple serial driver to send binary data and execute programs on a remote apple //
 * @author blurry
 */
public class TransferHost extends GenericHost {

    /**
     * File containing init commands to get the apple to the monitor and init the SSC
     */
    @Configurable(category = CATEGORY.RUNTIME, isRequired = true)
    @FileType("txt")
    public static String INIT_FILE = "ags/resources/init.txt";
    /**
     * Directory containing game files
     */
    @Configurable(category = CATEGORY.RUNTIME, isRequired = true)
    public static String GAMES_DIR = "games";
    /**
     * Maximum size of data chunk to send at a time
     */
    @Configurable(category = CATEGORY.ADVANCED, isRequired = false)
    public static int MAX_CHUNK_SIZE = 4096;
    /**
     * Maximum consecutive errors allowed when transferring binary data
     */
    @Configurable(category = CATEGORY.ADVANCED, isRequired = false)
    public static int MAX_ERRORS_ALLOWED = 10;
    /**
     * Number of ack requests to send in a row when performing a last-ditch effort to communicate with the apple
     */
    @Configurable(category = CATEGORY.ADVANCED, isRequired = false)
    public static int MAX_ACK_BURST = 16;
    /**
     * Address of routine to run the current basic program
     */
    public static final int BASIC_RUN = 0x00d566;
    /**
     * Basic program starting address pointer
     */
    public static final int BASIC_PTR_START = 0x0067;
    /**
     * Basic variable LOMEM variable pointer
     */
    public static final int BASIC_PTR_LOMEM = 0x0069;
    /**
     * Basic variable HIMEM variable pointer
     */
    public static final int BASIC_PTR_HIMEM = 0x0073;
    /**
     * Basic program ending address pointer
     */
    public static final int BASIC_PTR_END = 0x00AF;
    /**
     * The acknowledge message from the driver: "hi"
     */
    public static final String DRIVER_ACK = "hi";
    /**
     * Number of times to try requesting ACK before giving up and halting
     */
    @Configurable(category = CATEGORY.ADVANCED, isRequired = true)
    public static int NUM_ACK_RETRIES = 5;
    
    /**
     * Packbits decompressor assembler routine (loads at $6000)
     */
    @Configurable(category = CATEGORY.ADVANCED, isRequired = true)
    @FileType("o,asm,obj")
    public static String DECOMPRESSOR_ROUTINE = "ags/asm/deflate.o";

    public TransferHost() {
        super();
    }

    /**
     * Constructor
     * @param port active port to use
     */
    public TransferHost(SerialPort port) {
        super(port);
    }

    /**
     * Init the apple and send the driver code to it
     * @throws java.io.IOException If there is a problem resulting in unexpected input
     */
    public void init() throws IOException {
        Thread.currentThread().setName("Initalizing Serial Driver");
//        expectEcho = false;
        System.out.println("Executing init script.");
        Engine.start(INIT_FILE);
        // ensure the loader is running, falling back to the test routine if necessary
        try {
            expect(DRIVER_ACK, 2000, false);
            System.out.println("Received acknowledgement response from Apple!");
            GenericHost.setBootstrapPhase(false); // Switch to fast runtime communication
        } catch (IOException e) {
            Thread.currentThread().setName("Error during startup");
            System.out.println("Didn't get an immediate response from the driver, trying a few acknowledge tests.");
            try {
                testDriver();
                System.out.println("Received acknowledgement response from Apple!");
                GenericHost.setBootstrapPhase(false); // Switch to fast runtime communication
                Thread.currentThread().setName("Recovered from startup error");
            } catch (IOException ex) {
                System.out.println("ALERT: Didn't detect the apple driver is running.  Ensure it is started (will retry in 20 seconds)");
                System.out.println("For example, try pressing ctrl-reset on the apple and typing CALL 2049");
            }
        }
    }

    /**
     * Send a chunk of raw binary data directly to the apple's ram
     * @param fileData Data to send
     * @param addressStart Starting address in apple's ram to load data
     * @param dataStart Starting offset in data to send
     * @param length Length of data to send over
     * @throws java.io.IOException If there was trouble sending data after a number of attempts
     * @return Total number of errors experienced when sending data
     */
    public int sendRawData(byte[] fileData, int addressStart, int dataStart, int length) throws IOException, IOException {
        int offset = dataStart;
        int end = dataStart + length;
        int chunkSize = MAX_CHUNK_SIZE;
        int totalErrors = 0;
        int errors = 0;
        testDriver();
        while (offset < end && errors < MAX_ERRORS_ALLOWED) {
            Launcher.checkRuntimeStatus();
            // Set size so that it won't exceed the remainder of the data
            int size = Math.min(chunkSize, end - offset);
//            System.out.println("sending offset: " + offset + ", length=" + size);
            writeQuickly("A");
            writeOutput(DataUtil.getWord(offset + addressStart));
            writeQuickly("B");
            // Add 1 to hi and lo bytes after subtracting one from the total size
            // This was done here to reduce the SOS driver size by 4 bytes
            int useSize = (0x0ff00 & (size + 255)) | (0x0FF & (size));
            writeOutput(DataUtil.getWord(useSize));
            writeQuickly("C");
            // Send data chunk
            writeOutput(fileData, offset, size);
            try {
                // Verify checksum
                byte[] checksum = computeChecksum(fileData, offset, size);
                readBytes(); // Clear input buffer
                writeQuickly("D");
                expectBytes(checksum, 500);
                // If we got this far then the checksum matched.
                // Move on to the next chunk and restablish the max chunk size.
                offset += chunkSize;
                errors = 0;
                chunkSize = MAX_CHUNK_SIZE;
            } catch (IOException e) {
                // If we didn't get the expected checksum, retry with a smaller chunk size
                System.out.println("Checksum failed: " + e.getMessage());
                errors++;
                totalErrors++;
                chunkSize = chunkSize / 2;
                if (chunkSize < 2) {
                    chunkSize = 2;
                }
                // Verify we can still get a response from the driver
                // In the case of missing bytes this should fill the gap, so to speak
                tryToFixDriver();
            }
        }
        if (errors >= MAX_ERRORS_ALLOWED) {
            throw new IOException("TOO MANY CHECKSUM ERRORS!  ABORTING TRANSFER!");
        }
        return totalErrors;
    }

    /**
     * Store a byte value in the apple's ram
     * @param address Apple memory address to set
     * @param b Value to store
     * @throws java.io.IOException If data could not be sent correctly
     */
    public void storeMemory(int address, byte... b) throws IOException {
        System.out.println("Storing " + b.length + " bytes at " + Integer.toHexString(address));
        sendRawData(b, address, 0, b.length);
    }

    /**
     * Store a chunk of values into the apple's ram (for memory patching)
     * @param address Starting address in Apple's ram to store data
     * @param i One or more bytes to store
     * @throws java.io.IOException If data could not be sent correctly
     */
    public void storeMemory(int address, int... i) throws IOException {
        byte[] b = new byte[i.length];
        for (int x = 0; x < i.length; x++) {
            b[x] = (byte) (i[x] & 0x00ff);
        }
        storeMemory(address, b);
    }

    /**
     * Tell the apple to jump to the specified address (set PC=address)
     * @param address Address to jump to
     * @param sub If true, tells driver to execute a JSR instead of a JMP -- this allows creation of extendable modules. :-)
     * @throws java.io.IOException If data could not be sent correctly
     */
    public void jmp(int address, boolean sub) throws IOException {
        testDriver();
        writeQuickly("A");
        writeOutput(DataUtil.getWord(address));
        if (sub) {
            writeQuickly("F");
        } else {
            writeQuickly("E");
        }
    }

    /**
     * Load and then start a game
     * @param game Game to start
     * @return true if apple is still connected, false if the apple is no longer connected
     * @throws java.io.IOException If data could not be sent correctly
     */
    public boolean startGame(GameBase game) throws IOException {
        Launcher.checkRuntimeStatus();
        Game g = null;
        Part p = null;
        if (game instanceof Game) {
            g = (Game) game;
            Thread.currentThread().setName("Executing game " + g.getName());
            if (g.getPart() != null && g.getPart().size() > 0) {
                for (Part gg : g.getPart()) {
                    System.out.println("Loading game part: " + gg.getName());
                    if (!startGame(gg)) {
                        System.out.println("Load process terminated at part " + gg.getName());
                        // The program did not return back to the driver.  Time to exit!
                        return false;
                    }
                }
            }
            if (g.getType().equalsIgnoreCase(TYPE_DISK)) {
                bootDiskGame(g);
                return false;
            }
        } else {
            p = (Part) game;
        }
        if (game.getFile() != null && !"".equals(game.getFile())) {
            // Do some zero-page patches (based on observations)
            /*
            storeMemory(0x2E, 0x21, 0x01);
            storeMemory(0x3A, 0xba, 0x00);
            storeMemory(0x45, 0x00, 0xff, 0x01, 0x35, 0xD4);
            storeMemory(0x80, 0x01);
             */
            if (game.getType().equalsIgnoreCase(TYPE_BASIC)) {
                int length = loadGame(game);
                // Set start address of basic program
                storeMemory(0xD8, 0x00); // Reset onErr flag
                storeMemory(toInt(game.getStart()) - 1, 0);
                storeMemory(BASIC_PTR_START, DataUtil.getWord(toInt(game.getStart())));
                storeMemory(BASIC_PTR_END, DataUtil.getWord(toInt(game.getStart() + length)));
                storeMemory(BASIC_PTR_LOMEM, DataUtil.getWord(toInt(game.getStart() + length)));
                storeMemory(BASIC_PTR_HIMEM, DataUtil.getWord(0x00BEFF));
                // Now execute it!
                jmp(BASIC_RUN, false);
                return false;
            } else if (!game.getType().equalsIgnoreCase(TYPE_BINARY)) {
                loadGame(game);
                return true;
            } else {
                loadGame(game);
                // Don't execute if type is LOAD
                if (p != null && p.getAction().equals(ACTION_LOAD)) {
                    return true;
                }
                boolean isSubroutine = p != null && p.getAction().equalsIgnoreCase(ACTION_SUB);
                if (p == null || isSubroutine || p.getAction().equalsIgnoreCase(ACTION_RUN)) {
                    jmp(toInt(game.getStart()), isSubroutine);
                }
                if (isSubroutine) {
                    for (int i = 0; i < 10000; i += 100) {
                        try {
                            testDriver();
                        } catch (IOException e) {
                            // The program did not return back to the driver yet.
                            DataUtil.wait(100);
                            continue;
                        }
                        return true;
                    }
                }
                // We launched a program and it's not coming back.
                return false;
            }
        }
        // We could still send more stuff to the apple at this point if we want to!
        return true;
    }

    /**
     * Send a game binary file to the apple
     * @param g Game to send to the apple
     * @return Length of transfered data
     * @throws java.io.IOException java.io.IOException If there is a problem sending data
     */
    public int loadGame(GameBase g) throws IOException {
        // So now the driver is started, start the upload process
        if (g.getName() != null) {
            Thread.currentThread().setName("Launching "+g.getName());
            System.out.println("Sending: " + g.getName());
        } else {
            System.out.println("Sending game part (no name)");

        }
        String fileName = GAMES_DIR + "/" + g.getFile();
        byte[] fileData = DataUtil.getFileAsBytes(fileName);

        int address = toInt(g.getStart());
        int length = fileData.length;
        int offset = 0;
        if (g instanceof Part) {
            Part p = (Part) g;
            if (p.getOffset() != null) {
                offset = Math.min(length - 1, toInt(p.getOffset()));
            }
            if (p.getLength() != null) {
                length = Math.min(length - offset, toInt(p.getLength()));
            }
        }
        System.out.println("Starting address: " + g.getStart());
        System.out.println("Length: " + length);

        int totalErrors = sendRawData(fileData, address - offset, offset, length);
        System.out.println("Finished transfering game with " + totalErrors + " errors");
        return length;
    }

    /**
     * Ensure driver is responsive (send ack command: @)
     * @throws java.io.IOException If the driver is not correctly responding in a timely manner
     */
    public void testDriver() throws IOException {
        int numRetries = NUM_ACK_RETRIES;
        while (numRetries > 0) {
            Launcher.checkRuntimeStatus();
            try {
                writeQuickly("@");
                expect(DRIVER_ACK, 1000, false);
                return;
            } catch (IOException ex) {
                // Ignore error for now
                //ex.printStackTrace();
            }
            numRetries--;
        }
        throw new IOException("Failed to get response from driver after " +
                NUM_ACK_RETRIES + " retries");
    }

    /**
     * Attempt to get a response from the apple by sending multiple ack requests to it
     * This is used at the end of a data transfer in case bytes were dropped
     * So that way, the missing bytes are filled in with @'s -- this should get the driver
     * back to a state where we can communicate with it and retry sending the data (this is up to the caller)
     * @throws java.io.IOException If the apple's driver is not responsive
     */
    public void tryToFixDriver() throws IOException {
        for (int i = 0; i < MAX_ACK_BURST; i++) {
            writeQuickly("@");
        }
        expect(DRIVER_ACK, 5000, false);    // We should get back at least one ACK
        DataUtil.wait(10);              // Wait for the ACK responses to stop
        readBytes();             // Flush out the buffer to eliminate any false positives
    }

    /**
     * Get a keypress from the apple's keyboard
     * @throws java.io.IOException If data could not be sent or received correctly
     * @return Keypress if any
     */
    public byte getKey() throws IOException {
        Launcher.checkRuntimeStatus();
        writeQuickly("G");
        for (int i = 0; (i < 500) && (inputAvailable() == 0); i++) {
            DataUtil.wait(1);
        }
        if (inputAvailable() > 0) {
            byte[] b = new byte[1];
            readInput(b);
            byte bb = b[0];
            if (bb >= 0) {
                return 0;
            }
            return (byte) (bb & 0x007f);
        } else {
            System.out.println("Not getting a response from the apple, testing connection");
            testDriver();
            System.out.println("Connection working, how odd.  Going on...");
            return 0;
        }
    }

    /**
     * Checksum methods used during data transfer
     * @param data file data
     * @param start start offset to checksum
     * @param size number of bytes to calculate checksum for
     * @return expected checksum
     */
    protected static byte[] computeChecksum(byte[] data, int start, int size) {
        byte checksum = 0;
        for (int i = start; i < start + size; i++) {
            checksum = (byte) ((0x00ff & checksum) ^ (0x00ff & data[i]));
        }
        return new byte[]{checksum};
    }

    private void bootDiskGame(Game g) throws IOException {
        Thread.currentThread().setName("Running in disk mode for game "+g.getName());
        System.out.println("Booting disk-based game: " + g.getName());
        ClassLoader c = TransferHost.class.getClassLoader();
        // Set up the drive device and all disks
        Drive d = new Drive(this);
        if (g.getFile() != null) {
            try {
                String filename = GAMES_DIR + "/" + g.getFile();
                System.out.println("Attempting to load disk image " + filename);
                Disk33 boot = new Disk33(filename);
                d.insertDisk(5, 0, boot);
            } catch (IOException ex) {
                Logger.getLogger(TransferHost.class.getName()).log(Level.SEVERE, "Error inserting disk " + g.getName(), ex);
            }
        }
        //TODO: Implement a mechanism to keep track of all disks for a game... (?)
        if (g.getDisk() != null) {
            for (Disk diskData : g.getDisk()) {
                try {
                    Disk33 otherDisk = new Disk33(c.getResourceAsStream(GAMES_DIR + "/" + diskData.getFile()));
                    d.insertDisk(diskData.getSlot() - 1, diskData.getDrive() - 1, otherDisk);
                } catch (IOException ex) {
                    Logger.getLogger(TransferHost.class.getName()).log(Level.SEVERE, "Error inserting disk " + diskData.getName(), ex);
                }
            }
        }
        // Boot the drive!
        d.boot();
    }

    public void loadDriver(String driverName, int address) {
        try {
            String filename = "";
            Target gs = Target.getTarget("apple2gs_setup");
            String slot = Variable.getVariable("slot").getValue();
            if (gs != null && gs.isRunAlready()) {
                filename = "ags/asm/" + driverName + "_gs_port" + slot + ".o";
            } else {
                filename = "ags/asm/" + driverName + "_ssc_slot" + slot + ".o";
            }
            InputStream data = ClassLoader.getSystemResourceAsStream(filename);
            byte[] driverData = new byte[data.available()];
            data.read(driverData);
            data.close();
            sendRawData(driverData, address, 0, driverData.length);
        } catch (IOException ex) {
            Logger.getLogger(TransferHost.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    boolean decompressorLoaded = false;

    private void loadDecompressor() throws IOException {
        if (!decompressorLoaded) {
            byte[] decompressor = DataUtil.getFileAsBytes(DECOMPRESSOR_ROUTINE);
            sendRawData(decompressor, 0x0BD00, 0, decompressor.length);
            // Register decompressor as a command
            jmp(0x0BD00, true);
            testDriver();
        }
        decompressorLoaded = true;
    }

    // Track if XOR is enabled at the moment according to data being encountered
    // The decompressor is assembled with XOR enabled, so start off with value TRUE
    boolean xorMode = true;
    public void sendCompressedData(byte[] compressedData) throws IOException {
        // Nothing to send?  Just exit and do nothing!
        if (compressedData == null || compressedData.length == 0) return;
        
        Launcher.checkRuntimeStatus();
        loadDecompressor();
        writeQuickly("H");
//        long start = System.nanoTime();
        // Send data chunk
        int next = 2;   // Send two byte header without caring what is in it.
        long wait = 0;
//        String debug = "Frame length "+compressedData.length+"\n";
        for (int i = 0; i < compressedData.length; i++) {
            // When we hit the next data block, look at it more thouroughly
            if (next == i) {
                out.flush();
                DataUtil.nanosleep(wait);
//                debug += ", ";
                if (compressedData[i] == 0 && i < compressedData.length-1) {
                    System.err.println("ERROR: Compression stream sent termination character 0x00 before end of actual data!");
                }
                if ((compressedData[i] & 0x080) == 0) {
                    if (compressedData[i] >= 0x07E) {
                        // XOR/Data store commands
                        next = i+1;
                        wait = 40;
                        xorMode = (compressedData[i] == 0x07E);
//                        debug += xorMode ? "!! " : "@@ ";
                    } else {
                        // Uncompressed data (73 cycles/char, below threshold)
                        next = i + compressedData[i] + 1;
                        wait = 73 - DataUtil.NANOS_PER_CHAR;
                    }
                } else {
                    next = i + 3;
                    if (compressedData[i + 1] != 0 || compressedData[i + 2] != 0 || !xorMode) {
                        // Compressed data
                        int reps = Math.abs(((compressedData[i] & 0x07f) + 2));
                        // 101 cycles per repetition
                        wait = DataUtil.cyclesToNanos(reps * 101) + DataUtil.NANOS_PER_CHAR;
                    } else {
                        // Compressed zeros in XOR mode == skipped
                        // 50 cycles to increment memory pointers
                        wait = DataUtil.cyclesToNanos(50);
                    }
                }
//                debug += Integer.toHexString(compressedData[i] & 0x0ff) + " ";
            }
            writeOutput(compressedData, i, 1);
        }
//        System.out.println(debug);
        out.flush();
        DataUtil.nanosleep(wait);
//        long end = System.nanoTime();
//        System.out.println("Took "+(end-start)+" nanos to send "+compressedData.length+" bytes");
    }

    public void toggleSwitch(int address) throws IOException {
        loadDecompressor();
        writeQuickly("I");
//        writeOutput(DataUtil.getWord(address));
        writeOutput((byte) (address & 0x0ff));
    }
}