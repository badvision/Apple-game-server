package ags.disk;

import java.io.IOException;

/**
 *
 * @author brobert
 */
public class C6RWTS extends RWTS {

    // C6 Rom RWTS Constants
    public static int BASE = 0x026;
    public static int ADDR_BUFFER = 0x026 - BASE;       // 256-byte r/w page
    public static int ADDR_SECTOR = 0x03D - BASE; // $0-$f
    public static int ADDR_TRACK = 0x041 - BASE;  // $0-$22

    public static int USE_SLOT = 5;
    public static int USE_DRIVE = 0;

    public C6RWTS() throws IOException {
        super("c6rwts", 0x0a2, 0x020, 0x0a0, 0x00 ,0x0a2, 0x003, 0x086, 0x03c);
    }
    
    @Override
    public CommandBlock parseCommandBlock(byte[] commandBlock) {
        if (commandBlock.length != ADDR_TRACK+1) return null;
            int slot = USE_SLOT;
            int drive = USE_DRIVE;
            int track = commandBlock[ADDR_TRACK];
            int sector = commandBlock[ADDR_SECTOR];
            int ioBuffAddr = (0x0ff & commandBlock[ADDR_BUFFER]) |
                             (0x0FF00 & (commandBlock[ADDR_BUFFER+1] << 8));
            CommandBlock theCommand = new CommandBlock(this);
            theCommand.setSlot(slot);
            theCommand.setDrive(drive);
            theCommand.setTrack(track);
            theCommand.setSector(disk.translatePhysicalSectorNumber(sector));
            theCommand.setBufferAddress(ioBuffAddr);
            theCommand.setVolume(255);
            theCommand.setCommand(Command.READ);
            System.out.println("C6 Read: S"+slot+",D"+drive+",T"+track+",S"+sector);
            return theCommand;
    }

    @Override
    void advance(CommandBlock command) {
        // Do nothing (block reads are only used in Boot0 phase)
    }

    @Override
    void finishRWTS(Drive aThis, CommandBlock command) {
        // Do nothing (maybe update IOB with incremented addresss?)
    }

    @Override
    public boolean isLogicalSector() {
        return false;
    }
}