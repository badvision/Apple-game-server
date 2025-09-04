/*
 * Util.java
 *
 * Created on May 19, 2006, 10:53 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.ui;

/**
 * Misc. utility methods used in the UI
 * @author blurry
 */
public class Util {
    
    /** Creates a new instance of Util */
    private Util() {}

    /**
     * Repeat character n times
     * @param c Character to repeat
     * @param times number of times to repeat character
     * @return N repetitions of the character
     */
    static public String repeat(char c, int times) {
        String s = "";
       for (int i=0; i < times; i++) {
            s = s+c;
        }
        return s;
    }    
}
