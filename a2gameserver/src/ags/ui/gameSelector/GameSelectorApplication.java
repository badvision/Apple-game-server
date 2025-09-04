/*
 * GameSelectorApplication.java
 *
 * Created on May 19, 2006, 10:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package ags.ui.gameSelector;

import ags.communication.TransferHost;
import ags.ui.*;
import java.io.IOException;
import java.util.List;
import ags.game.Game;

/**
 * Application that allows user to browse games and execute one
 * @author blurry
 */
public class GameSelectorApplication extends IApplication {

    public List<Game> games;

    /**
     * Constructor method
     * @param s Virtual framebuffer to use
     */
    public GameSelectorApplication(IVirtualScreen s) {
        super(s);
    }
    /**
     * Game information panel
     */
    GameInfoWidget info;
    /**
     * Search box (not used yet)
     */
    GameSearchWidget search;
    /**
     * Game results browser
     */
    GameResultsWidget results;

    /** Creates a new instance of GameSelectorApplication */
//    public GameSelectorApplication {}
    protected void init() {
        search = new GameSearchWidget(this);
        search.setX(1);
        search.setY(1);
        search.setXSize(38);
        search.setYSize(22);
        info = new GameInfoWidget(this);
        info.setX(1);
        info.setXSize(38);
        info.setY(1);
        info.setYSize(7);
        results = new GameResultsWidget(this);
        results.setX(1);
        results.setXSize(38);
        results.setY(10);
        results.setYSize(13);
//        addWidget(infoBar);
//        addWidget(search);
        addWidget(results);
        addWidget(info);
        moveToTop(results);
    }

    /**
     * Main loop: Wait for user to select game from results and then execute the game
     * @param host Transfer host used to communicate with the remote apple computer
     * @throws java.io.IOException If there was an unrecoverable communication problem
     */
    public void mainLoop(TransferHost host) throws IOException {
        redraw();
        getScreen().send(host);
        while (gameSelected() == null) {
            byte key = host.getKey();
            if (key != 0) {
                if (handleKeypress(key)) {
                    getScreen().send(host);
                }
            }
        }
        host.toggleSwitch(0x0c051); // Text
        host.toggleSwitch(0x0c054); // Page1
        host.startGame(gameSelected());
        System.out.println("Have fun playing " + gameSelected().getName() + "!");
    }

    /**
     * handle global keypress: None implemented yet
     * @param b Key pressed
     * @return false
     */
    @Override
    protected boolean handleGlobalKeypress(byte b) {
        if (b == Keyboard.KEY_TAB || b == '/') {
            IWidget newTop = visible.get(0);
            if (newTop == search) {
                newTop = visible.get(1);
            }
            moveToTop(newTop);
            return true;
        }
        if (b == Keyboard.KEY_ESCAPE) {
            if (info.isActive() || results.isActive()) {
                removeWidget(info);
                removeWidget(results);
                addWidget(search);
                search.show();
                moveToTop(search);
                return true;
            } else if (search.isActive()) {
                // ESC cancels search and shows all games
                setResults(games);
                removeWidget(search);
                search.hide();
                addWidget(info);
                addWidget(results);
                moveToTop(results);
                return true;
            }
        }
        if (b == Keyboard.KEY_RETURN && search.isActive()) {
            // Return accepts search and shows results
            removeWidget(search);
            search.hide();
            addWidget(info);
            addWidget(results);
            moveToTop(results);
            return true;
        }
        // Panic button forces screen redraw...
        if (b == '!') {
            getScreen().markBufferStale();
            return true;
        }
        return false;
    }

    /**
     * handle invalid keypress: Not implemented yet
     * @param b Key pressed
     * @return false
     */
    protected boolean handleInvalidKeypress(byte b) {
        return false;
    }

    /**
     * Set list of games to select from
     * @param games list of games to select from
     */
    public void setGames(List<Game> games) {
        this.games = games;
        setResults(games);
    }

    public void setResults(List<Game> games) {
        results.results = games;
        results.activeItem = 0;
        if (games != null && games.size() > 0) {
            GameInfoWidget.setActiveItem(games.get(0));
        }
    }

    /**
     * Get game selection
     * @return selected game or null if there was no selection yet
     */
    public Game gameSelected() {
        return results.selection;
    }
}