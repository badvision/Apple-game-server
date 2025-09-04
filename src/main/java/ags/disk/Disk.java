/*
 * Disk.java
 *
 * Created on May 31, 2006, 9:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.disk;

import ags.disk.RWTS.CommandBlock;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Disk image base class, can be subclassed to support different sector ordering variants
 * @author blurry
 */
public abstract class Disk {
    
    /**
     * Number of tracks per disk (35)
     */
   static final public int TRACK_COUNT = 35;
    /**
     * Number of sectors per track (16)
     */
   static final public int SECTOR_COUNT = 16;
    /**
     * Image of raw disk data
     */
    byte[][][] diskImage = new byte[TRACK_COUNT][SECTOR_COUNT][256];

    private List<RWTS> rwts = new ArrayList<RWTS>();
    
    /**
     * Creates a new instance of Disk
     * @param file File path of disk image to load
     * @throws java.io.IOException If the disk image could not be read
     */
    public Disk(InputStream file) throws IOException {
//        System.out.println("Opening disk: "+file);
        openDiskImage(file);
//        System.out.println("Successful!");
    }

    public Disk(String filename) throws IOException {
        File f = new File(filename);
        if (f.exists()) {
            openDiskImage(new FileInputStream(f));
        } else {
            InputStream i = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
            if (i != null) {
                openDiskImage(i);
            } else {
                i = ClassLoader.getSystemResourceAsStream(filename);
                if (i != null) {
                    openDiskImage(i);
                } else {
                    System.out.println("Could not find file "+filename);
                }
            }
        }
    }

    public void registerRWTS(RWTS r) {
        rwts.add(r);
        r.disk = this;
    }

    public CommandBlock parseCommand(byte[] data) {
        for (RWTS r : rwts) {
            CommandBlock c = r.parseCommandBlock(data);
            if (c != null) return c;
        }
        return null;
    }

    /**
     * Open disk image and read into memory
     * @param file File path of disk image to read
     * @throws java.io.IOException If disk image could not be read
     */
    private void openDiskImage(InputStream file) throws IOException {
        int counter = 0;
        while (file.available() > 0) {
            byte[] sector = new byte[256];
            int read = file.read(sector);
            if (read < 256) System.out.println("Read less than 256 bytes for last sector! (got "+read+" instead)");
            int sectorNumber = counter % SECTOR_COUNT;
            int trackNumber = counter / SECTOR_COUNT;
            setSector(trackNumber, sectorNumber, sector);
            counter++;
        }
    }
    
    /**
     * Convert physical sector order (in file) to logical sector order (in memory)
     * @param sectorNumber Physical sector number
     * @return Locical sector number
     */
    protected abstract int translatePhysicalSectorNumber(int sectorNumber);
    /**
     * Convert logical sector order to physical sector order
     * @param sectorNumber Logical sector number
     * @return Physical sector number
     */
    protected abstract int translateLogicalSectorNumber(int sectorNumber);
    protected byte[] getRWTSReplacement(byte[] sector, int target) {
        for (RWTS r:rwts) {
            if (r.matches(sector))
                return r.getReloactedRWTS(target, sector);
        }
        return sector;
    }

    /**
     * Get 256-byte sector data
     * @param trackNumber Track number to read
     * @param sectorNumber Sector number to read
     * @param target Targer address where sector will be sent (for relocating code)
     * @return 256-byte array
     */
    public byte[] getSector(int trackNumber, int sectorNumber, int target) {
        byte[] sector = diskImage[trackNumber][sectorNumber];
        sector = getRWTSReplacement(sector, target);
        return sector;
    }
    
    /**
     * Read sector into a buffer
     * @param trackNumber Track number to read
     * @param sectorNumber Sector number to read
     * @param sector Buffer to read data into
     */
    public void setSector(int trackNumber, int sectorNumber, byte[] sector) {
        diskImage[trackNumber][sectorNumber] = sector;
    }

    //-------------------------------------------------------------------------------------
    // Standard 62 nybblize scheme
    // NOTE: Assumes PHYSICAL sector numbering!
    // Code borrowed from DISK II class of Jace
    public byte[] getNybblizedSector(int track, int sector) throws IOException {
           ByteArrayOutputStream output = new ByteArrayOutputStream();
               // 15 junk bytes
               writeJunkBytes(output, 15);
               // Address block
               writeAddressBlock(output, track, sector);
               // 4 junk bytes
               writeJunkBytes(output, 4);
               // Data block
               writeDataBlock(output, track, sector, getSector(track, sector, 0));
               // 34 junk bytes
               writeJunkBytes(output, 34);
       return output.toByteArray();
   }

   private void writeJunkBytes(ByteArrayOutputStream output, int i) {
       for (int b = 0; b < i; b ++) output.write(0x0FF);
   }

   int VOLUME_NUMBER = 0x0FE;
   private void writeAddressBlock(ByteArrayOutputStream output, int track, int sector) throws IOException {
       output.write(0x0d5);
       output.write(0x0aa);
       output.write(0x096);
       int checksum = 00;
       // volume
       checksum ^= VOLUME_NUMBER;
       output.write(getOddEven(VOLUME_NUMBER));
       // track
       checksum ^= track;
       output.write(getOddEven(track));
       // sector
       checksum ^= sector;
       output.write(getOddEven(sector));
       // checksum
       output.write(getOddEven(checksum&0x0ff));
       output.write(0x0de);
       output.write(0x0aa);
       output.write(0x0eb);
   }
   private byte[] getOddEven(int i) {
       byte[] out = new byte[2];
       out[0] = (byte) (0xAA | ( i >> 1 ));
       out[1] = (byte) (0xAA | i);
       return out;
   }

   int[] NIBBLE_62 = {
       0x96,0x97,0x9a,0x9b,0x9d,0x9e,0x9f,0xa6,
       0xa7,0xab,0xac,0xad,0xae,0xaf,0xb2,0xb3,
       0xb4,0xb5,0xb6,0xb7,0xb9,0xba,0xbb,0xbc,
       0xbd,0xbe,0xbf,0xcb,0xcd,0xce,0xcf,0xd3,
       0xd6,0xd7,0xd9,0xda,0xdb,0xdc,0xdd,0xde,
       0xdf,0xe5,0xe6,0xe7,0xe9,0xea,0xeb,0xec,
       0xed,0xee,0xef,0xf2,0xf3,0xf4,0xf5,0xf6,
       0xf7,0xf9,0xfa,0xfb,0xfc,0xfd,0xfe,0xff};

   private void writeDataBlock(ByteArrayOutputStream output, int track, int sector, byte[] nibbles) {
       int[] temp = new int[342];
       for (int i=0; i < 256; i++)
           temp[i] = (nibbles[i] & 0x0ff) >> 2;
       int hi = 0x001;
       int med = 0x0AB;
       int low = 0x055;
       for (int i=0; i < 0x56; i++) {
           int value = ((nibbles[hi] & 1) << 5) |
                       ((nibbles[hi] & 2) << 3) |
                       ((nibbles[med] & 1) << 3) |
                       ((nibbles[med] & 2) << 1) |
                       ((nibbles[low] & 1) << 1) |
                       ((nibbles[low] & 2) >> 1);
           temp[i+256] = value;
           hi = (hi - 1) & 0x0ff;
           med = (med - 1) & 0x0ff;
           low = (low - 1) & 0x0ff;
       }
       output.write(0x0d5);
       output.write(0x0aa);
       output.write(0x0ad);
       int last = 0;
       for (int i=temp.length-1; i > 255; i--) {
           int value = temp[i] ^ last;
           output.write(NIBBLE_62[value]);
           last = temp[i];
       }
       for (int i=0; i < 256; i++) {
           int value = temp[i] ^ last;
           output.write(NIBBLE_62[value]);
           last = temp[i];
       }
       // Last data byte used as checksum
       output.write(NIBBLE_62[last]);
       output.write(0x0de);
       output.write(0x0aa);
       output.write(0x0eb);
   }
}