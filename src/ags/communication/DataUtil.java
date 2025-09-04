/*
 * DataUtil.java
 *
 * Created on June 14, 2006, 9:19 PM
 */
package ags.communication;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collection of miscellaneous text formatting and other routines used by the host utilities.
 * @author Administrator
 */
public class DataUtil {

    public static long CPU_SPEED = 1020500L;
    public static long NANOS_PER_SECOND = 1000000000L;
    // How long it takes to send 1 character at 115200 baud in nanoseconds
    public static long NANOS_PER_CHAR = nanosPerCharAtSpeed(115200L);

    static long nanosPerCharAtSpeed(long speed) {
        long result = (NANOS_PER_SECOND * 9L) / speed;
//        System.out.println("calculate "+result+" nanos per character at "+speed+" baud");
        return result;
    }

    static long cyclesToNanos(long cycles) {
        return (NANOS_PER_SECOND * cycles) / CPU_SPEED;

    }

    static void nanosleep(long duration) {
        if (duration > 0) {
            LockSupport.parkNanos(duration);
        }
    }

    /** Creates a new instance of DataUtil */
    private DataUtil() {
    }

    /**
     * Strip off apple high-order text
     * @param in text to strip
     * @return normalized ascii text (hopefully)
     */
    public static String convertFromAppleText(String in) {
        if (in == null) {
            return "";
        }
        StringBuffer out = new StringBuffer();
        for (char c : in.toCharArray()) {
            out.append((char) (c & 0x7f));
        }
        return out.toString();
    }

    /**
     * Convert an array of bytes (assuming 8-bit ascii) to a string
     * @param data array of bytes to convert
     * @return new string
     */
    public static String bytesToString(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuffer out = new StringBuffer();
        for (byte b : data) {
            out.append((char) b);
        }
        return out.toString();
    }

    /**
     * Convert a string of data to hexidecimal equivilants.  Useful for writing text directly to the screen.
     * @param in text
     * @return string of hex digits ready to be typed to the apple
     */
    public static String asAppleScreenHex(String in) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c >= ' ') {
                out.append(Integer.toHexString(c | 0x80)).append(' ');
            }
        }

        return out.toString();
    }

    /**
     * Given an int, break it down to a little-endian word (byte array format)
     * @param i int to convert
     * @return little endian byte array
     */
    public static byte[] getWord(int i) {
        byte word[] = new byte[2];
        word[0] = (byte) (i % 256);
        word[1] = (byte) (i / 256);
        return word;
    }

    /**
     * Open a file and get its contents as one big string
     * @param file name of file to open
     * @return The file contents as a string
     */
    public static String getFileAsString(String file) {
        InputStream stream = DataUtil.class.getResourceAsStream(file);
        StringBuffer data = new StringBuffer();
        byte buf[] = new byte[256];
        int read = 0;
        try {
            while ((read = stream.read(buf)) > 0) {
                String s = new String(buf, 0, read);
                data.append(s);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return data.toString();
    }

    /**
     * Open a file and get its contents as one big byte array
     * @param file name of file to open
     * @throws java.io.IOException if the file or com port cannot be accessed
     * @return byte array containing file's contents
     */
    public static byte[] getFileAsBytes(String file) throws IOException {
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
        if (stream == null) {
            throw new IOException("Unable to find file "+file);
        }
        int size = 0;
        try {
            size = stream.available();
        } catch (Throwable ex) {
            System.out.println("Error reading file " + file);
            Logger.getLogger(DataUtil.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException("Error reading " + file);
        }
        ByteBuffer bb = ByteBuffer.allocate(size);
        byte[] buf = new byte[256];
        int read = 0;
        try {
            while ((read = stream.read(buf)) > 0) {
                bb.put(buf, 0, read);
            }
        } catch (IOException ex) {
            Logger.getLogger(DataUtil.class.getName()).log(Level.WARNING, null, ex);
        }
        System.out.println("Read file " + file);
        return bb.array();
    }

    /**
     * Does this byte buffer contain the same values as the array of bytes?
     * @param bb buffer to check
     * @param data bytes to look for
     * @return true if bytes were found in the buffer
     */
    public static boolean bufferContains(ByteBuffer bb, byte data[]) {
        int d = 0;
        boolean match = false;
        for (int i = 0; i < bb.position() && d < data.length; i++) {
            if (bb.get(i) == data[d]) {
                match = true;
                d++;
            } else {
                match = false;
                d = 0;
            }
        }

        return match;
    }

    /**
     * Sleep the current thread for a little while
     * @param time time in ms to wait
     */
    public static void wait(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    // Starts off as True because assembly code starts off with EOR
    public static boolean XOR_MODE = true;

    // TC7
    private static boolean isInScreenhole(int i) {
        // There are 64 locations unused in text modes
        // There are 512 locations unused in hires modes
        // Screenholes appear in each bank from x78 thru x7f and xf8 thru xff
        return ((i & 0x07f) >= 0x078);
    }

    private static int countReps(byte[] data, int baseAddress, int offset) {
        if (offset >= data.length - 4) {
            return -1;
        }
        // Just in case we start this within a screenhole, which might not be as efficient!
        int start = offset;
// TC8 : Scan ahead to take pattern from end of screenhole
// Results in a marginal compression increase, but introduces errors (?)
//        while (isInScreenhole(baseAddress + start) && start < data.length-4) {
//            start += 2;
//        }
        byte b1 = data[start];
        byte b2 = data[start + 1];
        int numberReps = -1;
        int seek = offset+2;
        while (numberReps < 127 && seek < data.length-2) {
            if ((b1 == data[seek] || isInScreenhole(baseAddress + seek))
            &&  (b2 == data[seek+1] || isInScreenhole(baseAddress + seek+1))) {
                seek += 2;
                numberReps++;
            } else {
                break;
            }
        }
        return numberReps;
    }

    /**
     * Packbits compression scheme:
     * first two bytes = base address (little endian format)
     * Repetitions of:
     *   Length, Data 1, Data 2 ... Data (length)
     *   Where 00-7B = Length of uncompressed data (copy Data 1, Data 2... as is)
     *   and 80-FF = Length of compressed data (length - 0x7E) repetitions of Data 1 and Data 2
     *   7E = Switch to XOR (for large areas with no changes)
     *   7F = Switch to write mode (for large areas of same color/pattern)
     * Until end:
     * 00 = end;
     * @param baseAddress destination base address
     * @param xorFrame Data xor'd against previous frame, should be same size as newFrame (or null, if not available)
     * @param newFrame New frame to store
     * @return packed data
     */
    public static byte[] packbits(int baseAddress, byte[] xorFrame, byte[] newFrame) {
        boolean XOR_ALLOWED = true;
        byte[] data = XOR_MODE ? xorFrame : newFrame;

        List<Byte> out = new ArrayList<Byte>();
        out.add((byte) (0x0ff & baseAddress));
        out.add((byte) ((0x0ff00 & baseAddress) >> 8));
        if (xorFrame == null) {
            // No previous frame to work against, disable XOR support for this frame
            XOR_ALLOWED = false;
            // Flip mode to write mode
            out.add((byte) 0x07F);
            XOR_MODE = false;
            data = newFrame;
        }
        int offset = 0;
        while (offset < newFrame.length) {
            boolean rawData = true;
            // Pick mode...
            int xcount = (XOR_ALLOWED ? countReps(xorFrame, baseAddress, offset) : -1);
            int ccount = countReps(newFrame, baseAddress, offset);
            if (xcount > -1 || ccount > -1) {
                rawData = false;        // We are writing out something compressed...
                int numberReps = 0;
                if (ccount > xcount || (ccount == xcount && !XOR_MODE)) {
                    if (XOR_MODE) {
                        // Flip to data store mode only if feasible
                        out.add((byte) 0x07F);
                        XOR_MODE = false;
                    }
                    numberReps = ccount;
                    data = newFrame;
                } else if (XOR_ALLOWED && (xcount > ccount || (xcount == ccount && XOR_MODE))) {
                    if (!XOR_MODE) {
                        // Flip to XOR mode only if feasible and allowed
                        out.add((byte) 0x07E);
                        XOR_MODE = true;
                    }
                    numberReps = xcount;
                    data = xorFrame;
                }
                byte size = (byte) (0x0ff & (128 + numberReps));
                out.add(size);
                out.add(data[offset]);
                out.add(data[offset + 1]);
                offset += numberReps * 2 + 4;
            }

            // No pattern, just output raw data until
            // 1) We hit a repeating pattern
            // 2) We hit end of data
            // 3) We hit 125 characters
            if (rawData) {
                int seek = offset;
                boolean foundPattern = false;
                int count = 0;
                while (seek < data.length && !foundPattern && count < 125) {
                    // Evaluate if packbits data follows, but switching XOR MODE has a slight penalty
                    int copyCount = (XOR_MODE ? -1 : 0) + countReps(newFrame, baseAddress, seek);
                    int xorCount = XOR_ALLOWED ? (XOR_MODE ? 0 : -1) + countReps(xorFrame, baseAddress, seek) : -1;
                    if (copyCount > -1 || xorCount > -1) {
                        foundPattern = true;
                        // Read ahead +1 and see if better compression would occur there (again, with XOR_MODE switch penalty)
                        int copyCount2 = (XOR_MODE ? -1 : 0) + countReps(newFrame, baseAddress, seek + 1) - 1;
                        int xorCount2 = XOR_ALLOWED ? (XOR_MODE ? 0 : -1) + countReps(xorFrame, baseAddress, seek + 1) - 1 : -1;
                        if (copyCount2 > copyCount || xorCount2 > xorCount) {
                            // If there was a better deal by going up one, use that instead
                            seek++;
                            count++;
                        }
                    } else {
                        seek++;
                        count++;
                    }
                }
                out.add((byte) (0x0ff & count));
                for (int i = 0; i < count; i++) {
                    out.add(data[offset + i]);
                }
                offset = seek;
            }
        }

        out.add((byte) 0);

//        System.out.println("Packbits: "+input.length+" bytes compressed to "+out.size()+" bytes ("+(100-(100*out.size()/input.length))+"% compression)");

        // Convert back to native array
        byte[] result = new byte[out.size()];

        for (int i = 0;
                i < out.size();
                i++) {
            result[i] = out.get(i);
        }
        return result;
    }

    public static byte[] xor(byte[] b1, byte[] b2) {
        if (b1.length != b2.length) {
            return null;
        }

        byte[] out = new byte[b1.length];
        for (int i = 0; i <
                b1.length; i++) {
            out[i] = (byte) (0x0ff & (b1[i] ^ b2[i]));
        }

        return out;
    }

    public static int countBeginningZeros(byte[] in) {
        int index = 0;
        while (index < in.length && in[index] == 0) {
            index++;
        }

        return index;
    }

    public static int countEndingZeros(byte[] in) {
        int count = 0;
        int index = in.length - 1;
        while (index >= 0 && in[index] == 0) {
            index--;
            count++;

        }
        return count;
    }

    public static byte[] subarray(byte[] input, int start, int length) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = input[start + i];
        }

        return out;
    }

    public static byte[] packScreenUpdate(int address, byte[] oldFrame, byte[] newFrame) {
        if (oldFrame != null) {
            byte[] diff = xor(oldFrame, newFrame);
            int z1 = countBeginningZeros(diff);
            int z2 = countEndingZeros(diff);
            if (z1 == oldFrame.length) {
                // Protect against empty frames, nothing to do
                // So return null and let caller deal with it.
                return null;
            }
            address += z1;
            // Get frames, truncating off parts at beginning and end that do not change
            byte[] xf1 = subarray(diff, z1, oldFrame.length - z1 - z2);
            byte[] nf1 = subarray(newFrame, z1, newFrame.length - z1 - z2);
            return packbits(address, xf1, nf1);
        } else {
            return packbits(address, null, newFrame);
        }
    }
}
