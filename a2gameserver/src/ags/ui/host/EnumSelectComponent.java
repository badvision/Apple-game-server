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
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataListener;

/**
 *
 * @author brobert
 */
class EnumSelectComponent extends JComboBox implements ActionListener {
    Field backingField;

    public void actionPerformed(ActionEvent e) {
        Configurator.setVariable(backingField, getSelectedItem());
    }

    public void synchronizeValue() {
        try {
            Object value = backingField.get(null);
            if (value == null) {
                setSelectedItem(null);
                getModel().setSelectedItem(null);
            } else {
                setSelectedItem(value);
                getModel().setSelectedItem(value);
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StringComponent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StringComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public EnumSelectComponent(final Field f) {
        backingField = f;
        addActionListener(this);
        setModel(new ComboBoxModel() {
            Object[] values = f.getType().getEnumConstants();
            Object value = values[0];

            public void setSelectedItem(Object anItem) {
                value = anItem;
            }

            public Object getSelectedItem() {
                return value;
            }

            public int getSize() {
                return values.length;
            }

            public Object getElementAt(int index) {
                return values[index];
            }

            public void addListDataListener(ListDataListener l) {
            }

            public void removeListDataListener(ListDataListener l) {
            }
            
        });
        synchronizeValue();
    }

}
