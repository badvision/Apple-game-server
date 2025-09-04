/*
 * Palette16.java
 *
 * Created on June 23, 2006, 1:10 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ags.ui.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Administrator
 */
public class Palette16 extends PaletteYIQ {

    protected void initPalette() {
        addColor(0.0, 0.0, 0.0);  //0000 0
        addColor(0.25, 0.5, 0.5);  //0001 1
        addColor(0.25, -0.5, 0.5);  //0010 2
        addColor(0.5, 0.0, 1.0);  //0011 3 +Q
        addColor(0.25, -0.5, -0.5);  //0100 4
        addColor(0.5, 0.0, 0.0);  //0101 5
        addColor(0.5, -1.0, 0.0);  //0110 6 +I
        addColor(0.75,-0.5, 0.5);  //0111 7
        addColor(0.25, 0.5, -0.5);  //1000 8
        addColor(0.5, 1.0, 0.0);  //1001 9 -I
        addColor(0.5, 0.0, 0.0);  //1010 a
        addColor(0.75, 0.5, 0.5);  //1011 b
        addColor(0.5, 0.0, -1.0);  //1100 c -Q
        addColor(0.75, 0.5, -0.5);  //1101 d
        addColor(0.75, -0.5, -0.5);  //1110 e
        addColor(1.0, 0.0, 0.0);  //1111 f
    }

    public static void main(String[] args) {
        try {
            BufferedImage img = new BufferedImage(160, 100, BufferedImage.TYPE_INT_RGB);
            File f = new File("/tmp/test.bmp");
            Palette16 p = new Palette16();
            Graphics2D g = img.createGraphics();
            for (int i = 0; i < 16; i++) {
                ColorYIQ col = p.getColor(i);
                System.out.println("Color " + Integer.toHexString(col.toRGB()));
                g.setColor(new Color(col.toRGB()));
                g.fillRect(i * 10, 0, 10, 100);
            }
            ImageIO.write(img, "bmp", f);
        } catch (IOException ex) {
            Logger.getLogger(Palette16.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}