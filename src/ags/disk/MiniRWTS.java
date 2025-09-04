package ags.disk;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author brobert
 */
public class MiniRWTS extends RWTS {
    // MINI RWTS Constants

    public static int BASE = 0x0f1;
    public static int SLOTDRIVE = 0x0f1 - BASE;    // #sss##dd -- dd=0 == last drive
    public static int BUFFER = 0x0fb - BASE;       // 256-byte r/w page
    public static int SECTOR_COUNT = 0x0fd - BASE;
    public static int TRACK = 0x0fe - BASE;        // $0-$22
    public static int SECTOR = 0x0ff - BASE;       // $0-$f
    public int LAST_DRIVE = 0;
    public int LAST_ADDR = 0;
    public int LAST_UNIQUE_ADDR = 0;

    public MiniRWTS() throws IOException {
        super("minirwts", 0x0A5, 0x0F1, 0x029, 0x070, 0x0AA, 0x0A5);
    }

    @Override
    public CommandBlock parseCommandBlock(byte[] commandBlock) {
        if (commandBlock.length > 16) {
            return null;
        }
        int slot = (commandBlock[SLOTDRIVE] >> 4) - 1;
        int drive = commandBlock[SLOTDRIVE] & 0x0F;
        if (slot < 0 || slot > 6) {
            return null;
        }
        if (drive < 0 || drive > 2) {
            return null;
        }
        int ioBuffAddr = (0x0ff & commandBlock[BUFFER]) |
                (0x0FF00 & (commandBlock[BUFFER + 1] << 8));
        if (drive == 0) {
            System.out.println("Drive is 0, buffer is " + Integer.toString(ioBuffAddr, 16));
            drive = LAST_DRIVE;
        } else {
            drive = drive - 1;
        }
        if (ioBuffAddr == LAST_UNIQUE_ADDR) {
            ioBuffAddr = LAST_ADDR + 256;
            LAST_ADDR = ioBuffAddr;
        } else {
            LAST_UNIQUE_ADDR = ioBuffAddr;
            LAST_ADDR = ioBuffAddr;
        }
        int track = commandBlock[TRACK];
        int sector = commandBlock[SECTOR];
        int numSectors = commandBlock[SECTOR_COUNT];

        CommandBlock theCommand = new CommandBlock(this);
        theCommand.setSlot(slot);
        theCommand.setDrive(drive);
        theCommand.setTrack(track);
        theCommand.setSector(sector);
        theCommand.setBufferAddress(ioBuffAddr);
        theCommand.setVolume(255);
        theCommand.setSectorCount(numSectors);
        // MiniRWTS does not support writes
        theCommand.setCommand(Command.READ);
        System.out.println("MINIRWTS Read: S" + slot + ",D" + drive + ",T" + track + ",S" + sector + ",N" + numSectors);
        return theCommand;
    }

    @Override
    void advance(CommandBlock command) {
        int sector = command.getSector();
        int track = command.getTrack();
        int bufferAddress = command.getBufferAddress();
        sector--;
        if (sector < 0) {
            sector = 15;
            track--;
        }
        LAST_ADDR = bufferAddress;
        bufferAddress += 256;
        command.setSector(sector);
        command.setTrack(track);
        command.setBufferAddress(bufferAddress);
    }

    @Override
    void finishRWTS(Drive drive, CommandBlock command) {
        int bufferAddress = command.getBufferAddress() & 0x0ffff;
        int lo = (bufferAddress & 0x0ff);
        int hi = ((bufferAddress >> 8) & 0x0ff);
    /*
    System.out.println("Writing buffer address == "+Integer.toString(bufferAddress, 16));
    System.out.println("Writing buffer address < "+Integer.toString(lo, 16));
    System.out.println("Writing buffer address > "+Integer.toString(hi, 16));

    try {
    drive.storeMemory(BASE+BUFFER, lo, hi);
    } catch (IOException ex) {
    Logger.getLogger(MiniRWTS.class.getName()).log(Level.SEVERE, null, ex);
    }
     */
    }

    @Override
    public boolean isLogicalSector() {
        return true;
    }
}
