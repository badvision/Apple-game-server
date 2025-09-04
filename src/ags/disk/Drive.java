package ags.disk;

import ags.communication.*;
import ags.controller.Launcher;
import ags.disk.RWTS.Command;
import ags.disk.RWTS.CommandBlock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles disk operations
 * @author brobert
 */
public class Drive {
    // Commands understood by RWTS replacement drivers

    public static byte COMMAND_SET_ADDRESS = 0x00;
    public static byte COMMAND_SET_LENGTH = 0x01;
    public static byte COMMAND_STORE = 0x02;
    public static byte COMMAND_READBLOCK = 0x03;
    public static byte COMMAND_END = 0x04;
    TransferHost apple;

    public Drive(TransferHost t) {
        apple = t;
    }
    Disk[][] disk = new Disk[7][2];
    Disk bootDisk;

    public void insertDisk(int slot, int drive, Disk newdisk) {
        disk[slot][drive] = newdisk;
    }

    public void ejectDisk(int slot, int drive) {
        disk[slot][drive] = null;
    }

    public void boot() throws IOException {
        boot(5, 0);      // Boot slot 6, drive 1 (very standard!)
    }

    public void boot(int slot, int drive) throws IOException {
        bootDisk = disk[slot][drive];
        C6RWTS.USE_SLOT = slot;
        C6RWTS.USE_DRIVE = drive;
        // Load C6 RWTS replacement logic
        apple.loadDriver("c6rwts", 0x025c);

        // Emulate Boot Stage 0 code by uploading data for Stage 1 directly
        byte[] sector0 = bootDisk.getSector(0, 0, 0x0800);
        int numSectors = sector0[0];
        int address = 0x0800;
        int sector = 0;
        for (int i = 0; i < numSectors; i++) {
            System.out.println("Sending track 0, sector " + sector + " to address $" + Integer.toHexString(address));
            int sectorNum = bootDisk.translatePhysicalSectorNumber(sector++);
//            sectorNum = bootDisk.translateLogicalSectorNumber(sectorNum);
            // If this is done right, RWTS goes to BD00
            byte[] sectorData = bootDisk.getSector(0, sectorNum, address);
            apple.sendRawData(hackBootSector(sectorData), address, 0, 256);
            address += 256;
        }
        System.out.println("Cold start dos (or die trying anyway)");
        byte[] executeCode = {
            //(LDX #(slot *16)
            (byte) 0x0a2, (byte) ((slot + 1) << 4),
            // STX $2B
            (byte) 0x086, (byte) 0x2b,
            // LDA #sectors + 8
            (byte) 0x0a9, (byte) (0x08 + numSectors),
            // STA $27
            (byte) 0x085, (byte) 0x027,
            // LDA #01
            (byte) 0x0a9, (byte) 0x01,
            // STA $3D
            (byte) 0x085, (byte) 0x03D,
            // LDA #00
            (byte) 0x0a9, (byte) 0x00,
            // STA $26
            (byte) 0x085, (byte) 0x026,
            // STA $41
            (byte) 0x085, (byte) 0x041,
            //JMP $801
            (byte) 0x04c, (byte) 0x01, (byte) 0x08
        };
        apple.sendRawData(executeCode, 0x0330, 0, executeCode.length);
        apple.jmp(0x0330, false);
        mainLoop();
    }

    public void mainLoop() throws IOException {
        while (Thread.currentThread().isAlive()) {
            byte[] commandBlock = waitForCommand();

            System.out.println("Got command with " + commandBlock.length + " bytes");
            CommandBlock command = bootDisk.parseCommand(commandBlock);
            if (command == null) {
                System.out.println("Could not identify how to handle this request!");
                continue;
            }
            if (command.getCommand() == Command.READ) {
                while (command.getSectorCount() > 0) {
                    Disk d = disk[command.getSlot()][command.getDrive()];
                    int sector = command.getSector();
                    if (!command.isLogicalSector()) {
                        sector = d.translatePhysicalSectorNumber(sector);
                    }
                    storeMemory(command.getBufferAddress(),
                            d.getSector(command.getTrack(), command.getSector(), command.getBufferAddress()));
                    command.next();
                }
            } else if (command.getCommand() == Command.WRITE) {
                byte[] newSector = readData(command.getBufferAddress(), 256);
                disk[command.getSlot()][command.getDrive()].setSector(command.getTrack(), command.getSector(), newSector);
            // TODO: Save to physical disk?
            }
            command.finishRWTS(this);
            finishRWTS();

            continue;
        }
    }

    //------------------- Basic operations that work with RWTS Driver directly
    // Wait for apple RWTS loop to start again
    public byte[] waitForCommand() throws IOException {
        while (apple.inputAvailable() == 0) {
            try {
                Thread.sleep(100);
                Launcher.checkRuntimeStatus();
            } catch (InterruptedException ex) {
                Logger.getLogger(Drive.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        int lastCheck = apple.inputAvailable();
        boolean gotMore = true;
        // Wait for full command to fill buffer...
        while (gotMore) {
            LockSupport.parkNanos(DataUtil.NANOS_PER_CHAR * 2);
            int check = apple.inputAvailable();
            if (check == lastCheck) {
                gotMore = false;
                lastCheck = check;
            }
        }
        return read();
    }

    // Tell apple RWTS loop that we're done
    public void finishRWTS() throws IOException {
        apple.writeOutput(COMMAND_END);
    }

    public byte[] readData(int address, int length) throws IOException {
        System.out.println("Reading " + length + " bytes from $" + Integer.toHexString(address));
        // Set Address
        setAddress(address);

        setLength(length);

        // Clear out input buffer
        apple.readBytes();

        // Get Byte
        apple.writeOutput(COMMAND_READBLOCK);
        return read();
    }

    public byte[] read() throws IOException {
        int timeout = 200;
        int pos = 0;
        int length = 256;
        List<Byte> buffer = new ArrayList<Byte>();

        while (buffer.size() < length && timeout > 0) {
            while (apple.inputAvailable() == 0 && timeout > 0) {
                DataUtil.wait(20);
                timeout -= 20;
            }
            if (timeout != 0) {
                timeout = 40;
            }

            if (apple.inputAvailable() > 0) {
                byte[] b = apple.readBytes();
                for (byte B : b) {
                    buffer.add(B);
                }
            }
        }
        if (buffer.size() > 0) {
            byte[] output = new byte[buffer.size()];
            for (int i = 0; i < buffer.size(); i++) {
                output[i] = buffer.get(i);
            }
            return output;
        } else {
            return null;
        }
    }

    /**
     * Send a chunk of raw binary data directly to the apple's ram
     * @param fileData Data to send
     * @param addressStart Starting address in apple's ram to load data
     * @param dataStart Starting offset in data to send
     * @param length Length of data to send over
     * @throws java.io.IOException If there was trouble sending data after a number of attempts
     */
    public void sendRawData(byte[] fileData, int addressStart, int dataStart, int length) throws IOException {
        int offset = dataStart;
        int end = dataStart + length;
        while (offset < end) {
            setAddress(offset + addressStart);
            // Set size so that it won't exceed the remainder of the data
            int size = Math.min(256, end - offset);
//            System.out.println("sending offset: " + offset + ", length=" + size);
            // Set size
            setLength(size);
            // Send data chunk
            apple.writeOutput(COMMAND_STORE);
            apple.writeOutput(fileData, offset, size);
            offset += size;
        }
    }

    private byte[] hackBootSector(byte[] sectorData) {
        // Replace:
        //     lsr, lsr, lsr, lsr, ora #$20
        // With:
        //     lsr, lsr, lsr, lsr, lda #$02
        // This forces an execute to our routine at $25c
        int[] search = new int[]{
            0x04a, 0x04a, 0x04a, 0x04a, 0x09, 0x0c0
        };
        int[] replace = new int[]{
            0x04a, 0x04a, 0x04a, 0x04a, 0x0a9, 0x002
        };
        int found = 0;
        int match = 0;
        for (int i = 0; i < sectorData.length && found < search.length; i++) {
            if (sectorData[i] == (byte) search[found]) {
                found++;
            } else {
                found = 0;
                match = i + 1;
            }
        }
        if (found == search.length) {
            for (int i = 0; i < replace.length; i++) {
                sectorData[i + match] = (byte) replace[i];
            }
        }
        return sectorData;
    }

    private void setAddress(int address) throws IOException {
        // Set start address
        apple.writeOutput(COMMAND_SET_ADDRESS);
        apple.writeOutput(DataUtil.getWord(address));
    }

    private void setLength(int length) throws IOException {
        apple.writeOutput(COMMAND_SET_LENGTH);
        // Add 1 to hi and lo bytes after subtracting one from the total size
        // This was done here to reduce the SOS driver size by 4 bytes
        int useSize = (0x0ff00 & (length + 255)) | (0x0FF & (length));
        apple.writeOutput(DataUtil.getWord(useSize));
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
            b[x] = (byte) (i[x] * 0x00ff);
        }
        storeMemory(address, b);
    }

    private void switchToLomemSOS() throws IOException {
        apple.loadDriver("sos_lo", 0x0800);
        apple.jmp(0x0801, false);
    }
}