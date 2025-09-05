/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.ui.gameSelector;

import ags.controller.Configurable;
import ags.controller.Launcher;
import ags.game.Game;
import ags.ui.FrameBasedWidget;
import ags.ui.IApplication;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author brobert
 */
public class GameInfoWidget extends FrameBasedWidget {

    @Configurable(category=Configurable.CATEGORY.RUNTIME, isRequired=false)
    public static String SCREENSHOTS_PATH = "data/screenshots";
    @Configurable(category=Configurable.CATEGORY.ADVANCED, isRequired=false)
    public static int SCREENSHOT_SMALL_WIDTH = 10;
    @Configurable(category=Configurable.CATEGORY.ADVANCED, isRequired=false)
    public static int SCREENSHOT_SMALL_HEIGHT = 8;

    static Game activeGame = null;

    static void setActiveItem(Game g) {
        activeGame = g;
    }
    int desiredHeight = 0;
    int inactiveHeight = 0;
    boolean screenViewMode = false;
    private int currentScreen = 0;
    boolean hasScreenshots = false;

    public void drawScreenshot(String filename, int x, int y, int xSize, int ySize) throws IOException {
        String fullPath = SCREENSHOTS_PATH+"/" + filename;
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fullPath);
        if (in == null) {
            return; // Skip drawing this screenshot instead of crashing
        }
        BufferedImage i = ImageIO.read(in);
        app.getScreen().drawImage(x, y, x + xSize, y + ySize, i);
    }

    @Override
    public void setYSize(int ySize) {
        setYSize(ySize, false);
    }

    public void setYSize(int ySize, boolean isDesired) {
        super.setYSize(ySize);
        if (!isDesired) {
            inactiveHeight = ySize;
        }
    }

    public GameInfoWidget(IApplication parentApp) {
        super(parentApp);
    }

    @Override
    public void redrawInside() {
        String title = "No game selected";
        String description = "Select a game below using the arrow keys.";
        List<String> screenshots = null;
        if (activeGame != null) {
            title = activeGame.getName();
            if (isEmpty(activeGame.getDescription())) {
                description = "No description for this game.";
            } else {
                description = activeGame.getDescription();
            }
            screenshots = normalizeList(activeGame.getScreenshot());
            if (!isEmpty(activeGame.getAuthor())) {
                description += "\n\nPublisher: " + activeGame.getAuthor();
            }
            if (!isEmpty(activeGame.getYear())) {
                description += "\n\nYear: " + activeGame.getYear();
            }
        }
        app.getScreen().drawBox(getX(), getY(), getX() + getXSize() - 1, getY() + getYSize() - 1, false);
        app.getScreen().drawText((getXSize() - title.length()) / 2 + getX(), getY() - 1, title, true);

        String message = null;
        if (screenViewMode) {
            if (screenshots == null || screenshots.isEmpty()) {
                screenViewMode = false;
            } else {
                if (currentScreen >= screenshots.size()) {
                    currentScreen = 0;
                }
                message = "SPACE advances, V exits";
                String screen = screenshots.get(currentScreen);
                try {
                    drawScreenshot(screen, 1, 1, 38, 22);
                } catch (IOException ex) {
                    Logger.getLogger(GameInfoWidget.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            int topWidth = getXSize();
            int topHeight = 0;
            desiredHeight = inactiveHeight;
            if (screenshots != null && !screenshots.isEmpty()) {
                hasScreenshots = true;
                topWidth -= SCREENSHOT_SMALL_WIDTH;
                topHeight = SCREENSHOT_SMALL_HEIGHT * screenshots.size();
                drawScreenshots(screenshots, getX() + getXSize() - SCREENSHOT_SMALL_WIDTH, getY(),
                        SCREENSHOT_SMALL_WIDTH, SCREENSHOT_SMALL_HEIGHT);
                desiredHeight = screenshots.size() * 8;
            } else {
                hasScreenshots = false;
            }

            List<String> lines = formatString(description, topWidth, topHeight, getXSize());
            desiredHeight = Math.max(desiredHeight, lines.size());
            for (int i = 0; i < getYSize() && i < lines.size(); i++) {
                app.getScreen().drawText(getX(), getY() + i, lines.get(i), false);
            }
            String tab = "TAB";
            if (Launcher.MACHINE_TYPE == Launcher.MACHINE_TYPES.Apple2)
                tab = "/";
            if (!isActive()) {
                if (desiredHeight != inactiveHeight || hasScreenshots) {
                    message = "("+tab+" for more)";
                }
            } else {
                message = "("+tab+" for less" + (hasScreenshots ? ", V to view screens)" : ")");
            }
        }
        if (message != null) {
            app.getScreen().drawText((getXSize() - message.length()) / 2 + getX(), getY() + getYSize(), message, true);
        }
    }

    @Override
    public boolean handleKeypress(byte b) {
        if (hasScreenshots && (b == 'V' || b == 'v')) {
            screenViewMode = !screenViewMode;
            if (screenViewMode) {
                setYSize(22, true);
            } else {
                setYSize(desiredHeight, true);
            }
            return true;
        }
        if (b == ' ' && screenViewMode) {
            currentScreen++;
            return true;
        }
        // Doesn't respond to keypresses
        return false;
    }

    @Override
    public void setActive(boolean active) {
        screenViewMode = false;
        currentScreen = 0;
        if (active) {
            setYSize(desiredHeight, true);
        } else {
            setYSize(inactiveHeight, true);
        }

        super.setActive(active);
    }

    private void drawScreenshots(List<String> screenshots, int x, int y, int xSize, int ySize) {
        for (String filename : screenshots) {
            try {
                drawScreenshot(filename, x, y, xSize, ySize);
                y += ySize;
                if (!isActive()) {
                    break;
                }
            } catch (IOException ex) {
                Logger.getLogger(GameInfoWidget.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private List<String> formatString(String s, int topWidth, int topHeight, int mainWidth) {
        ArrayList<String> output = new ArrayList<String>();
        int useWidth = topWidth;
        List<String> reformat = new ArrayList<String>();
        String last = "";
        for (String line : s.split("\n")) {
            line = line.trim();
            if (line.length() == 0) {
                reformat.add(last);
                last = "";
            } else {
                if (last.length() > 0) {
                    last += " ";
                }
                last += line;
            }
        }
        if (last.length() > 0) {
            reformat.add(last);
        }

        for (String line : reformat) {
            while (line.length() > 0) {
                if (line.length() <= useWidth) {
                    output.add(line);
                    line = "";
                } else {
                    int stop = line.lastIndexOf(' ', useWidth);
                    output.add(line.substring(0, stop));
                    line = line.substring(stop + 1);
                }
                if (output.size() == topHeight) {
                    useWidth = mainWidth;
                }
            }
        }
        return output;
    }

    private boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    private List<String> normalizeList(List<String> l) {
        if (l == null) {
            return l;
        }
        l = new ArrayList<String>(l);
        for (Iterator<String> i = l.iterator(); i.hasNext();) {
            String value = i.next();
            if (isEmpty(value)) {
                i.remove();
            }
        }
        return l;
    }
}
