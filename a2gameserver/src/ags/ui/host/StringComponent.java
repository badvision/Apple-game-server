/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.ui.host;

import ags.controller.Configurator;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextField;

/**
 *
 * @author brobert
 */
class StringComponent extends JTextField implements KeyListener {
    Field backingField;
    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
        Configurator.setVariable(backingField, getText());
    }

    public StringComponent(Field f) {
        backingField = f;
        synchronizeValue();
        addKeyListener(this);
    }

    public void synchronizeValue() {
        try {
            Object value = backingField.get(null);
            if (value == null) {
                setText("");
            } else {
                setText(String.valueOf(value));
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StringComponent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StringComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
