/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.ui.host;

import ags.controller.Configurator;
import ags.controller.Launcher;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataListener;

/**
 * Serial port selection dropdown that dynamically enumerates available ports
 * 
 * @author brobert
 */
class SerialPortSelectComponent extends JComboBox implements ActionListener {
    Field backingField;

    public void actionPerformed(ActionEvent e) {
        Configurator.setVariable(backingField, getSelectedItem());
    }

    public void synchronizeValue() {
        try {
            Object value = backingField.get(null);
            if (value == null || value.toString().isEmpty()) {
                // If no value set, use the default
                String defaultPort = Launcher.getDefaultSerialPort();
                setSelectedItem(defaultPort);
                getModel().setSelectedItem(defaultPort);
            } else {
                setSelectedItem(value.toString());
                getModel().setSelectedItem(value.toString());
            }
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(StringComponent.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(StringComponent.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public SerialPortSelectComponent(final Field f) {
        backingField = f;
        addActionListener(this);
        setModel(new ComboBoxModel() {
            String[] availablePorts = Launcher.getAvailableSerialPorts();
            Object selectedValue = availablePorts.length > 0 ? availablePorts[0] : "";

            public void setSelectedItem(Object anItem) {
                selectedValue = anItem;
            }

            public Object getSelectedItem() {
                return selectedValue;
            }

            public int getSize() {
                return availablePorts.length;
            }

            public Object getElementAt(int index) {
                return availablePorts[index];
            }

            public void addListDataListener(ListDataListener l) {
            }

            public void removeListDataListener(ListDataListener l) {
            }
            
        });
        
        // Add tooltip with port information
        setToolTipText("Select the serial port to use for Apple II communication");
        
        synchronizeValue();
    }
}