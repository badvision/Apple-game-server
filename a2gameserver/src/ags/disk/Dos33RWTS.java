package ags.disk;

import java.io.IOException;

/**
 *
 * @author brobert
 */
public class Dos33RWTS extends RWTS {
    // DOS 3.3 RWTS Constants

    public static int ADDR_COMMAND = 0x0c;      // Read/Write/Format/Seek
    public static int ADDR_SLOT = 0x01;         // Slot * 16 (e.g. $60)
    public static int ADDR_DRIVE_NO = 0x02;     // 1 or 2
    public static int ADDR_VOLUME = 0x03;       // 0 == any
    public static int ADDR_TRACK = 0x04;        // $0-$22
    public static int ADDR_SECTOR = 0x05;       // $0-$f
    public static int ADDR_BUFFER = 0x08;       // 256-byte r/w page
    public static int ADDR_RWTS_IOB_PTR = 0x048;

    // Commands sent from RWTS
    public static byte COMMAND_READ_SECTOR = 0x01;
    public static byte COMMAND_WRITE_SECTOR = 0x02;

    public Dos33RWTS() throws IOException {
        super("rwts", 0x084, 0x048, 0x085, 0x049);
    }

    @Override
    public CommandBlock parseCommandBlock(byte[] commandBlock) {
        if (commandBlock.length != 16) {
            return null;
        }
        int command = commandBlock[ADDR_COMMAND];
        if (command != COMMAND_READ_SECTOR && command != COMMAND_WRITE_SECTOR) {
            // This is not a command we understand -- ignore it!
            return null;
        }
        int slot = (commandBlock[ADDR_SLOT] >> 4) - 1;
        if (slot < 0 || slot > 6) return null;
        int drive = commandBlock[ADDR_DRIVE_NO] - 1;
        if (drive != 0 && drive != 1) return null;
        int track = commandBlock[ADDR_TRACK];
        if (track <0 || track > 35) return null;
        int sector = commandBlock[ADDR_SECTOR];
        if (sector < 0 || sector > 15) return null;
        // handy for debugging ...
        //if (track == 25 && sector == 8)
            //System.out.println("Um, T25,S8!");
        int ioBuffAddr = (0x0ff & commandBlock[ADDR_BUFFER]) |
                (0x0FF00 & (commandBlock[ADDR_BUFFER + 1] << 8));
        CommandBlock theCommand = new CommandBlock(this);
        theCommand.setSlot(slot);
        theCommand.setDrive(drive);
        theCommand.setTrack(track);
        theCommand.setSector(sector);
        theCommand.setBufferAddress(ioBuffAddr);
        theCommand.setVolume(255);
        if (command == COMMAND_READ_SECTOR) {
            theCommand.setCommand(Command.READ);
        }
        if (command == COMMAND_WRITE_SECTOR) {
            theCommand.setCommand(Command.WRITE);
        }
        System.out.println(theCommand.getCommand() + ": S" + slot + ",D" + drive + ",T" + track + ",S" + sector);
        return theCommand;
    }

    @Override
    void advance(CommandBlock command) {
        // do nothing -- not applicable to dos 3.3
    }

    @Override
    void finishRWTS(Drive aThis, CommandBlock command) {
        // Maybe update IOB with result info?
    }
}