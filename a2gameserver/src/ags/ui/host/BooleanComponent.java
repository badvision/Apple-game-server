/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.ui.host;

import ags.controller.Configurator;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBox;

/**
 *
 * @author brobert
 */
class BooleanComponent extends JCheckBox implements ActionListener {
    Field backingField;

    public BooleanComponent(Field f) {
        backingField = f;
        synchronizeValue();
        addActionListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        Boolean value = Boolean.valueOf(isSelected());
        Configurator.setVariable(backingField, value);
    }

    public void synchronizeValue() {
        try {
            Object value = backingField.get(null);
            if (value == null) {
                setSelected(false);
            } else {
                setSelected(Boolean.valueOf(String.valueOf(value)).booleanValue());
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(BooleanComponent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(BooleanComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}