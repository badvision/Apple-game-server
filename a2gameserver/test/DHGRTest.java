/*
 * DHGRTest.java
 *
 * Created on June 23, 2006, 6:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */



import ags.ui.graphics.DHGR2Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Administrator
 */
public class DHGRTest {
    public static void main(String[] args) throws IOException {
        DHGR2Image i = new DHGR2Image();
        i.mode = 1;
//        i.readColorImage("/apple2e/dev/apple_colors.gif");
        i.readColorImage("/desktop/131 colors/billie.gif");
//        i.readColorImage("/desktop/131 colors/2infinitum2.gif");
//        i.readColorImage("/desktop/131 colors/apple-logo.gif");
        File f = new File("/apple2e/mask_131#062000");
        OutputStream o = new FileOutputStream(f);
        o.write(i.getAppleImage(0),0, 8192);
        o.write(i.getAppleImage(1),0, 8192);
        o.write(i.getAppleImage(0),8192, 8192);
        o.write(i.getAppleImage(1),8192, 8192);
        o.close();
    }
    /** Creates a new instance of DHGRTest */
    public DHGRTest() {
    }
    
}
