/*
 * ImageBuffer.java
 *
 * Created on June 24, 2006, 10:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.ui.graphics;

import java.io.IOException;

/**
 *
 * @author Administrator
 */
public interface ImageBuffer {
    void setTargetSize(int x, int y);
    
    byte[] getAppleImage();

    void plotColor(int x, int y, int color);
    
    void readColorImage(String file) throws IOException;
}
