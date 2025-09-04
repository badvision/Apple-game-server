/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.ui.graphics;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 *
 * @author brobert
 */
public class Convert {
    // NOTE: Most of this code was borrowed from the video rendering classes of JACE
    // Maps the dhgr patterns to hgr bit positions (bit 2 is the hi-bit):
    //  000 --> 0000
    //  001 --> 1001 GREEN
    //  010 --> 0110 PURPLE
    //  011 --> 1111 WHITE
    //  101 --> 0011 BLUE
    //  110 --> 1100 ORANGE
    //  111 --> 1111 WHITE
    private static final int[] hgrDhgr = new int[]{
        0, 9, 6, 15, 0, 3, 12, 15
    };
    static int[][] hgrToDhgr;
    static int[][] hgrToDhgrBW;
    static int[] textOffset;
    static int[] hiresOffset;
    static {
        // Calculate HGR to DHGR pattern conversion tables
        hgrToDhgr = new int[256][256];
        hgrToDhgrBW = new int[256][256];
        for (int bb1 = 0; bb1 < 256; bb1++) {
            for (int bb2 = 0; bb2 < 256; bb2++) {
                int h1 = (bb1 & 0x080) >> 5;
                int h2 = (bb2 & 0x080) >> 5;
                int b1 = bb1 & 0x07f;
                int b2 = bb2 & 0x07f;
                int dhgrWord = 0;
                dhgrWord |= (hgrDhgr[(b1 & 3) | h1]);
                b1 >>= 2;
                dhgrWord |= (hgrDhgr[(b1 & 3) | h1] << 4);
                b1 >>= 2;
                dhgrWord |= (hgrDhgr[(b1 & 3) | h1] << 8);
                b1 >>= 2;
                dhgrWord |= (3 & (hgrDhgr[(b1 & 1) | ((b2 & 1) << 1) | h1])) << 12;
                dhgrWord |= (12 & (hgrDhgr[(b1 & 1) | ((b2 & 1) << 1) | h2])) << 12;
                b2 >>= 1;
                dhgrWord |= hgrDhgr[(b2 & 3) | h2] << 16;
                b2 >>= 2;
                dhgrWord |= hgrDhgr[(b2 & 3) | h2] << 20;
                b2 >>= 2;
                dhgrWord |= hgrDhgr[(b2 & 3) | h2] << 24;
                hgrToDhgr[bb1][bb2] = dhgrWord;
                hgrToDhgrBW[bb1][bb2] =
                        byteDoubler((byte) bb1) | (byteDoubler((byte) bb2) << 14);
            }
        }

        // Calculate Y coordinate offsets (screen y -> memory offset)
        // Note: Text offset assumes y ranges from 0 to 191 -- multiply by 7 for hi-res lines
        textOffset = new int[192];
        hiresOffset = new int[192];
        for (int i=0; i < 192; i++) {
            textOffset[i] = calculateTextOffset(i>>3);
            hiresOffset[i] = calculateHiresOffset(i);
        }
    }

    public static void convertDHGRtoBWRaster(byte[] source, BufferedImage output) {
        boolean doubleHeight = false;
        if (output.getHeight() == 192*2) {
            doubleHeight = true;
        }
        for (int y = 0; y < 192; y++) {
            int rowAddress = hiresOffset[y];
            for (int x = 0; x < 40; x++) {
                int b1 = 0x07f & (source[rowAddress + 0x02000 + x]);
                int b2 = 0x07f & (source[rowAddress + x]);
                int dhgrWord = b1 << 7 | b2;
                for (int i = 0; i < 14; i++) {
                    boolean isOn = (dhgrWord & 0x01) == 1;
                    int xx = x*14 + i;
                    int color = isOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                    if (doubleHeight) {
                        output.setRGB(xx, y*2, color);
                        output.setRGB(xx, y*2 + 1, color);
                    } else {
                        output.setRGB(xx, y, color);
                    }
                    dhgrWord >>= 1;
                }
            }
        }
    }
    
    public static void convertHGRtoBWRaster(byte[] source, BufferedImage output) {
        boolean doubleHeight = false;
        if (output.getHeight() == 192*2) {
            doubleHeight = true;
        }
        for (int y = 0; y < 192; y++) {
            int rowAddress = hiresOffset[y];
            for (int x = 0; x < 40; x++) {
                int b1 = 0x0ff & (source[rowAddress + x]);
                int b2 = 0x0ff & (source[rowAddress + x + 1]);
                int dhgrWord = hgrToDhgr[b1][b2];
                for (int i = 0; i < 14; i++) {
                    boolean isOn = (dhgrWord & 0x01) == 1;
                    int xx = x*14 + i;
                    int color = isOn ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                    if (doubleHeight) {
                        output.setRGB(xx, y*2, color);
                        output.setRGB(xx, y*2 + 1, color);
                    } else {
                        output.setRGB(xx, y, color);
                    }
                    dhgrWord >>= 1;
                }
            }
        }
    }
    static public int calculateHiresOffset(int y) {
        return calculateTextOffset(y>>3) + ((y&7) << 10);
    }
    static public int calculateTextOffset(int y) {
        return ((y&7)<<7) + 40*(y>>3);
    }

    protected static int byteDoubler(byte b) {
        int num =
// Skip hi-bit because it's not used in display
//                ((b&0x080)<<7) |
                ((b&0x040)<<6) |
                ((b&0x020)<<5) |
                ((b&0x010)<<4) |
                ((b&0x08)<<3) |
                ((b&0x04)<<2) |
                ((b&0x02)<<1) |
                (b&0x01);
        return num | ( num << 1 );
    }
}
