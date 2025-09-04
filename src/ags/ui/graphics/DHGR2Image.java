/*
 * DHGR2Image.java
 *
 * Created on June 24, 2006, 10:06 PM
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
public class DHGR2Image implements ImageBuffer {

    Palette48 palette;
    DHGRImage i1;
    DHGRImage i2;
    public int mode = 0;

    /**
     * Creates a new instance of DHGR2Image
     */
    public DHGR2Image() {
        palette = new Palette48();
        i1 = new DHGRImage();
        i2 = new DHGRImage();
    }

    public void readColorImage(String file) throws IOException {
        File f = new File(file);
        BufferedImage i = ImageIO.read(f);
        Image s = i.getScaledInstance(140, 192, java.awt.Image.SCALE_SMOOTH);
        BufferedImage b = FloydSteinbergDither.floydSteinbergDither(s, palette);
        for (int y = 0; y < 192; y++) {
            for (int x = 0; x < 140; x++) {
                int color = palette.findColor(b.getRGB(x, y));
                plotColor(x, y, color);
//                b.setRGB(x, y, palette.getColorInt(color));
            }
        }
//        ImageIO.write(b, "bmp", new File("/apple2e/test_image.bmp"));
    }

    public byte[] getAppleImage(int i) {
        if (i == 0) {
            return i1.getAppleImage();
        }
        return i2.getAppleImage();
    }

    public byte[] getAppleImage() {
        return null;
    }

    public void plotColor(int x, int y, int color) {
        Integer[] colors = palette.getAppleColors(color);
        if (mode == 0 || (mode == 1 && y % 2 == 0) || (mode == 2 && (x % 2 == y % 2))) {
            i1.plotColor(x, y, colors[0]);
            i2.plotColor(x, y, colors[1]);
        } else {
            i1.plotColor(x, y, colors[1]);
            i2.plotColor(x, y, colors[0]);
        }
    }

    public void setTargetSize(int x, int y) {
    }
}
