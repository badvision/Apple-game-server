package ags.disk;

import ags.script.Target;
import ags.script.Variable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 *
 * @author brobert
 */
public abstract class RWTS {

    public enum Command {
        READ,
        WRITE,
        FORMAT
    };

    public class CommandBlock {
        private int slot;
        private int drive;
        private Command command;
        private int sector;
        private int track;
        private int volume;
        private int bufferAddress;
        private int sectorCount = 1;
        private RWTS rwts;

        public CommandBlock(RWTS r) {
            rwts = r;
        }

        /**
         * @return the slot
         */
        public int getSlot() {
            return slot;
        }

        /**
         * @param slot the slot to set
         */
        public void setSlot(int slot) {
            this.slot = slot;
        }

        /**
         * @return the drive
         */
        public int getDrive() {
            return drive;
        }

        /**
         * @param drive the drive to set
         */
        public void setDrive(int drive) {
            this.drive = drive;
        }

        /**
         * @return the command
         */
        public Command getCommand() {
            return command;
        }

        /**
         * @param command the command to set
         */
        public void setCommand(Command command) {
            this.command = command;
        }

        /**
         * @return the sector
         */
        public int getSector() {
            return sector;
        }

        /**
         * @param sector the sector to set
         */
        public void setSector(int sector) {
            this.sector = sector;
        }

        /**
         * @return the track
         */
        public int getTrack() {
            return track;
        }

        /**
         * @param track the track to set
         */
        public void setTrack(int track) {
            this.track = track;
        }

        /**
         * @return the volume
         */
        public int getVolume() {
            return volume;
        }

        /**
         * @param volume the volume to set
         */
        public void setVolume(int volume) {
            this.volume = volume;
        }

        /**
         * @return the bufferAddress
         */
        public int getBufferAddress() {
            return bufferAddress;
        }

        /**
         * @param bufferAddress the bufferAddress to set
         */
        public void setBufferAddress(int bufferAddress) {
            this.bufferAddress = bufferAddress;
        }

        /**
         * @return the sectorCount
         */
        public int getSectorCount() {
            return sectorCount;
        }

        /**
         * @param sectorCount the sectorCount to set
         */
        public void setSectorCount(int sectorCount) {
            this.sectorCount = sectorCount;
        }

        public void next() {
            sectorCount--;
            rwts.advance(this);
        }

        public boolean isLogicalSector() {
            return rwts.isLogicalSector();
        }

        void finishRWTS(Drive aThis) {
            rwts.finishRWTS(aThis, this);
        }
    }
    
    private String RWTS_REPLACEMENT;
    private byte[] RWTS_SECTOR;
    private int RWTS_SIZE;
    private int[] signature;
    protected Disk disk;
    public RWTS() {}
    
    public RWTS(String driverName, int... signature) throws IOException {
        readRWTS(driverName);
        this.signature = signature;
    }

    private void readRWTS(String driverName) throws IOException {
            Target gs = Target.getTarget("apple2gs_setup");
            String slot = Variable.getVariable("slot").getValue();
            if (gs != null && gs.isRunAlready()) {
                RWTS_REPLACEMENT = "ags/asm/"+driverName+"_gs_port" + slot + ".o";
            } else {
                RWTS_REPLACEMENT = "ags/asm/"+driverName+"_ssc_slot" + slot + ".o";
            }
            InputStream data = ClassLoader.getSystemResourceAsStream(RWTS_REPLACEMENT);
            if (data == null) {
                // Try alternative resource loading methods
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                if (cl != null) {
                    data = cl.getResourceAsStream(RWTS_REPLACEMENT);
                }
                if (data == null) {
                    data = RWTS.class.getClassLoader().getResourceAsStream(RWTS_REPLACEMENT);
                }
                if (data == null) {
                    data = RWTS.class.getResourceAsStream("/" + RWTS_REPLACEMENT);
                }
                if (data == null) {
                    throw new IOException("RWTS driver not found: " + RWTS_REPLACEMENT + 
                        ". Available drivers may not include slot " + slot + " for driver " + driverName);
                }
            }
            RWTS_SECTOR = new byte[data.available()];
            RWTS_SIZE = data.read(RWTS_SECTOR);
            System.out.println("Using RWTS "+RWTS_REPLACEMENT+", length="+RWTS_SIZE+" bytes");
            data.close();
    }

    /**
     * Does the sector match the RWTS being emulated?
     * @param sector
     * @return true if the sector looks like the RWTS being emulated
     */
    public boolean matches(byte[] sector) {
        for (int i=0; i < signature.length; i++) {
            if ((byte) signature[i] != sector[i]) return false;
        }
        return true;
    }

    public byte[] getReloactedRWTS(int targetAddress, byte[] sector) {
        int targetLo = 0x0ff & targetAddress;
        int targetHi = 0x0FF & (targetAddress >> 8);
        // Start with a copy of the original sector
        byte[] rwts = new byte[256];
        for (int i=0; i < 256 && i < sector.length; i++) {
            rwts[i] = sector[i];
        }
        System.out.println("Relocating RWTS to "+Integer.toHexString(targetAddress));
        // Overwrite the sector up to the relocation data stub
        for (int i=0; i < RWTS_SIZE - RWTS_SECTOR[RWTS_SIZE-1]; i++) {
            rwts[i] = RWTS_SECTOR[i];
        }
        // Perform relocation from addresses based on $1000 originally
        for (int i=0; i < RWTS_SECTOR[RWTS_SIZE-1]; i++) {
            int pos = 0x0ff & RWTS_SECTOR[RWTS_SIZE-2-i];
            System.out.println("Offset "+Integer.toHexString(pos));
            rwts[pos] += targetLo;
            rwts[pos+1] += targetHi - 0x10;
        }
        return rwts; // TODO: strip off the relocation data
    }

    /**
     * Parse raw IOB (or similar) into CommandBlock structure
     * @param command Raw bytes from IOB
     * @return Parsed command if valid, otherwise null if not suppoted RWTS variation
     */
    public abstract CommandBlock parseCommandBlock(byte[] command);
    /**
     * Advance to next sector to read
     * used for RWTS variants that load multiple sectors in a loop
     * @param command Command Block containing information
     */
    abstract void advance(CommandBlock command);

    abstract void finishRWTS(Drive aThis, CommandBlock command);

    protected boolean isLogicalSector() {
        return true;
    }

}