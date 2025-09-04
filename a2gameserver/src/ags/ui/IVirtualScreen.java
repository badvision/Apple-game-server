package ags.ui;
import ags.communication.DataUtil;
import ags.communication.TransferHost;
import ags.controller.Configurable;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
/*
 * IVirtualScreen.java
 *
 * Created on May 19, 2006, 3:20 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 * Abstract frame buffer
 * @author blurry
 */
public abstract class IVirtualScreen {
    @Configurable(category=Configurable.CATEGORY.ADVANCED, isRequired=false)
    public static boolean USE_COMPRESSION = true;
    abstract public void activate(TransferHost host);
    byte[] lastScreen = null;
    /**
     * Make a copy of the screen
     */
    protected void copyScreen() {
        byte[] buffer = getBuffer();
        if (lastScreen == null) lastScreen=new byte[buffer.length];
        for (int i = 0; i < buffer.length; i++) {
            lastScreen[i] = buffer[i];
        }
    }

    public void markBufferStale() {
        stale = true;
    }

    public boolean stale = true;
    public void send(TransferHost host) {
        byte[] buffer = getBuffer();
        try {
            if (!USE_COMPRESSION) {
                host.sendRawData(buffer, getDisplayOffset(), 0, buffer.length);
            } else {
                byte[] send = DataUtil.packScreenUpdate(getDisplayOffset(), stale ? null : lastScreen, buffer);
                host.sendCompressedData(send);
            }
            stale = false;
            copyScreen();
        } catch (IOException ex) {
            Logger.getLogger(TextScreen40.class.getName()).log(Level.SEVERE, null, ex);
        }
        activate(host);
    }
    
    /**
     * Clear screen
     */
    abstract public void clear();
    /**
     * Draw text at a location on the screen
     * @param x X-coordinate (0-based)
     * @param y Y-Coordinate (0-based)
     * @param text Text to draw
     * @param invert If true, text should be drawn in inverse mode
     */
    abstract public void drawText(int x, int y, String text, boolean invert);
    /**
     * Draw border around the outside of the specified box
     * @param x1 Leftmost X coordinate
     * @param y1 Upper Y coordinate
     * @param x2 Rightmost X coordinate
     * @param y2 Lower Y coordinate
     * @param invert If true, border should be drawn in inverse mode
     */
    abstract public void drawBorder(int x1, int y1, int x2, int y2, boolean invert);
    /**
     * Draw a solid box between the given coordinates
     * @param x1 Leftmost X coordinate
     * @param y1 Upper Y coordinate
     * @param x2 Rightmost X coordinate
     * @param y2 Lower Y coordinate
     * @param invert @param invert If true, box should be drawn in inverse mode
     */
    abstract public void drawBox(int x1, int y1, int x2, int y2, boolean invert);
    /**
     * Draw the cursor at a specific coordinate
     * @param x X-coordinate of the cursor
     * @param y Y-coordinate of the cursor
     */
    abstract public void drawCursor(int x, int y);
    /**
     * Draw an image to the framebuffer scaled to fit within the provided boundary
     * @param x Leftmost X coordinate
     * @param y Upper Y coordinate
     * @param x1 Rightmost X coordinate
     * @param y1 Lower Y coordinate
     * @param i image to draw
     */
    abstract public void drawImage(int x, int y, int x1, int y1, Image i);
    /**
     * Get the raw buffer
     * @return Raw buffer data
     */
    abstract public byte[] getBuffer();

    abstract public int getDisplayOffset();
}
