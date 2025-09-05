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
    private static final int TINYLOADER_INITIAL_CHUNK_SIZE = 64; // Larger chunks for better throughput
    private static final int TINYLOADER_MIN_CHUNK_SIZE = 8;
    private static final int TINYLOADER_MAX_RETRIES = 3;
    private static final int TINYLOADER_BYTE_RETRIES = 3; // Fewer retries since it's working
    private static final int TINYLOADER_BYTE_TIMEOUT = 50; // 50ms timeout
    // No artificial delays needed - echo verification provides timing synchronization
    
    /**
     * Send data using the TinyLoader protocol - more resilient bootstrap transfer
     * @param fileData Data to send
     * @param addressStart Starting address in apple's ram to load data
     * @param dataStart Starting offset in data to send
     * @param length Length of data to send over
     * @throws IOException If there was trouble sending data after retries
     * @return Total number of errors experienced when sending data
     */
    public int sendRawDataTinyLoader(byte[] fileData, int addressStart, int dataStart, int length) throws IOException {
        // Ensure clean state with verified reset
        if (!resetAndVerifyTinyLoader()) {
            throw new IOException("TinyLoader not responding or unable to achieve clean state");
        }
        System.out.println("TinyLoader: Confirmed clean state and ready for transfer");
        
        int offset = dataStart;
        int end = dataStart + length;
        int totalErrors = 0;
        int maxChunkSize = Math.min(255, TINYLOADER_INITIAL_CHUNK_SIZE); // Max 255 bytes per transfer
        
        System.out.println("TinyLoader transfer: " + length + " bytes to $" + Integer.toHexString(addressStart));
        
        // Transfer in chunks of up to 255 bytes
        while (offset < end) {
            Launcher.checkRuntimeStatus();
            int size = Math.min(maxChunkSize, end - offset);
            int currentAddress = addressStart + (offset - dataStart);
            
            System.out.printf("TinyLoader: Transferring %d bytes to $%04X\n", size, currentAddress);
            
            boolean transferSuccess = false;
            for (int attempt = 0; attempt < TINYLOADER_MAX_RETRIES && !transferSuccess; attempt++) {
                try {
                    transferSuccess = sendTinyLoaderChunk(fileData, offset, size, currentAddress, attempt);
                    
                    // After successful chunk transfer, wait for TinyLoader to send S0
                    if (transferSuccess) {
                        try {
                            expect("S0", 100, false); // Wait for TinyLoader ready signal
                            System.out.println("TinyLoader: S0 received - ready for next chunk");
                        } catch (IOException e) {
                            System.out.println("TinyLoader: No S0 response - continuing anyway");
                            readBytes(); // Clear any stale data
                        }
                    }
                } catch (IOException e) {
                    totalErrors++;
                    System.out.printf("TinyLoader: Transfer attempt %d failed: %s\n", attempt + 1, e.getMessage());
                    
                    if (attempt == TINYLOADER_MAX_RETRIES - 1) {
                        // Final attempt - throw error and fail transfer
                        throw new IOException("TinyLoader: Failed to transfer chunk after " + TINYLOADER_MAX_RETRIES + " attempts", e);
                    }
                }
            }
            
            offset += size;
        }
        
        System.out.println("TinyLoader transfer complete with " + totalErrors + " total errors");
        return totalErrors;
    }
    
    /**
     * Execute code at specified address using TinyLoader
     */
    public void executeTinyLoader(int address) throws IOException {
        System.out.println("TinyLoader: Executing code at $" + Integer.toHexString(address));
        
        // Send: [addr_lo] [addr_hi] [size=0] to trigger execution - no delays needed
        writeOutput((byte)(address & 0xFF));
        writeOutput((byte)((address >> 8) & 0xFF));
        writeOutput((byte)0x00); // Size 0 = execute
        out.flush();
        
        System.out.println("TinyLoader: Execution command sent");
    }
    
    /**
     * Send a single chunk using the simplified TinyLoader protocol
     */
    private boolean sendTinyLoaderChunk(byte[] fileData, int offset, int size, int address, int attempt) throws IOException {
        // No artificial delays needed - echo verification provides timing sync
        
        // Temporarily disable echo checking during TinyLoader protocol to prevent echo corruption
        boolean originalEchoCheck = isEchoCheck();
        if (originalEchoCheck) {
            System.out.println("TinyLoader: Temporarily disabling echo checking to prevent serial echo corruption");
            setEchoCheck(false);
        }
        
        try {
            // Send: [addr_lo] [addr_hi] [size] [data...]
        System.out.printf("TinyLoader: Sending header: addr=%04X, size=%d\n", address, size);
        
        // Clear any stale data before sending header
        readBytes();
        
        byte addrLo = (byte)(address & 0xFF);
        byte addrHi = (byte)((address >> 8) & 0xFF);
        byte sizeB = (byte)size;
        
        System.out.printf("TinyLoader: Header bytes: %02X %02X %02X\n", addrLo & 0xFF, addrHi & 0xFF, sizeB & 0xFF);
        
        // Clear any pending responses before header
        readBytes();
        
        // Send header bytes with echo verification (TinyLoader now echoes each one)
        writeOutput(addrLo);
        out.flush();
        expectByte(addrLo & 0xFF, "address low echo");
        
        writeOutput(addrHi);
        out.flush();
        expectByte(addrHi & 0xFF, "address high echo");
        
        writeOutput(sizeB);
        out.flush(); 
        expectByte(sizeB & 0xFF, "size echo");
        
        System.out.printf("TinyLoader: Header echoes confirmed: %02X %02X %02X\n", addrLo & 0xFF, addrHi & 0xFF, sizeB & 0xFF);
        
        System.out.println("TinyLoader: Header sent, starting data transfer...");
        
        // Send data bytes with per-byte retry logic and running XOR checksum verification
        // Apple II sends running checksum: byte0 ^ byte1 ^ ... ^ currentByte
        // CRITICAL: Treat all bytes as unsigned (0-255) to avoid Java signed byte issues
        // Since we verified clean reset upfront, start with checksum = 0
        int runningChecksum = 0;
        for (int i = 0; i < size; i++) {
            int dataByte = fileData[offset + i] & 0xFF;  // Convert to unsigned
            runningChecksum ^= dataByte;  // XOR with unsigned values
            runningChecksum &= 0xFF;     // Keep in byte range
            
            boolean byteConfirmed = false;
            int byteRetries = 0;
            
            while (!byteConfirmed && byteRetries < TINYLOADER_BYTE_RETRIES) {
                try {
                    // Clear any stale input 
                    readBytes();
                    
                    // Send data byte and wait for checksum response (no artificial delays)
                    writeOutput((byte)dataByte);
                    out.flush();
                    
                    // Wait for checksum response with spin loop
                    long timeoutStart = System.currentTimeMillis();
                    while (in.available() == 0 && (System.currentTimeMillis() - timeoutStart) < TINYLOADER_BYTE_TIMEOUT) {
                        Thread.onSpinWait();
                    }
                    
                    if (in.available() == 0) {
                        throw new IOException("Timeout waiting for checksum");
                    }
                    
                    // Read checksum response and any additional data
                    int receivedChecksum = in.read() & 0xFF;
                    byte[] extraData = readBytes(); // Clear any additional data
                    
                    if (receivedChecksum == runningChecksum) {
                        byteConfirmed = true;
                    } else {
                        byteRetries++;
                        if (byteRetries >= TINYLOADER_BYTE_RETRIES) {
                            System.out.printf("TinyLoader: Checksum fail byte %d: expected %02X, got %02X", 
                                             i, runningChecksum, receivedChecksum);
                            if (extraData.length > 0) {
                                System.out.printf(" (+ %d extra bytes: ", extraData.length);
                                for (byte b : extraData) {
                                    System.out.printf("%02X ", b & 0xFF);
                                }
                                System.out.print(")");
                            }
                            System.out.println();
                        }
                    }
                    
                } catch (IOException e) {
                    byteRetries++;
                    if (byteRetries >= TINYLOADER_BYTE_RETRIES) {
                        System.out.printf("TinyLoader: Error on byte %d: %s\n", i, e.getMessage());
                    }
                }
            }
            
            if (!byteConfirmed) {
                throw new IOException(String.format(
                    "TinyLoader: Failed to confirm byte %d (value %02X) after %d retries", 
                    i, dataByte, TINYLOADER_BYTE_RETRIES));
            }
        }
        
            System.out.printf("TinyLoader: Successfully sent %d bytes with running checksum verification\n", size);
            return true;
            
        } finally {
            // Always restore original echo checking setting
            if (originalEchoCheck) {
                System.out.println("TinyLoader: Restoring echo checking");
                setEchoCheck(true);
            }
        }
    }
    
    /**
     * Reset TinyLoader - simplified since completed transfers just return to main loop
     */
    private boolean resetAndVerifyTinyLoader() throws IOException {
        System.out.println("TinyLoader: Performing reset...");
        
        // Reset sends 0x00,0x00 which triggers S0 response from TinyLoader
        if (!resetTinyLoaderSimple()) {
            System.out.println("TinyLoader: Reset failed - no S0 response");
            return false;
        }
        
        // Clear any leftover data
        readBytes(); // Clear any remaining data
        
        System.out.println("TinyLoader: Reset successful - ready for transfer");
        return true;
    }
    
    /**
     * Aggressive TinyLoader reset - breaks out of stuck data loops
     */
    private boolean resetTinyLoaderSimple() throws IOException {
        System.out.println("TinyLoader: Performing aggressive reset...");
        
        // Clear any pending input data first
        readBytes();
        
        // If stuck in data loop, need to complete the current transfer first
        // Send up to 255 zero bytes to satisfy any pending byte counter
        System.out.println("TinyLoader: Clearing any stuck data transfer...");
        for (int i = 0; i < 255; i++) {
            writeOutput((byte)0x00);
            out.flush(); // Ensure each byte is sent immediately
            
            // Check if we get any response (checksum or S0) without delay
            if (in.available() > 0) {
                byte[] response = readBytes();
                System.out.printf("TinyLoader: Got response after %d zero bytes: ", i + 1);
                for (byte b : response) {
                    System.out.printf("%02X ", b & 0xFF);
                }
                System.out.println();
                
                // Check if we got S0 (TinyLoader reset)
                String responseStr = new String(response);
                if (responseStr.contains("S0")) {
                    System.out.println("TinyLoader: Reset successful via data completion");
                    return true;
                }
            }
        }
        
        // Now try the normal reset sequence
        readBytes(); // Clear any remaining data
        System.out.println("TinyLoader: Attempting address-based reset...");
        
        // Send target address 0000 to trigger reset with more aggressive flushing
        System.out.println("TinyLoader: Sending reset command (0000)...");
        writeOutput((byte)0x00);
        out.flush(); // Flush after each byte
        
        writeOutput((byte)0x00);
        out.flush(); // Flush after each byte
        
        // Wait for S0 response
        System.out.println("TinyLoader: Waiting for S0 response...");
        
        try {
            expect("S0", 3000, false);
            System.out.println("TinyLoader: Address-based reset successful");
            return true;
        } catch (IOException e) {
            System.err.println("TinyLoader: Address-based reset failed - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Write a single byte with precise timing delay
     */
    private void writeOutputWithDelay(byte b, int delayNanos) throws IOException {
        writeOutput(b);
        if (delayNanos > 0) {
            DataUtil.nanosleep(delayNanos);
        }
    }
    
    /**
     * Wait for a specific byte value with timeout (for echo verification)
     */
    private void expectByte(int expectedByte, String description) throws IOException {
        long timeoutStart = System.currentTimeMillis();  
        while (in.available() == 0 && (System.currentTimeMillis() - timeoutStart) < TINYLOADER_BYTE_TIMEOUT) {
            Thread.onSpinWait();
        }
        
        if (in.available() == 0) {
            throw new IOException("TinyLoader: Timeout waiting for " + description);
        }
        
        int receivedByte = in.read() & 0xFF;
        if (receivedByte != expectedByte) {
            throw new IOException(String.format("TinyLoader: %s mismatch: expected %02X, got %02X", 
                description, expectedByte, receivedByte));
        }
    }
    
    /**
     * Compute XOR checksum for TinyLoader protocol
     * CRITICAL: Use unsigned arithmetic to avoid Java signed byte issues
     */
    private byte computeChecksumTinyLoader(byte[] data, int start, int size) {
        int checksum = 0;
        for (int i = start; i < start + size; i++) {
            checksum ^= (data[i] & 0xFF);  // Convert to unsigned before XOR
        }
        return (byte)(checksum & 0xFF);  // Convert back to byte
    }
    
    
    /**
     * Initialize TinyLoader with welcome message on screen
     */
    public void initTinyLoaderWithWelcome() throws IOException {
        System.out.println("Loading TinyLoader and displaying welcome message...");
        
        // Create screen memory data (text page 1: $400-$7FF)
        byte[] screenData = new byte[0x400]; // 1024 bytes
        
        // Fill with spaces (Apple II text uses $A0 for space)
        for (int i = 0; i < screenData.length; i++) {
            screenData[i] = (byte)0xA0;
        }
        
        // Add welcome message in center of screen
        String[] message = {
            "APPLE GAME SERVER",
            "",
            "LOADING SYSTEM...",
            "",
            "TINYLOADER ACTIVE"
        };
        
        // Position message starting at row 10 (each row is 40 chars)
        int startRow = 10;
        for (int line = 0; line < message.length; line++) {
            String text = message[line];
            int startCol = (40 - text.length()) / 2; // Center text
            int offset = (startRow + line) * 40 + startCol;
            
            for (int i = 0; i < text.length(); i++) {
                // Convert ASCII to Apple II high-bit text
                screenData[offset + i] = (byte)(text.charAt(i) | 0x80);
            }
        }
        
        // Send screen data using TinyLoader protocol
        sendRawDataTinyLoader(screenData, 0x400, 0, screenData.length);
        
        System.out.println("TinyLoader welcome message displayed!");
    }
}