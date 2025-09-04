package ags.ui;

import ags.communication.TransferHost;
import ags.ui.graphics.GRImage;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a lores based 40 column apple // screen
 * @author blurry
 */
public class LoresScreen extends IVirtualScreen {
    public static BufferedImage screen = new BufferedImage(40, 48, BufferedImage.TYPE_INT_RGB);
    private GRImage appleScreen = new GRImage();

    /**
     * 40 spaces in a row
     */
    String spaces = Util.repeat(' ', 40);
    private boolean alreadyActive = false;
    private boolean changed = false;

    /** Creates a new instance of TextScreen40 */
    public LoresScreen() {
        clear();
    }

    /**
     * Clear screen
     */
    public void clear() {
        changed = true;
        drawBox(0,0,39,47, false);
    }

    /**
     * Draw text at a location on the screen
     * @param x X-coordinate (0-based)
     * @param y Y-Coordinate (0-based)
     * @param text Text to draw
     * @param invert If true, text should be drawn in inverse mode
     */
    public void drawText(int x, int y, String text, boolean invert) {
    }

    /**
     * Draw a solid box between the given coordinates
     * @param x1 Leftmost X coordinate
     * @param y1 Upper Y coordinate
     * @param x2 Rightmost X coordinate
     * @param y2 Lower Y coordinate
     * @param invert @param invert If true, box should be drawn in inverse mode
     */
    public void drawBox(int x1, int y1, int x2, int y2, boolean invert) {
        changed = true;
        screen.getGraphics().setColor(invert ? Color.WHITE:Color.BLACK);
        screen.getGraphics().fillRect(x1,y1, x2-x1+1, y2-y1+1);
    }

    /**
     * Draw border around the outside of the specified box
     * @param x1 Leftmost X coordinate
     * @param y1 Upper Y coordinate
     * @param x2 Rightmost X coordinate
     * @param y2 Lower Y coordinate
     * @param invert If true, border should be drawn in inverse mode
     */
    public void drawBorder(int x1, int y1, int x2, int y2, boolean invert) {
        changed = true;
        screen.getGraphics().setColor(invert ? Color.BLACK:Color.BLUE);
        screen.getGraphics().drawRect(x1-1,y1-1, x2-x1+2, y2-y1+2);
    }

    /**
     * Draw the cursor at a specific coordinate
     * @param x X-coordinate of the cursor
     * @param y Y-coordinate of the cursor
     */
    public void drawCursor(int x, int y) {
    }

    /**
     * NOT IMPLEMENTED
     * @param x Leftmost X coordinate
     * @param y Upper Y coordinate
     * @param x1 Rightmost X coordinate
     * @param y1 Lower Y coordinate
     * @param i image to draw
     */
    public void drawImage(int x, int y, int x1, int y1, Image i) {
        changed = true;
        screen.getGraphics().drawImage(i, x, y, x1-x, y1-y, null);
    }

   /**
     * Get the raw buffer
     * @return Raw buffer data
     */
    public byte[] getBuffer() {
        try {
            if (changed) {
                appleScreen.convertColorImage(screen);
            }
            changed = false;
            return appleScreen.getAppleImage();
        } catch (IOException ex) {
            Logger.getLogger(LoresScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    boolean stale = true;
    public void activate(TransferHost host) {
        if (alreadyActive) {
            return;
        }
        alreadyActive = true;
        try {
            host.toggleSwitch(0x0c052); // fullscreen
            host.toggleSwitch(0x0c050); // 40-col
            host.toggleSwitch(0x0c056); // lores
            host.toggleSwitch(0x0c054); // page1
            host.toggleSwitch(0x0c00c); // 40-col
        } catch (IOException ex) {
            Logger.getLogger(HiresScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getDisplayOffset() {
        return 0x0400;
    }
}