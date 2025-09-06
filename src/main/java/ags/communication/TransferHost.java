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
import ags.ui.TextScreen40;
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
    public static String GAMES_DIR = "data/games";
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
        Game g;
        Part p = null;
        assert(game != null);
        
        if (game instanceof Game game1) {
            g = game1;
            Thread.currentThread().setName("Executing game " + g.getName());
            
            // Display loading screen with game name
            displayGameLoadingScreen(g.getName());
            
            if (g.getPart() != null && !g.getPart().isEmpty()) {
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
            // Do some ZERO-page patches (based on observations)
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
     * @return Length of transferred data
     * @throws java.io.IOException If there is a problem sending data
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
        if (g instanceof Part p) {
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
                System.out.println("Resource path: " + filename);
                System.out.println("ClassLoader: " + c.getClass().getName());
                InputStream diskStream = c.getResourceAsStream(filename);
                if (diskStream == null) {
                    // Try alternative resource loading approaches
                    System.out.println("First attempt failed, trying alternative paths...");
                    diskStream = c.getResourceAsStream("/" + filename);
                    if (diskStream == null) {
                        diskStream = TransferHost.class.getResourceAsStream("/" + filename);
                        if (diskStream == null) {
                            diskStream = ClassLoader.getSystemResourceAsStream(filename);
                        }
                    }
                    if (diskStream == null) {
                        throw new IOException("Disk image not found with any method: " + filename);
                    } else {
                        System.out.println("Successfully loaded disk image with alternative method");
                    }
                } else {
                    System.out.println("Successfully loaded disk image: " + filename);
                }
                Disk33 boot = new Disk33(diskStream);
                d.insertDisk(5, 0, boot);
            } catch (IOException ex) {
                System.err.println("Failed to load disk image: " + g.getName());
                System.err.println("Error: " + ex.getMessage());
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
            if (data == null) {
                // Try alternative resource loading methods
                data = TransferHost.class.getClassLoader().getResourceAsStream(filename);
                if (data == null) {
                    data = TransferHost.class.getResourceAsStream("/" + filename);
                    if (data == null) {
                        throw new IOException("Driver not found: " + filename);
                    }
                }
            }
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
    
    /**
     * TinyLoader protocol constants
     */
    private static final int TINYLOADER_INITIAL_CHUNK_SIZE = 128; // Larger chunks for better throughput
    private static final int TINYLOADER_MIN_CHUNK_SIZE = 8;
    private static final int TINYLOADER_MAX_RETRIES = 3;
    private static final int TINYLOADER_BYTE_TIMEOUT = 100; // 100ms timeout
    private static final byte[] ZERO = new byte[]{0};
    private static final byte[] ZERO_ASCII = new byte[]{'0'};
    
    /**
     * Send data using the TinyLoader protocol with optional reset
     * @param fileData Data to send
     * @param addressStart Starting address in apple's ram to load data
     * @throws IOException If there was trouble sending data after retries
     * @return Total number of errors experienced when sending data
     */
    public int sendRawDataTinyLoader(byte[] fileData, int addressStart) throws IOException {
        int dataOffset = 0;
        int target = addressStart;
        int remaining = fileData.length;
        int errors = 0;
        int chunkSize = TINYLOADER_INITIAL_CHUNK_SIZE;
        
        while (remaining > 0) {
            int chunkRetriesLeft = TINYLOADER_MAX_RETRIES;
            boolean chunkSentSuccessfully = false;
            int attemptNumber = 1;
            while (!chunkSentSuccessfully && --chunkRetriesLeft > 0) {
                // First we ensure the loader starts at a ready state each time
                // If this fails, it throws an error because we cannot recover if reset doesn't work!
                resetTinyLoader();
                
                try {
                    System.out.printf("TinyLoader: Chunk attempt %d/%d - target=$%04X, size=%d\n", 
                        attemptNumber, TINYLOADER_MAX_RETRIES, target, Math.min(chunkSize, remaining));
                    
                    // Clear any residual buffered data before starting packet protocol
                    flush();
                    readBytes();
                    
                    // New sync protocol: send 0, expect 0 back
                    try {
                        writeByteAndExpectResponse((byte)0, (byte)0, TINYLOADER_BYTE_TIMEOUT);
                    } catch (IOException e) {
                        throw new IOException("Sync handshake failed: " + e.getMessage());
                    }
                    
                    // Now send the target address (little endian) expecting echo from client
                    byte addressLow = (byte) (target & 0x0ff);
                    try {
                        writeByteAndExpectResponse(addressLow, addressLow, TINYLOADER_BYTE_TIMEOUT);
                    } catch (IOException e) {
                        throw new IOException("Address low byte failed: " + e.getMessage());
                    }
                    
                    byte addressHigh = (byte) (target >> 8);
                    try {
                        writeByteAndExpectResponse(addressHigh, addressHigh, TINYLOADER_BYTE_TIMEOUT);
                    } catch (IOException e) {
                        throw new IOException("Address high byte failed: " + e.getMessage());
                    }
                    
                    // Send the packet length and expect echo
                    int bytesToSend = Math.min(chunkSize, remaining);
                    byte lengthByte = (byte) bytesToSend;
                    try {
                        writeByteAndExpectResponse(lengthByte, lengthByte, TINYLOADER_BYTE_TIMEOUT);
                    } catch (IOException e) {
                        throw new IOException("Length byte failed: " + e.getMessage());
                    }
                    
                    int currentChecksum = 0;
                    for (int i=0; i < bytesToSend; i++) {
                        byte dataByte = fileData[dataOffset + i];
                        currentChecksum = currentChecksum ^ (dataByte & 0x0ff);
                        byte expectedChecksum = (byte) currentChecksum;
                        
                        try {
                            // Use optimized write method that doesn't wait extra time
                            writeByteAndExpectResponse(dataByte, expectedChecksum, TINYLOADER_BYTE_TIMEOUT);
                        } catch (IOException e) {
                            throw new IOException(String.format("Data byte %d/%d failed: %s", i+1, bytesToSend, e.getMessage()));
                        }
                    }
                    target += bytesToSend;
                    remaining -= bytesToSend;
                    dataOffset += bytesToSend;
                    chunkSentSuccessfully = true;
                } catch (IOException e) {
                    // Uh oh, this didn't work.  Retry!
                    System.out.printf("TinyLoader: Error encountered during chunk transfer: %s\n", e.getMessage());
                    System.out.printf("TinyLoader: Reducing chunk size from %d to %d and retrying\n", chunkSize, Math.max(TINYLOADER_MIN_CHUNK_SIZE, chunkSize / 2));
                    chunkSize = Math.max(TINYLOADER_MIN_CHUNK_SIZE, chunkSize / 2);
                    errors++;
                }
            }
            if (!chunkSentSuccessfully) {
                throw new IOException(String.format("TinyLoader: Failed to send chunk after %d attempts - target=$%04X, size=%d", 
                    TINYLOADER_MAX_RETRIES, target, Math.min(chunkSize, remaining)));
            }
        }
        if (errors > 0) {
            System.out.printf("TinyLoader: Transfer complete - sent %d bytes with %d errors\n", fileData.length, errors);
        }
        return errors;
    }
        
    public void resetTinyLoader() throws IOException{

        // First flush our in/out buffers
        flush();
        byte[] input = readBytes();
        
        // Check if our input buffer had anything that ended with "S0"
        if (input.length >= 2 && input[input.length-2] == 'S' && input[input.length-1] == '0' ) {
            // Looks like it already told us it was ready, but drain any extra responses
            DataUtil.wait(50);
            byte[] drain = readBytes();
            if (drain.length > 0) {
                System.out.printf("TinyLoader: Drained %d extra bytes from initial buffer\n", drain.length);
            }
            return;
        }
        
        boolean resetSuccessful = false;
        // Now send 0's until we see an S
        for (int i=0; !resetSuccessful && i < 255; i++) {
            // see if we have any input
            if (inputAvailable() > 0) {
                input = readBytes();
                
                // Look for "S0" pattern anywhere in the received data (handles buffered responses)
                boolean foundS0 = false;
                for (int j = 0; j < input.length - 1; j++) {
                    if (input[j] == 'S' && input[j + 1] == '0') {
                        foundS0 = true;
                        break;
                    }
                }
                
                if (foundS0) {
                    resetSuccessful = true;
                    // Give time for any remaining S0 responses to arrive and then drain them
                    DataUtil.wait(50);
                    byte[] drain = readBytes();
                    if (drain.length > 0) {
                        System.out.printf("TinyLoader: Drained %d extra bytes after reset\n", drain.length);
                    }
                }
            }
            if (!resetSuccessful) {
                writeQuickly(ZERO);
            }
        }
        
        if (!resetSuccessful) {
            throw new IOException("TinyLoader: Failed to get S0 response after 255 attempts");
        }
    }
    
    /**
     * Execute code at specified address using TinyLoader
     * @param address target address to JMP to
     */
    public void executeTinyLoader(int address) throws IOException {
        System.out.printf("TinyLoader: Executing code at $%04X\n", address);

        resetTinyLoader();
        
        // Clear any residual buffered data before starting execution protocol
        flush();
        readBytes();
        
        // New sync protocol: send 0, expect 0 back
        try {
            writeByteAndExpectResponse((byte)0, (byte)0, TINYLOADER_BYTE_TIMEOUT);
        } catch (IOException e) {
            throw new IOException("Execution sync handshake failed: " + e.getMessage());
        }

        // Now send the target address (little endian) expecting echo from client
        byte addressLow = (byte) (address & 0x0ff);
        try {
            writeByteAndExpectResponse(addressLow, addressLow, TINYLOADER_BYTE_TIMEOUT);
        } catch (IOException e) {
            throw new IOException("Execution address low byte failed: " + e.getMessage());
        }
        
        byte addressHigh = (byte) (address >> 8);
        try {
            writeByteAndExpectResponse(addressHigh, addressHigh, TINYLOADER_BYTE_TIMEOUT);
        } catch (IOException e) {
            throw new IOException("Execution address high byte failed: " + e.getMessage());
        }
        
        // Send the packet length (no echo) - 0 means execute
        writeQuickly(ZERO);
    }
        
    /**
     * Initialize TinyLoader with welcome message on screen
     */
    public void initTinyLoaderWithWelcome() throws IOException {
        System.out.println("Loading TinyLoader and displaying welcome message...");
        
        // Create a TextScreen40 instance to draw the starburst background
        TextScreen40 screen = new TextScreen40();
        screen.drawLoadingScreen("APPLE GAME SERVER", "LOADING SYSTEM...");
        
        // Send screen data using TinyLoader protocol (no reset needed - freshly bootstrapped)
        byte[] screenData = screen.getTextScreenBuffer();
        sendRawDataTinyLoader(screenData, 0x400);
    }
    
    /**
     * Display loading screen with game name using SOS kernel text screen
     */
    private void displayGameLoadingScreen(String gameName) throws IOException {
        System.out.println("Displaying loading screen for: " + gameName);
        
        // Create a TextScreen40 instance to draw the starburst background
        TextScreen40 screen = new TextScreen40();
        screen.drawLoadingScreen("LOADING GAME", gameName);
        
        // Send the screen buffer to Apple II memory using standard memory operations
        byte[] screenData = screen.getBuffer();
        sendRawData(screenData, 0x400, 0, screenData.length);
        
        System.out.println("Game loading screen displayed for: " + gameName);
    }
        

    /**
     * Generate 4+4 encoded BASIC with direct PEEK approach (like autumn.bas)
     * Much more compact - no variables needed, direct memory access
     * @param binaryData The binary data to encode
     * @param targetAddress Where to load the data
     * @param executeAddress Address to execute after loading
     * @param offset Character offset (32 or 64)
     * @return BASIC program as single line
     */
    private String generate44EncodedBasicWithOffset(byte[] binaryData, int targetAddress, int offset, boolean execute) {
        // Encode the binary data as high nibbles + low nibbles
        StringBuilder highNibbles = new StringBuilder();
        StringBuilder lowNibbles = new StringBuilder();
        
        for (byte b : binaryData) {
            int byteValue = b & 0xFF;
            int highNibble = (byteValue >> 4) + offset;  // Upper 4 bits + offset
            int lowNibble = (byteValue & 0x0F) + offset; // Lower 4 bits + offset
            
            highNibbles.append((char)highNibble);
            lowNibbles.append((char)lowNibble);
        }
        
        // Two-step approach to avoid keyboard buffer limitations:
        // Step 1: Store encoded data on line 1: 1"[data]"
        // Step 2: Immediate FOR loop reads from 0x806 (2054) where string data starts
        
        StringBuilder result = new StringBuilder();
        
        // Step 1: Line with encoded data
        result.append("1\"");
        result.append(highNibbles.toString());
        result.append(lowNibbles.toString());
        result.append("\"\n");
        
        // Step 2: Immediate FOR loop to decode and execute
        // String data starts at 0x806 (2054) after tokenizing line 1
        int highNibblesAddr = 2054; // 0x806 - where string data starts in line 1  
        int lowNibblesAddr = highNibblesAddr + binaryData.length;
        
        result.append("FORX=0TO");
        result.append(binaryData.length - 1);
        result.append(":POKE");
        result.append(targetAddress);
        result.append("+X,(PEEK(");
        result.append(highNibblesAddr);
        result.append("+X)-");
        result.append(offset);
        result.append(")*16+(PEEK(");
        result.append(lowNibblesAddr);
        result.append("+X)-");
        result.append(offset);
        result.append("):NEXT");
        if (execute) {
            result.append(": CALL");
            result.append(targetAddress);
        }
        
        return result.toString();
    }
    
    /**
     * Send 4+4 encoded TinyLoader bootstrap to Apple II
     * This replaces hex typing for initial TinyLoader loading
     * @param tinyLoaderPath Full path to TinyLoader binary
     */
    public void sendTinyLoader44Bootstrap(String tinyLoaderPath) throws IOException {
        byte[] tinyLoaderBinary = DataUtil.getFileAsBytes(tinyLoaderPath);

        String bootstrap = generate44EncodedBasicWithOffset(tinyLoaderBinary, 0x0300, 'A', true);
        if (bootstrap == null) {
            throw new IOException("Failed to generate TinyLoader 4+4 bootstrap");
        }
        
        System.out.println("Sending TinyLoader 4+4 bootstrap...");
        
        // Split into line 1 and immediate statement
        String[] parts = bootstrap.split("\n");
        String line1 = parts[0];        // 1"[encoded_data]"
        String forLoop = parts[1];      // FOR X=0 TO...
        
        System.out.println("Step 1 - Store data: " + line1);
        System.out.println("Step 2 - Execute: " + forLoop);
        
        // Step 1: Send line 1 with encoded data using echo verification
        writeParanoid(line1);
        writeOutput((byte)13); // Return key
        DataUtil.wait(500); // Give Apple II time to tokenize
        
        // Step 2: Send immediate FOR loop - executes automatically when return is pressed
        writeParanoid(forLoop);
        writeOutput((byte)13); // Return key - this executes immediately
        
        // Wait for TinyLoader to execute and initialize before starting communication
        DataUtil.wait(2000);
    }
    
}