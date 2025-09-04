/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.game;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;

/**
 *
 * @author brobert
 */
public class GameUtil {

    public static String GAMES_DATA_FILE="games.xml";
    /**
     * File type for basic programs
     */
    public static final String TYPE_BASIC = "fc";
    /**
     * File type for executable binary programs
     */
    public static final String TYPE_BINARY = "06";
    /**
     * File type for disk images
     */
    public static final String TYPE_DISK = "disk";
    public static final String ACTION_SUB = "subroutine";
    public static final String ACTION_RUN = "run";
    public static final String ACTION_LOAD = "load";

    public static List<Game> readGames() {
        try {
            InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("data/" + GAMES_DATA_FILE);
            if (input == null) return null;
            javax.xml.bind.JAXBContext jaxbCtx = javax.xml.bind.JAXBContext.newInstance(Games.class);
            Games out = (Games) jaxbCtx.createUnmarshaller().unmarshal(input);
            return out.getGame();
        } catch (JAXBException ex) {
            Logger.getLogger(GameUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static int toInt(String value) {
        return Integer.parseInt(value, 16);
    }
}
