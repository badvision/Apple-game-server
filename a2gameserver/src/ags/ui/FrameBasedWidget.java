/*
 * FrameBasedWidget.java
 *
 * Created on May 19, 2006, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.ui;

/**
 * Abstract widget that is contained with a border/frame
 * @author blurry
 */
public abstract class FrameBasedWidget extends IWidget {
    
    /**
     * Constructor
     * @param a Application using this widget
     */
    public FrameBasedWidget(IApplication a) {
        super(a);
    }
    
    /**
     * Redraw the widget
     */
    public void redraw() {
        app.getScreen().drawBorder(getX(), getY(), getX()+getXSize()-1, getY()+getYSize()-1, isActive());
        app.getScreen().drawBox(getX(), getY(), getX()+getXSize()-1, getY()+getYSize()-1, false);
        redrawInside();
    }
    
    /**
     * Abstract method to draw the inside of the widget (subclass must implement)
     */
    abstract public void redrawInside();
}
