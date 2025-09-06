/*
 * GameResultsWidget.java
 *
 * Created on May 19, 2006, 10:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ags.ui.gameSelector;

import ags.communication.GenericHost;
import ags.controller.Launcher;
import ags.ui.*;
import java.util.ArrayList;
import java.util.List;
import ags.game.Game;
import static ags.game.GameUtil.*;

/**
 * Framed widget that shows a list of games found and lets the user scroll the list and select a game
 * @author blurry
 */
public class GameResultsWidget extends FrameBasedWidget {

    /**
     * List of found results
     */
    List<Game> results;
    /**
     * Currently active (hilighted) item -- index in results list
     */
    int activeItem;
    /**
     * Selected game
     */
    Game selection;

    /**
     * Creates a new instance of GameResultsWidget
     * @param a GameSelector application
     */
    public GameResultsWidget(IApplication a) {
        super(a);
        selection = null;
        results = new ArrayList<Game>();
    }

    /**
     * Draws the game results
     */
    public void redrawInside() {
        if (selection == null) {
            String message = "Showing " + results.size() + " result";
            if (results.size() != 1) {
                message += "s";
            }
//            if (message.length() < getXSize())
//                message += Util.repeat(' ', getXSize()-message.length());
            app.getScreen().drawText((getXSize() - message.length()) / 2 + getX(), getY() - 1, message, true);
            message = "(Press ESC to search)";
            app.getScreen().drawText((getXSize() - message.length()) / 2 + getX(), getY()+getYSize(), message, true);
            int showStart = activeItem - getYSize() / 2;
            showStart = Math.max(0, showStart);
            showStart = Math.min(showStart, Math.max(0, results.size() - getYSize()));
            int currentIndex = showStart;
            for (int i = 0; i < getYSize() && currentIndex < results.size(); i++) {
                boolean hilight = (currentIndex == activeItem);
                String name = results.get(currentIndex).getName();
                if (results.get(currentIndex).getType().equalsIgnoreCase(TYPE_BASIC)) {
                    name += " (basic)";
                }
                if (results.get(currentIndex).getType().equalsIgnoreCase(TYPE_DISK)) {
                    name += " (disk)";
                }
//                if (selection == results.get(currentIndex)) name += " <-- LOADING!";
                app.getScreen().drawText(getX(), getY() + i, name, hilight);
                currentIndex++;
            }
            for (int i=getY(); i < getY()+getYSize(); i++) {
                app.getScreen().drawText(getX()+getXSize()-1, i, "|", false);
            }
            if (results.size() > 0) {
                int scrollSize = (getYSize() / results.size())+1;
                int scrollStart = getY() + (showStart * getYSize() / results.size());
                scrollStart = Math.min(scrollStart, getY()+ getYSize() - scrollSize-1);
                app.getScreen().drawBox(getX() + getXSize() - 1, scrollStart, getX() + getXSize()-1, scrollStart + scrollSize, true);
            }
        } else {
            // Use starburst loading screen if we have TextScreen40, otherwise use original
            if (app.getScreen() instanceof TextScreen40) {
                ((TextScreen40)app.getScreen()).drawLoadingScreen("LOADING GAME", selection.getName());
            } else {
                app.getScreen().drawText(getX(), getY(), "--> Loading game <--", true);
                app.getScreen().drawText(getX(), getY() + 1, selection.getName(), false);
                app.getScreen().drawText(getX(), getY() + 2, "Starting Address: " + selection.getStart(), false);
            }
        }
    }

    /**
     * Process arrow and enter keypresses to navigate and/or select items
     * @param b key pressed
     * @return false if keypress not understood
     */
    public boolean handleKeypress(byte b) {
        boolean legacy = Launcher.MACHINE_TYPE == Launcher.MACHINE_TYPES.Apple2;
        if (b == Keyboard.KEY_UP || (legacy && b == Keyboard.KEY_LEFT)) {
            activeItem--;
        } else if (b == Keyboard.KEY_DOWN || (legacy && b == Keyboard.KEY_RIGHT)) {
            activeItem++;
        } else if (b == Keyboard.KEY_RETURN) {
            executeSelection();
        } else if (b == '[' || (!legacy && b == Keyboard.KEY_LEFT)) {
            activeItem -= (getYSize() - 1) / 2;
        } else if (b == ']' || (!legacy && b == Keyboard.KEY_RIGHT)) {
            activeItem += (getYSize() - 1) / 2;
        } else if ((b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z')) {
            if (b >= 'a') {
                b -= 32;
            }
            activeItem = 0;
            for (Game g : results) {
                if (g.getName().toUpperCase().charAt(0) >= b) {
                    break;
                }
                activeItem++;
            }
        } else {
            return false;
        }
        activeItem = Math.max(0, activeItem);
        activeItem = Math.min(activeItem, Math.max(0, results.size() - 1));
        GameInfoWidget.setActiveItem(results.get(activeItem));
        return true;
    }

    /**
     * Signifies that a game has been selected and should be executed
     */
    public void executeSelection() {
        selection = results.get(activeItem);
    }
}