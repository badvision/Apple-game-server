/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.ui.host;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author brobert
 */
public class Style {

    public static void applyDefaults() {

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Style.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(Style.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Style.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(Style.class.getName()).log(Level.SEVERE, null, ex);
        }

        UIManager.put("TabbedPane.highlight", Color.GREEN);
        UIManager.put("TabbedPane.foreground", Color.GREEN);
        UIManager.put("TabbedPane.background", Color.BLACK);
        UIManager.put("TabbedPane.light", Color.BLUE);
        UIManager.put("TabbedPane.shadow", Color.BLUE);
        UIManager.put("TabbedPane.focus", Color.WHITE);
        UIManager.put("TabbedPane.selectHighlight", Color.WHITE);
        UIManager.put("TabbedPane.selectedForeground", Color.RED);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentOpaque", Boolean.FALSE);
        UIManager.put("TabbedPane.tabsOpaque", Boolean.TRUE);
        UIManager.put("TextField.caretForeground", Color.WHITE);
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        UIManager.put("ComboBox.selectionBackground", Color.GREEN);
        UIManager.put("LabelUI", "javax.swing.plaf.basic.BasicLabelUI");
        UIManager.put("Label.foreground", Color.GREEN);
        UIManager.put("Label.font", FONT.APPLE2.font);
        UIManager.put("Panel.background", Color.BLACK);
        UIManager.put("OptionPane.messageForeground", Color.ORANGE);
        UIManager.put("OptionPane.background", Color.BLACK);
        UIManager.put("Button.background", Color.GREEN);
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.font", FONT.APPLE2FAT.font);
    }

    public static void apply(Component component) {
        component.setBackground(Color.BLACK);
        component.setForeground(Color.GREEN);
        component.setFont(FONT.APPLE2.font);
    }

    public static void apply(Container container) {
        apply((Component) container);
        for (Component c : container.getComponents()) {
            apply(c);
        }
    }
    public static String A2_TTF = "/ags/resources/a2like.ttf";

    public enum FONT {

        APPLE2(readFont(A2_TTF).deriveFont(Font.PLAIN, 16f), 1.0f),
        APPLE2FAT(readFont(A2_TTF).deriveFont(Font.PLAIN, 16f), 1.0f, true),
// Only works with JDK 1.6  :-(
//        DIALOG(new Font(Font.DIALOG, Font.BOLD, 9), 0.8f),
//        MONOSPACED(new Font(Font.MONOSPACED, Font.PLAIN, 9), 0.8f),
        DIALOG(new Font("Dialog", Font.BOLD, 9), 0.8f),
        MONOSPACED(new Font("Monospaced", Font.PLAIN, 9), 0.8f),
        ARIAL(new Font("Arial", Font.PLAIN, 9), 0.8f);
        public Font font;
        public float heightAdjust;
        public boolean isFat;

        FONT(Font useFont, float useHeightAdjust) {
            this(useFont, useHeightAdjust, false);
        }

        FONT(Font useFont, float useHeightAdjust, boolean useFat) {
            font = useFont;
            heightAdjust = useHeightAdjust;
            isFat = useFat;
        }
    };

    public static Font readFont(String fontName) {
        try {
            InputStream fontInputStream = Style.class.getResourceAsStream(fontName);
            if (fontInputStream == null) {
                System.err.println("Error reading font " + fontName);
            }
            Font base = Font.createFont(Font.TRUETYPE_FONT, fontInputStream);
            return base;
        } catch (FontFormatException ex) {
            Logger.getLogger(Style.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Style.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    ;
}
