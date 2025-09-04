/*
 * GRImage.java
 *
 * Created on June 23, 2006, 12:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
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
 * @author Administrator
 */
public class GR2Image implements ImageBuffer {

    Palette48 palette;
    int targetWidth = 40;
    int targetHeight = 48;
    boolean oddFrame = false;

    /**
     * Creates a new instance of GRImage
     */
    public GR2Image() {
        palette = new Palette48();
    }

    private byte[] rawData1 = new byte[1024];
    private byte[] rawData2 = new byte[1024];
    public void readColorImage(String file) throws IOException {
        File f = new File(file);
        BufferedImage i = ImageIO.read(f);
        Image s = i.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_SMOOTH);
        BufferedImage b = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        b.getGraphics().drawImage(s, 0, 0, null);
        ImageIO.write(b, "bmp", new File("/apple2e/test_image.bmp"));
//        for (int y=0; y < targetHeight; y++)
//            for (int x=0; x < targetWidth; x++) {
//                int color = palette.findColor(b.getRGB(x,y));
//                plotColor(x,y,color);
//            }
    }
    
    public int calcYOffset(int i) {
        i /= 2;
        return 128 * (i % 8) + 40 * (i / 8);
    }
    
    public void plotColor(int x, int y, int color) {
        int offset = calcYOffset(y);
        Integer[] c=palette.getAppleColors(color);
        int cc = c[0];
        if (oddFrame) cc=c[1];
//        if ((x+y) % 2 == 0) {
//            cc = oddFrame ? c[1]:c[0];
//        } else {
//            cc = oddFrame ? c[0]:c[1];
//        }
        if (y%2 == 0) {
            rawData1[offset+x] = (byte) ((rawData1[offset+x] & 0xF0) | cc);
            rawData2[offset+x] = (byte) ((rawData2[offset+x] & 0xF0) | cc);
        } else {
            rawData1[offset+x] = (byte) ((rawData1[offset+x] & 0x0F) | (cc << 4));
            rawData2[offset+x] = (byte) ((rawData2[offset+x] & 0x0F) | (cc << 4));
        }
    }

    public void convertColorImage(Image i) throws IOException {
        oddFrame = !oddFrame;
        Image s = i.getScaledInstance(targetWidth, targetHeight, java.awt.Image.SCALE_FAST);
        // Allocate buffer to capture resized image source
        BufferedImage source = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_INDEXED, getColorModel());
        source.getGraphics().drawImage(s, 0, 0, null);
        // Allocate buffer to capture program's simulated output
//        BufferedImage simulated = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        DataBuffer raster = source.getRaster().getDataBuffer();
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
//                int sourcePixel = b.getRGB(x, y);
//                ColorYIQ targetColor = ColorYIQ.fromRGB(sourcePixel);
//                int color = palette.findColor(targetColor);
//                int color = source.getData().getSample(x, y, 0);
                int color = raster.getElem((y*targetWidth)+x);

                plotColor(x, y, color);
//                simulated.setRGB(x, y, palette.getColor(color).toRGB());
            }
        }
    }

    private IndexColorModel getColorModel() {
        byte[] r = new byte[palette.colors.size()];
        byte[] g = new byte[palette.colors.size()];
        byte[] b = new byte[palette.colors.size()];
        for (int i = 0; i < palette.colors.size(); i++) {
//            int color = palette.getColor(i).toRGB();
//            r[i] = (byte) ColorYIQ.getR(color);
//            g[i] = (byte) ColorYIQ.getG(color);
//            b[i] = (byte) ColorYIQ.getB(color);
            int[] color = palette.getColor(i);
            r[i] = (byte) color[0];
            g[i] = (byte) color[1];
            b[i] = (byte) color[2];
        }
        IndexColorModel colorModel = new IndexColorModel(8, palette.colors.size(), r, g, b);
        return colorModel;
    }

    public byte[] getAppleImage() {
        if (oddFrame)
            return rawData1;
        return rawData2;
    }

    public void setTargetSize(int x, int y) {
        targetWidth=x;
        targetHeight=y;
    }
}