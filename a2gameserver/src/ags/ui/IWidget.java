/*
 * IWidget.java
 *
 * Created on May 19, 2006, 5:37 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.ui;

/**
 * Abstract representation of a (possibly) interactive widget
 * @author blurry
 */
public abstract class IWidget {
    /**
     * Application this widget belongs to
     */
    protected IApplication app;
    /**
     * Is this the active widget?
     */
    private boolean active;
    /**
     * Left-most x coordinate
     */
    int x;
    /**
     * upper y-coordinate
     */
    int y;
    /**
     * Widget width
     */
    int xSize;
    /**
     * Widget height
     */
    int ySize;
    
    /**
     * Constructor method - set current application
     * @param a Current application using this widget
     */
    public IWidget(IApplication a) {
        app=a;
    }
    
    /**
     * Move this widget to the top
     */
    public void moveToTop() {
        app.moveToTop(this);
    }

    /**
     * Is this widget active?
     * @return active property value
     */
    public boolean isActive() {
        return (this.active);
    }
    /**
     * Setter for active property
     * @param active active property value
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Destroy this widget
     */
    public void destroy() {
        app.removeWidget(this);
    }
    
    /**
     * Try to handle a keypress
     * @param b Key pressed
     * @return True if keypress was handled, false if key was not understood
     */
    abstract public boolean handleKeypress(byte b);
    /**
     * Draw this widget to the virtual framebuffer
     */
    abstract public void redraw();

    /**
     * Getter for x coodinate property
     * @return x coodinate property
     */
    public int getX() {
        return x;
    }

    /**
     * setter for x coodinate property
     * @param x x coodinate property
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * Getter for y coodinate property
     * @return y coodinate property
     */
    public int getY() {
        return y;
    }

    /**
     * Setter for y coodinate property
     * @param y y coodinate property
     */
    public void setY(int y) {
        this.y = y;
    }

    /**
     * Getter for widget width
     * @return widget width
     */
    public int getXSize() {
        return xSize;
    }

    /**
     * Setter for widget width
     * @param xSize widget width
     */
    public void setXSize(int xSize) {
        this.xSize = xSize;
    }

    /**
     * Getter for widget height
     * @return widget height
     */
    public int getYSize() {
        return ySize;
    }

    /**
     * Setter for widget height
     * @param ySize widget height
     */
    public void setYSize(int ySize) {
        this.ySize = ySize;
    }
}