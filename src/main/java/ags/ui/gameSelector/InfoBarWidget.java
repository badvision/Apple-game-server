/*
 * InfoBarWidget.java
 *
 * Created on May 19, 2006, 10:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.ui.gameSelector;

import ags.ui.*;

/**
 * Display an information bar on the screen (not implemented)
 * @author blurry
 */
public class InfoBarWidget extends IWidget {
    
    /**
     * Creates a new instance of InfoBarWidget
     * @param a Application using this widget
     */
    public InfoBarWidget(IApplication a) {
        super(a);
    }

    /**
     * Handle keypress (not used for this type of widget)
     * @param b Key pressed
     * @return False
     */
    public boolean handleKeypress(byte b) {
        return false;
    }

    /**
     * Redraw this widget
     */
    public void redraw() {
    }
    
}
