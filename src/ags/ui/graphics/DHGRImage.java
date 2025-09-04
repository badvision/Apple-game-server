/*
 * DHGRImage.java
 *
 * Created on June 23, 2006, 12:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ags.ui.graphics;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author Administrator
 */
public class DHGRImage implements ImageBuffer {

    int[][] colorMask;
    int[][] colorPattern;
    int[] pageOffset;
    PaletteYIQ palette;
    int targetWidth;
    int targetHeight;

    /**
     * Creates a new instance of DHGRImage
     */
    public DHGRImage() {
        pageOffset = new int[]{0x0000, 0x2000, 0x0001, 0x2001};
        palette = new Palette16();
        colorMask = new int[][]{
            {0x0f, 0x00, 0x00, 0x00},
            {0x70, 0x01, 0x00, 0x00},
            {0x00, 0x1e, 0x00, 0x00},
            {0x00, 0x60, 0x03, 0x00},
            {0x00, 0x00, 0x3c, 0x00},
            {0x00, 0x00, 0x40, 0x07},
            {0x00, 0x00, 0x00, 0x78}
        };
        targetWidth = 140;
        targetHeight = 192;

        // Source: Apple //c reference manual, chapter 5
        colorPattern = new int[][]{
            {0x00, 0x00, 0x00, 0x00},
            {0x08, 0x11, 0x22, 0x44},
            {0x11, 0x22, 0x44, 0x08},
            {0x19, 0x33, 0x66, 0x4c},
            {0x22, 0x44, 0x08, 0x11},
            {0x2a, 0x55, 0x2a, 0x55},
            {0x33, 0x66, 0x4c, 0x19},
            {0x3b, 0x77, 0x6e, 0x5d},
            {0x44, 0x08, 0x11, 0x22},
            {0x4c, 0x19, 0x33, 0x66},
            {0x55, 0x2a, 0x55, 0x2a},
            {0x5d, 0x3b, 0x77, 0x6e},
            {0x66, 0x4c, 0x19, 0x33},
            {0x6e, 0x5d, 0x3b, 0x77},
            {0x77, 0x6e, 0x5d, 0x3b},
            {0x7f, 0x7f, 0x7f, 0x7f}
        };
    }
    private byte[] rawData = new byte[16384];

    public void readColorImage(String file) throws IOException {
        File f = new File(file);
        BufferedImage i = ImageIO.read(f);
        Image s = i.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage b = FloydSteinbergDither.floydSteinbergDither(s, palette);
        ImageIO.write(b, "bmp", new File("/apple2e/test_image.bmp"));
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int color = palette.findColor(ColorYIQ.fromRGB(b.getRGB(x, y)));
                plotColor(x, y, color);
            }
        }
    }

    public int calcYOffset(int i) {
        int a = i % 8 * 1024;
        int y = i / 8;
        return 128 * (y % 8) + 40 * (y / 8) + a;
    }

    public void plotColor(int x, int y, int color) {
        int yOffset = calcYOffset(y);
        int xOffset = (x / 7) * 2;
        int maskOffset = x % 7;
        for (int i = 0; i < 4; i++) {
            int offset = pageOffset[i] + xOffset + yOffset;
            int mask = colorMask[maskOffset][i];
            int oldByte = rawData[offset] & 0x00ff;
            int pattern = colorPattern[color][i];
            rawData[offset] = (byte) ((oldByte & (0x00ff ^ mask))
                    | (pattern & mask));
            if (y % 2 == 0) {
                rawData[offset] = (byte) (rawData[offset] | 0x0080);
            }
        }
    }

    public byte[] getAppleImage() {
        return rawData;
    }

    public void setTargetSize(int x, int y) {
        targetWidth = x;
        targetHeight = y;
    }
}