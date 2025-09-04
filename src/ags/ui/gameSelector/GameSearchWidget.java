/*
 * GameSearchWidget.java
 *
 * Created on May 19, 2006, 10:17 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ags.ui.gameSelector;

import ags.controller.Launcher;
import ags.game.Game;
import ags.ui.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Search box to find games (not implemented yet)
 * @author blurry
 */
public class GameSearchWidget extends FrameBasedWidget {

    public static final String SELECT_CONTAINS = "Contains";
    public static final String SELECT_STARTS_WITH = "Starts with";
    public static final String SELECT_ENDS_WITH = "Ends with";
    public static final String SELECT_EQUALS = "Equals";
    public static final String SELECT_BEFORE = "Before";
    public static final String SELECT_AFTER = "After";
    public static final String SELECT_ANY = "Any";
    public static final String SELECT_YES = "Yes";
    public static final String SELECT_NO = "No";
    List<IWidget> searchWidgets;
    SelectWidget titleType;
    TextWidget titleText;
    SelectWidget authorType;
    TextWidget authorText;
    SelectWidget yearType;
    TextWidget yearText;
    SelectWidget keyboard;
    SelectWidget joystick;
    SelectWidget mouse;
    List<Game> results;

    /**
     * Creates a new instance of GameSearchWidget
     */
    public GameSearchWidget(IApplication a) {
        super(a);
        searchWidgets = new ArrayList<IWidget>();
        titleType = new SelectWidget(a);
        titleType.setX(9);
        titleType.setY(3);
        titleType.setOptions(SELECT_CONTAINS, SELECT_STARTS_WITH, SELECT_ENDS_WITH);
        titleText = new TextWidget(a);
        titleText.setX(23);
        titleText.setY(3);
        titleText.setXSize(15);
        authorType = new SelectWidget(a);
        authorType.setX(9);
        authorType.setY(6);
        authorType.setOptions(SELECT_CONTAINS, SELECT_STARTS_WITH, SELECT_ENDS_WITH);
        authorText = new TextWidget(a);
        authorText.setX(23);
        authorText.setY(6);
        authorText.setXSize(15);
        yearType = new SelectWidget(a);
        yearType.setX(9);
        yearType.setY(9);
        yearType.setOptions(SELECT_EQUALS, SELECT_BEFORE, SELECT_AFTER);
        yearText = new TextWidget(a);
        yearText.setX(23);
        yearText.setY(9);
        yearText.setXSize(5);
        keyboard = new SelectWidget(a);
        keyboard.setX(17);
        keyboard.setY(12);
        keyboard.setOptions(SELECT_ANY, SELECT_YES, SELECT_NO);
        joystick = new SelectWidget(a);
        joystick.setX(17);
        joystick.setY(15);
        joystick.setOptions(SELECT_ANY, SELECT_YES, SELECT_NO);
        mouse = new SelectWidget(a);
        mouse.setX(17);
        mouse.setY(18);
        mouse.setOptions(SELECT_ANY, SELECT_YES, SELECT_NO);
        searchWidgets.add(titleText);
        searchWidgets.add(authorType);
        searchWidgets.add(authorText);
        searchWidgets.add(yearType);
        searchWidgets.add(yearText);
        searchWidgets.add(keyboard);
        searchWidgets.add(joystick);
        searchWidgets.add(mouse);
        searchWidgets.add(titleType);
    }

    @Override
    public boolean isActive() {
        for (IWidget w : searchWidgets) {
            if (w.isActive()) {
                return true;
            }
        }
        return super.isActive();
    }

    @Override
    public void setActive(boolean active) {
        if (active) {
            app.moveToBottom(this);
        }
        super.setActive(active);
    }

    public void show() {
        for (IWidget w : searchWidgets) {
            app.addWidget(w);
        }
//        app.activate(titleText);
    }

    public void hide() {
        for (IWidget w : searchWidgets) {
            app.removeWidget(w);
        }

    }

    public void redrawInside() {
        processSearch();
        String title = "Game Search";
        app.getScreen().drawText(getX() + (getXSize() - title.length()) / 2, 0, title, true);
        if (results == null || results.size() == 0) {
            title = "No results found";
        } else {
            title = "Found " + results.size() + " results";
        }
//        app.getScreen().drawBox(1,1,38,22, false);
        app.getScreen().drawText(getX() + (getXSize() - title.length()) / 2, 23, title, true);
        app.getScreen().drawText(1, 3, " Title:", false);
        app.getScreen().drawText(1, 6, "Author:", false);
        app.getScreen().drawText(1, 9, "  Year:", false);
        app.getScreen().drawText(6, 12, " Keyboard:", false);
        app.getScreen().drawText(6, 15, " Joystick:", false);
        app.getScreen().drawText(6, 18, "    Mouse:", false);
        if (Launcher.MACHINE_TYPE == Launcher.MACHINE_TYPES.Apple2) {
            app.getScreen().drawText(22,11, "/ switches       ", false);
        } else {
            app.getScreen().drawText(22,11, "TAB switches     ", false);
        }
        app.getScreen().drawText(22,12, "between fields   ", false);
        app.getScreen().drawText(22,14, "ARROW KEYS move  ", false);
        app.getScreen().drawText(22,15, "inside fields    ", false);
        app.getScreen().drawText(22,17, "RETURN views     ", false);
        app.getScreen().drawText(22,18, "search results   ", false);
        app.getScreen().drawText(22,20, "ESC shows all,   ", false);
        app.getScreen().drawText(22,21, "cancelling search", false);
    }

    public boolean handleKeypress(byte b) {
        return false;
    }

    private void processSearch() {
        GameSelectorApplication a = (GameSelectorApplication) app;
        results = new ArrayList<Game>(a.games);
        String text = authorText.getValue().toUpperCase();
        if (text != null && !"".equals(text)) {
            List<Game> tempResults = new ArrayList<Game>(results);
            for (Game g : results) {
                if (g.getAuthor() == null || "".equals(g.getAuthor())) {
                    tempResults.remove(g);
                    continue;
                }
                String compare = g.getAuthor().toUpperCase();
                if (authorType.getCurrentOption().equals(SELECT_STARTS_WITH)) {
                    if (!(compare.startsWith(text))) {
                        tempResults.remove(g);
                    }
                }
                if (authorType.getCurrentOption().equals(SELECT_ENDS_WITH)) {
                    if (!(compare.endsWith(text))) {
                        tempResults.remove(g);
                    }
                }
                if (authorType.getCurrentOption().equals(SELECT_CONTAINS)) {
                    if (!(compare.contains(text))) {
                        tempResults.remove(g);
                    }
                }
            }
            results = tempResults;
        }

        text = titleText.getValue().toUpperCase();
        if (text != null && !"".equals(text)) {
            List<Game> tempResults = new ArrayList<Game>(results);
            for (Game g : results) {
                String compare = g.getName().toUpperCase();
                if (titleType.getCurrentOption().equals(SELECT_STARTS_WITH)) {
                    if (!(compare.startsWith(text))) {
                        tempResults.remove(g);
                    }
                }
                if (titleType.getCurrentOption().equals(SELECT_ENDS_WITH)) {
                    if (!(compare.endsWith(text))) {
                        tempResults.remove(g);
                    }
                }
                if (titleType.getCurrentOption().equals(SELECT_CONTAINS)) {
                    if (!(compare.contains(text))) {
                        tempResults.remove(g);
                    }
                }
            }
            results = tempResults;
        }

        text = yearText.getValue().toUpperCase();
        if (text != null && !"".equals(text)) {
            List<Game> tempResults = new ArrayList<Game>(results);
            for (Game g : results) {
                if (g.getYear() == null || "".equals(g.getYear())) {
                    tempResults.remove(g);
                    continue;
                }
                String compare = g.getYear().toUpperCase();
                if (yearType.getCurrentOption().equals(SELECT_BEFORE)) {
                    if (compare.compareTo(text) >= 0) {
                        tempResults.remove(g);
                    }
                }
                if (yearType.getCurrentOption().equals(SELECT_AFTER)) {
                    if (compare.compareTo(text) <= 0) {
                        tempResults.remove(g);
                    }
                }
                if (yearType.getCurrentOption().equals(SELECT_EQUALS)) {
                    if (compare.compareTo(text) != 0) {
                        tempResults.remove(g);
                    }
                }
            }
            results = tempResults;
        }

        if (!keyboard.getCurrentOption().equals(SELECT_ANY)) {
            List<Game> tempResults = new ArrayList<Game>(results);
            for (Game g : results) {
                if (g.isSupportsKeyboard() == null) {
                    tempResults.remove(g);
                    continue;
                }
                if (keyboard.getCurrentOption().equals(SELECT_YES)) {
                    if (!g.isSupportsKeyboard()) {
                        tempResults.remove(g);
                    }
                }
                if (keyboard.getCurrentOption().equals(SELECT_NO)) {
                    if (g.isSupportsKeyboard()) {
                        tempResults.remove(g);
                    }
                }
            }
            results = tempResults;
        }
        if (!joystick.getCurrentOption().equals(SELECT_ANY)) {
            List<Game> tempResults = new ArrayList<Game>(results);
            for (Game g : results) {
                if (g.isSupportsJoystick() == null) {
                    tempResults.remove(g);
                    continue;
                }
                if (joystick.getCurrentOption().equals(SELECT_YES)) {
                    if (!g.isSupportsJoystick()) {
                        tempResults.remove(g);
                    }
                }
                if (joystick.getCurrentOption().equals(SELECT_NO)) {
                    if (g.isSupportsJoystick()) {
                        tempResults.remove(g);
                    }
                }
            }
            results = tempResults;
        }
        if (!mouse.getCurrentOption().equals(SELECT_ANY)) {
            List<Game> tempResults = new ArrayList<Game>(results);
            for (Game g : results) {
                if (g.isSupportsMouse() == null) {
                    tempResults.remove(g);
                    continue;
                }
                if (mouse.getCurrentOption().equals(SELECT_YES)) {
                    if (!g.isSupportsMouse()) {
                        tempResults.remove(g);
                    }
                }
                if (mouse.getCurrentOption().equals(SELECT_NO)) {
                    if (g.isSupportsMouse()) {
                        tempResults.remove(g);
                    }
                }
            }
            results = tempResults;
        }
        a.setResults(results);
    }
}
