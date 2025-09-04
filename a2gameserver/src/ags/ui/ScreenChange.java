/*
 * ScreenChange.java
 *
 * Created on May 19, 2006, 8:34 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.ui;

/**
 * Javabean representing a consecutive number of bytes that has changed in the framebuffer during the last screen draw interval
 * @author blurry
 */
public class ScreenChange {
    /**
     * Starting offset of change
     */
    public int start;
    /**
     * length of change
     */
    public int length;
}
