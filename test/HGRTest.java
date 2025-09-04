/*
 * DHGRTest.java
 *
 * Created on June 23, 2006, 6:41 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */



import ags.ui.graphics.Convert;
import ags.ui.graphics.HGRImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/**
 *
 * @author Administrator
 */
public class HGRTest {
    // Path to image viewer utility, e.g. Blargg's apple_ntsc demo program
    static String IMAGE_VIEWER_CMD = "/home/brobert/Documents/Personal/blargg/dist/Debug/GNU-Linux-x86/blargg";

    public static void main(String[] args) throws IOException, InterruptedException {
        HGRImage i = new HGRImage();
        String sourceFile =
//          "/media/disk-3/apple2e/dev/apple_colors.gif";
//          "/home/brobert/Pictures/SELF.JPG";
          "/home/brobert/Pictures/CRW_1170_crop.jpg";
//          "/home/brobert/Pictures/vignette.jpg";
//          "/home/brobert/Pictures/logo_simple_bw.gif";
//          "/home/brobert/Pictures/ringo_small.gif";
//          "/home/brobert/Photos/2006/04/19/img_0072.jpg";
//          "/home/brobert/Photos/2008/05/17/IMG_0387 (Modified).JPG";
        i.readColorImage(sourceFile);
        //-----------------------------------
        // Write binary data to a file for transfer to an apple
        // This can be done through fishwings, ciderprsss or AGS.
//        File f = new File("/home/brobert/Desktop/yiq#062000");
//        OutputStream o = new FileOutputStream(f);
//        o.write(i.getAppleImage());
//        o.close();
        //-----------------------------------
        // Write out BW pattern to view
        // Put in same directory as source image
//        File outputDirectory = new File(sourceFile).getParentFile();
        // Put in tmp directory
        File outputDirectory = new File("/tmp");
        File testPattern = File.createTempFile("test_hgr_bw",".bmp", outputDirectory);
        BufferedImage testbw = new BufferedImage(560, 192*2, BufferedImage.TYPE_INT_RGB);
        Convert.convertHGRtoBWRaster(i.getAppleImage(), testbw);
        ImageIO.write(testbw, "bmp", testPattern);
        // Call the image viewer, passing the B&W image file path as a parameter
        Runtime.getRuntime().exec(new String[]{IMAGE_VIEWER_CMD, testPattern.getAbsolutePath()});

        // Tell java to remove BW file
        testPattern.deleteOnExit();
        // Give viewer app enough time to read file
        // otherwise program might exit and delte file too soon.
        Thread.sleep(1000);
    }
    /** Creates a new instance of DHGRTest */
    public HGRTest() {
    }
    
}