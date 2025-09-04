package ags.ui;

import ags.communication.TransferHost;
import ags.controller.Configurable;
import ags.controller.Configurable.CATEGORY;
import ags.ui.graphics.HGRImage;
import ags.ui.graphics.Palette6;
import ags.ui.graphics.PaletteYIQ;
import ags.ui.host.Style;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a non-mousetext based 40 column apple // screen
 * @author blurry
 */
public class HiresScreen extends IVirtualScreen {
    // Screen buffer

    private PaletteYIQ palette = new Palette6();
    private Color BLUE = new Color(palette.getColor(5).toRGB());
    private Color ORANGE = new Color(palette.getColor(6).toRGB());
    public static BufferedImage screen = new BufferedImage(560, 192, BufferedImage.TYPE_INT_RGB);
    private HGRImage appleScreen = new HGRImage();
    @Configurable(category = CATEGORY.ADVANCED, isRequired = false)
    public static Style.FONT DISPLAY_FONT = Style.FONT.APPLE2FAT;
    public static FontMetrics appleFontMetrics;
    // Font variables
    public static int CHARACTER_WIDTH = 14;
    public static int CHARACTER_HEIGHT = 8;

    public static void setCurrentFont(Style.FONT newFont) {
        DISPLAY_FONT = newFont;
        appleFontMetrics = screen.getGraphics().getFontMetrics(DISPLAY_FONT.font);
    }
    /**
     * 40 spaces in a row
     */
    String spaces = Util.repeat(' ', 40);
    private boolean changed;
    private boolean alreadyActive = false;

    /** Creates a new instance of TextScreen40 */
    public HiresScreen() {
        setCurrentFont(DISPLAY_FONT);
        clear();
    }

    public BufferedImage getScreen() {
        return screen;
    }

    /**
     * Clear screen
     */
    public void clear() {
        changed = true;
        Graphics g = screen.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, screen.getWidth(), screen.getHeight());
    }

    /**
     * Draw text at a location on the screen
     * @param x X-coordinate (0-based)
     * @param y Y-Coordinate (0-based)
     * @param text Text to draw
     * @param invert If true, text should be drawn in inverse mode
     */
    public void drawText(int x, int y, String text, boolean invert) {
        changed = true;
        if (y < 0 || y > 23) {
            return;
        }
        int xx = x * CHARACTER_WIDTH;
        int yy = y * CHARACTER_HEIGHT;
        // Fix for 80-column mode and old ]['s
        // --> lowecase letters are not used if they won't work in those cases
//        if (invert || GenericHost.getInstance().isLegacyMode()) text = text.toUpperCase();
        Rectangle2D size = appleFontMetrics.getStringBounds(text, screen.getGraphics());
        BufferedImage textImg = new BufferedImage((int) size.getWidth(), (int) (size.getHeight() * DISPLAY_FONT.heightAdjust), BufferedImage.TYPE_INT_ARGB);
        Graphics g = textImg.getGraphics();
        g.setFont(DISPLAY_FONT.font);
        if (invert) {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, (int) size.getWidth(), (int) size.getHeight());
        }
        g.setColor(!invert ? Color.WHITE : Color.BLACK);
        g.drawString(text, 0, (int) (appleFontMetrics.getAscent() * DISPLAY_FONT.heightAdjust));
        if (DISPLAY_FONT.isFat) {
            g.drawString(text, 1, (int) (appleFontMetrics.getAscent() * DISPLAY_FONT.heightAdjust)); // make it fat!
        }
//        drawImage(xx, yy, xx + text.length() * CHARACTER_WIDTH, yy + CHARACTER_HEIGHT, textImg);
        drawImage(x, y, x + text.length(), y + 1, textImg);
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
        Graphics g = screen.getGraphics();
        int xx1 = (x1) * CHARACTER_WIDTH;
        int yy1 = (y1) * CHARACTER_HEIGHT;
        int xx2 = (x2 + 1) * CHARACTER_WIDTH;
        int yy2 = (y2 + 1) * CHARACTER_HEIGHT;
        g.setColor(invert ? Color.WHITE : Color.BLACK);
        g.fillRect(xx1, yy1, xx2 - xx1, yy2 - yy1);
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
        Graphics g = screen.getGraphics();
        int xx1 = (x1 - 1) * CHARACTER_WIDTH;
        int yy1 = (y1 - 1) * CHARACTER_HEIGHT;
        int xx2 = (x2 + 2) * CHARACTER_WIDTH - 1;
        int yy2 = (y2 + 2) * CHARACTER_HEIGHT - 1;
        g.setColor(Color.BLACK);
        g.fillRect(xx1, yy1, CHARACTER_WIDTH - 1, yy2 - yy1);
        g.fillRect(xx2 - CHARACTER_WIDTH + 1, yy1, CHARACTER_WIDTH - 1, yy2 - yy1);
        g.fillRect(xx1, yy1, xx2 - xx1, CHARACTER_HEIGHT - 1);
        g.fillRect(xx1, yy2 - CHARACTER_HEIGHT + 1, xx2 - xx1, CHARACTER_HEIGHT - 1);
        if (invert) {
            g.setColor(Color.WHITE);
        } else {
            g.setColor(ORANGE);
        }
        for (int ix = 0; ix < CHARACTER_WIDTH; ix++) {
            if (ix == 4) {
                if (invert) {
                    g.setColor(BLUE);
                } else {
                    g.setColor(Color.BLACK);
                }
            }
            int iy = (int) (ix * ((double) CHARACTER_HEIGHT / (double) CHARACTER_WIDTH));
            g.drawRect(xx1 + ix, yy1 + iy, xx2 - xx1 - ix * 2, yy2 - yy1 - iy * 2);
        }
        g.setColor(Color.BLACK);
        int cornerWidth = CHARACTER_WIDTH / 4;
        g.fillRect(xx1, yy1, cornerWidth, 1);
        g.fillRect(xx1, yy2, cornerWidth, 1);
        g.fillRect(xx2 - cornerWidth + 1, yy1, cornerWidth, 1);
        g.fillRect(xx2 - cornerWidth + 1, yy2, cornerWidth, 1);
    }

    /**
     * Draw the cursor at a specific coordinate
     * @param x X-coordinate of the cursor
     * @param y Y-coordinate of the cursor
     */
    public void drawCursor(int x, int y) {
        changed = true;
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
        changed = true;
        screen.getGraphics().drawImage(i,
                x * CHARACTER_WIDTH,
                y * CHARACTER_HEIGHT,
                (x1 - x) * CHARACTER_WIDTH,
                (y1 - y) * CHARACTER_HEIGHT, null);
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
            Logger.getLogger(HiresScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public int getDisplayOffset() {
        return 0x02000;
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
            host.toggleSwitch(0x0c057); // hires
            host.toggleSwitch(0x0c054); // page1
            host.toggleSwitch(0x0c00c); // 40-col
        } catch (IOException ex) {
            Logger.getLogger(HiresScreen.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}