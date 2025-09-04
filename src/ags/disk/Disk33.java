/*
 * Disk33.java
 *
 * Created on May 31, 2006, 9:16 PM
 */
package ags.disk;

import java.io.IOException;
import java.io.InputStream;

/**
 * Derivation of a disk image with DOS 3.3 sector ordering
 * @author blurry
 */
public class Disk33 extends Disk {

    /**
     * Order of sectors for this format -- Logical-to-physical mapping
     * index is logical DOS 3.3 sector #
     * value is physical disk sector #
     */
    static int[] LOGICAL_SECTOR_ORDER = {
        0x00, 0x0D, 0x0b, 0x09, 0x07, 0x05, 0x03, 0x01,
        0x0e, 0x0c, 0x0a, 0x08, 0x06, 0x04, 0x02, 0x0f
    };
    /**
     * Order of sectors for this format -- Physical-to-logical mapping
     * index is physical disk sector #
     * value is logical DOS 3.3 sector #
     */
    static int[] PHYSICAL_SECTOR_ORDER = {
        0x00, 0x07, 0x0E, 0x06, 0x0D, 0x05, 0x0C, 0x04,
        0x0B, 0x03, 0x0A, 0x02, 0x09, 0x01, 0x08, 0x0F
    };

    /**
     * Constructor (read disk image)
     * @param file Path of disk image
     * @throws java.io.IOException If disk image could not be read
     */
    public Disk33(InputStream file) throws IOException {
        super(file);
        setupRWTS();
    }

    public Disk33(String filename) throws IOException {
        super(filename);
        setupRWTS();
    }

    public void setupRWTS() throws IOException {
        registerRWTS(new Dos33RWTS());
        registerRWTS(new MiniRWTS());
        registerRWTS(new C6RWTS());
    }

    /**
     * Translate plain disk image sector ordering
     * @param sectorNumber Physical (disk image) sector number
     * @return Logical (memory) sector number
     */
    protected int translatePhysicalSectorNumber(int sectorNumber) {
        if (sectorNumber < 0 || sectorNumber >= SECTOR_COUNT) {
            System.out.println("Sector " + sectorNumber + " is out of range!");
            return 0;
        }
        return PHYSICAL_SECTOR_ORDER[sectorNumber];
    }

    /**
     * Translate plain disk image sector ordering
     * @param sectorNumber Logicak sector number
     * @return Physical sector number
     */
    protected int translateLogicalSectorNumber(int sectorNumber) {
        if (sectorNumber < 0 || sectorNumber >= SECTOR_COUNT) {
            System.out.println("Sector " + sectorNumber + " is out of range!");
            return 0;
        }
        return LOGICAL_SECTOR_ORDER[sectorNumber];
    }

    protected boolean isBootable(byte[] sector0) {
        boolean isBootable = true;
        // Always one!
        isBootable &=
                sector0[0] == 0x01;
        // The command that calls the disk ][ firmware sector read subroutine
        isBootable &=
                sector0[0x0036] == 0x6c;
        isBootable &=
                sector0[0x0037] == 0x3e;
        isBootable &=
                sector0[0x0038] == 0x00;
        return isBootable;
    }
}