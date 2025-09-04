package ags.ui;

import ags.communication.DataUtil;
import ags.communication.GenericHost;
import ags.communication.TransferHost;
import java.awt.Image;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a non-mousetext based 40 column apple // screen
 * @author blurry
 */
public class TextScreen40 extends IVirtualScreen {

    /**
     * Raw screen buffer
     */
    byte[] buffer = new byte[0x2000];
    /**
     * 40 spaces in a row
     */
    String spaces = Util.repeat(' ', 40);
    private boolean alreadyActive = false;

    /**
     * Insert a series of bytes to a specific offset
     * @param start Starting offset
     * @param b bytes to insert
     */
    public void insert(int start, byte... b) {
        for (int i = 0; i < b.length; i++) {
            buffer[i + start] = b[i];
        }
    }

    /**
     * Insert a series of bytes to a specific offset
     * @param start Starting offset
     * @param b Bytes to insert
     */
    public void insert(int start, int... b) {
        for (int i = 0; i < b.length; i++) {
            buffer[i + start] = (byte) (b[i] & 0x00ff);
        }
    }

    /** Creates a new instance of TextScreen40 */
    public TextScreen40() {
        clear();
// Set screen hole with observed information (note: taken from a Rom 0 //c!)
// This is in case we have any stupid programs that are too lazy to init their own crap
//478   00	0	0	00	00	00	00	00
//4f8   0	0	b0	8	00	00	00	00
//578	17	ff	ff	00	00	00	00	00
//5f8	28	00	00	17	00	00	00	00
//678	00	00	00	f7	00	ff	ff	ff
//6f8	00	00	00	00	ff	ff	ff	ff
//778	00	00	00	a0	00	03	ff	ff
//7f8	a0	00	00	ff	00	03	ff	ff
        insert(0x4fa - 1024, 0xb0, 0xff);
        insert(0x578 - 1024, 0x17, 0xff, 0xff);
        insert(0x5f8 - 1024, 0x28, 0, 0, 0x17);
        insert(0x678 - 1024, 0, 0, 0, 0xf7, 0, 0xff, 0xff, 0xff);
        insert(0x6f8 - 1024, 0, 0, 0, 0, 0xff, 0xff, 0xff, 0xff);
        insert(0x778 - 1024, 0, 0, 0, 0xa0, 0, 3, 0xff, 0xff);
        insert(0x7f8 - 1024, 0xa0, 0, 0, 0xff, 0, 3, 0xff, 0xff);
    }

    /**
     * Clear screen
     */
    public void clear() {
        for (int i = 0; i < 24; i++) {
            drawText(0, i, spaces, false);
        }
    }

    /*
    Line Addresses     Line Addresses     Line Addresses
    0  $400-$427       8  $428-$44F      16  $450-$477
    1  $480-$4A7       9  $4A8-$4CF      17  $4D0-$4F7
    2  $500-$527      10  $528-$54F      18  $550-$577
    3  $580-$5A7      11  $5A8-$5CF      19  $5D0-$5F7
    4  $600-$627      12  $628-$64F      20  $650-$677
    5  $680-$6A7      13  $6A8-$6CF      21  $6D0-$6F7
    6  $700-$727      14  $728-$74F      22  $750-$777
    7  $780-$6A7      15  $7A8-$7CF      23  $7D0-$7F7
     */
    /**
     * Given y-coordinate, calculate screen buffer offset
     * @param i Logical y-coordinate
     * @return Offset in screen buffer for that row
     */
    private int getYOffset(int i) {
        return 128 * (i % 8) + 40 * (i / 8);
    }

    /**
     * Draw text at a location on the screen
     * @param x X-coordinate (0-based)
     * @param y Y-Coordinate (0-based)
     * @param text Text to draw
     * @param invert If true, text should be drawn in inverse mode
     */
    public void drawText(int x, int y, String text, boolean invert) {
        if (y < 0 || y > 23) {
            return;
        }
        int offset = getYOffset(y);
        // Fix for 80-column mode and old ]['s
        // --> lowecase letters are not used if they won't work in those cases
        if (invert || GenericHost.getInstance().isLegacyMode()) {
            text = text.toUpperCase();
        }
        for (int i = 0; i < text.length(); i++) {
            if (i + x < 0 || i + x > 39) {
                continue;
            }
            byte c = (byte) text.charAt(i);
            buffer[offset + i + x] = invert ? (byte) (c & 0x003f) : (byte) (c | 0x0080);
        }
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
        String spaces = Util.repeat(' ', x2 - x1 + 1);
        for (int i = y1; i <= y2; i++) {
            drawText(x1, i, spaces, invert);
        }
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
        int xSize = x2 - x1 + 1;
        String line = Util.repeat('-', xSize);
        drawText(x1 - 1, y1 - 1, "/" + line + "\\", invert);
        for (int i = y1; i <= y2; i++) {
            String bar = "|";
            if (invert) bar="!";
            drawText(x1 - 1, i, bar, invert);
            drawText(x2 + 1, i, bar, invert);
        }
        drawText(x1 - 1, y2 + 1, "\\" + line + "/", invert);
    }

    /**
     * Draw the cursor at a specific coordinate
     * @param x X-coordinate of the cursor
     * @param y Y-coordinate of the cursor
     */
    public void drawCursor(int x, int y) {
        drawText(x, y, ">", true);
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
//        throw new java.lang.UnsupportedOperationException("Dude, this is a text buffer!  You can't draw graphics here!");
    }

    /**
     * Get the raw buffer
     * @return Raw buffer data
     */
    public byte[] getBuffer() {
        return this.buffer;
    }

    public int getDisplayOffset() {
        return 0x0400;
    }

    public int[] getActivateCode() {
        return new int[]{
                    0xad, 0x51, 0xc0, // lda $c050 - text
                    0xad, 0x54, 0xc0, // lda $c054 - page1
                    0x8d, 0x0c, 0xc0, // sta $c00c - disable 80-col
                    0x8d, 0x0e, 0xc0, // disable mousetext
                    0x60 // rts
                };
    }

    public void activate(TransferHost host) {
        try {
            if (alreadyActive) {
                return;
            }
            alreadyActive = true;
            host.toggleSwitch(0x0c051); // Text
            host.toggleSwitch(0x0c054); // Page1
            host.toggleSwitch(0x0c00e); // Disable ALT Charset
        } catch (IOException ex) {
            Logger.getLogger(TextScreen40.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}