/*
 * HGRImage.java
 *
 * Created on Jan 7, 2009, 6:00 PM
 *
 */
package ags.ui.graphics;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 *
 * @author blurry
 */
public class HGRImage implements ImageBuffer {

    int[] colorMask;
    int[][] colorPattern;
    PaletteYIQ palette;
    int targetWidth;
    int targetHeight;
    File currentDirectory = null;

    /**
     * Creates a new instance of HGRImage
     */
    public HGRImage() {
        palette = new Palette6();
//        colorMask = new int[][]{
//                    {0x83, 0x00},
//                    {0x8C, 0x00},
//                    {0xB0, 0x00},
//                    {0xC0, 0x81},
//                    {0x00, 0x86},
//                    {0x00, 0x98},
//                    {0x00, 0xE0}
//                };

        colorMask = new int[]{0x81, 0x82, 0x84, 0x88, 0x90, 0xA0, 0xC0};
        targetWidth = 280;
        targetHeight = 192;

        // Source: Apple //c reference manual, chapter 5
        colorPattern = new int[][]{
                    {0x00, 0x00},
                    {0x2A, 0x55},
                    {0x55, 0x2A},
                    {0x7F, 0x7F},
                    {0x80, 0x80},
                    {0xD5, 0xAA},
                    {0xAA, 0xD5},
                    {0xFF, 0xFF}
                };
    }
    private byte[] rawData = new byte[0x2000];

    public void readColorImage(String file) throws IOException {
        File f = new File(file);
        currentDirectory = f.getParentFile();
        BufferedImage i = ImageIO.read(f);
        convertColorImage(i);
    }

    public void convertColorImage(Image i) throws IOException {
        Image s = i.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage b = FloydSteinbergDither.floydSteinbergDither(s, palette);
        // Allocate buffer to capture resized image source
        BufferedImage source = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_INDEXED, getColorModel());
        source.getGraphics().drawImage(b, 0, 0, null);
        DataBuffer raster = source.getRaster().getDataBuffer();
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int color = raster.getElem((y*targetWidth)+x);
                plotColor(x, y, color);
            }
        }
//     Write simulated image to a file for reviewing purposes
//        File output = File.createTempFile("test_hgr_color",".bmp", new File("/tmp"));
//        ImageIO.write(b, "bmp", output);
    }

    public int calcYOffset(int i) {
        int a = i % 8 * 1024;
        int y = i / 8;
        return 128 * (y % 8) + 40 * (y / 8) + a;
    }

    public void plotColor(int x, int y, int color) {
        int yOffset = calcYOffset(y);
//        int xOffset = (x / 7) * 2;
//        int maskOffset = x % 7;
        int xOffset = (x / 7);
        int maskOffset = x % 7;
//        for (int i = 0; i < 2; i++) {
//            int offset = xOffset + yOffset + i;
            int i = (x/7)%2;
            int offset = xOffset + yOffset;
            int mask = colorMask[maskOffset];
            // Don't let black and white colors screw with the high-order bit!
            if (color % 4 == 0 || color % 4 == 3) {
                mask &= 0x7f;
            }
            int oldByte = rawData[offset] & 0x00ff;
            int pattern = colorPattern[color][i];
            rawData[offset] = (byte) ((oldByte & (0x00ff ^ mask)) |
                    (pattern & mask));
//        }
    }

    public byte[] getAppleImage() {
        return rawData;
    }

    public void setTargetSize(int x, int y) {
        targetWidth = x;
        targetHeight = y;
    }

    private IndexColorModel getColorModel() {
        byte[] r = new byte[palette.colors.size()];
        byte[] g = new byte[palette.colors.size()];
        byte[] b = new byte[palette.colors.size()];
        for (int i = 0; i < palette.colors.size(); i++) {
            int color = palette.getColor(i).toRGB();
            r[i] = (byte) ColorYIQ.getR(color);
            g[i] = (byte) ColorYIQ.getG(color);
            b[i] = (byte) ColorYIQ.getB(color);
        }
        IndexColorModel colorModel = new IndexColorModel(8, palette.colors.size(), r, g, b);
        return colorModel;
    }
}