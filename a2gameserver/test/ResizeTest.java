/*
 * DHGRTest.java
 *
 * Created on June 23, 2006, 6:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */



import ags.ui.graphics.DHGRImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Administrator
 */
public class ResizeTest {
    public static void main(String[] args) throws IOException {
        DHGRImage i = new DHGRImage();
        i.setTargetSize(35,48);
        i.readColorImage("/desktop/screens/cestlavie.gif");
        File f = new File("/desktop/screens/test#062000");
        OutputStream o = new FileOutputStream(f);
        o.write(i.getAppleImage());
        o.close();
    }
    /** Creates a new instance of DHGRTest */
    public ResizeTest() {
    }
    
}
