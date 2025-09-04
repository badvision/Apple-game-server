/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.ui.host;

import ags.controller.Configurable;
import ags.controller.Configurable.CATEGORY;
import ags.controller.Configurator;
import ags.controller.FileType;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//import javax.swing.GroupLayout;
//import javax.swing.GroupLayout.Alignment;
//import javax.swing.GroupLayout.ParallelGroup;
//import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;

/**
 *
 * @author brobert
 */
public class ConfigPanel extends JFrame {

    public ConfigPanel() {
        generateForm();
//        Style.apply(this);
        validate();
//        setVisible(true);
    }

    private Component generateEditComponent(Field f) {
        if (f.getType().isEnum()) {
            return new EnumSelectComponent(f);
        }

        if (f.getType().equals(Boolean.TYPE)) {
            return new BooleanComponent(f);
        }
        if (f.getType().equals(Integer.TYPE)) {
            return new IntegerComponent(f);
        }
        if (f.getType().equals(String.class)) {
            if (f.getAnnotation(FileType.class) != null) {
                return new FileComponent(f);
            } else {
                return new StringComponent(f);
            }
        }

        return new JLabel("Unknown type: " + f.getType().getCanonicalName());
    }

    private void generateForm() {
        HashMap<CATEGORY, List<Field>> fields = new HashMap<CATEGORY, List<Field>>();
        JTabbedPane tabPane = new JTabbedPane();
        Style.apply(tabPane);

        List<Field> allFields = Configurator.findVariables();
        for (Field f : allFields) {
            Configurable annotation = Configurator.getAnnotation(f);
            CATEGORY cat = annotation.category();
            List<Field> catList = fields.get(cat);
            if (catList == null) {
                catList = new ArrayList<Field>();
                fields.put(cat, catList);
            }
            catList.add(f);
        }

        // Build tabs
        for (CATEGORY c : CATEGORY.values()) {
            JPanel categoryPanel = new JPanel();
            tabPane.add(c.name(), categoryPanel);

            GroupLayout layout = new GroupLayout(categoryPanel);
            categoryPanel.setLayout(layout);
//            layout.setAutocreateContainerGaps(true);
//            layout.setAutocreateGaps(true);
//            layout.setAutoCreateContainerGaps(true);
//            layout.setAutoCreateGaps(true);

            ParallelGroup horizontalGroup1 = layout.createParallelGroup();
            ParallelGroup horizontalGroup2 = layout.createParallelGroup();
            SequentialGroup verticalGroup = layout.createSequentialGroup();
            List<Field> fieldList = fields.get(c);
            if (fieldList == null) {
                System.err.println("Category " + c.name() + " came back with nothing");
            } else {
                for (Field f : fieldList) {
                    JLabel fieldLabel = new JLabel(f.getName());
                    Component editComponent = generateEditComponent(f);
                    Style.apply(fieldLabel);
                    Style.apply(editComponent);

//                verticalGroup.addGroup(layout.createParallelGroup(Alignment.BASELINE).
//                        addComponent(fieldLabel).
//                        addComponent(editComponent));
                    verticalGroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
                            addComponent(fieldLabel).
                            addComponent(editComponent));
//                horizontalGroup1.addComponent(fieldLabel);
//                horizontalGroup2.addComponent(editComponent);
                    horizontalGroup1.addComponent(fieldLabel);
                    horizontalGroup2.addComponent(editComponent);
                }
            }
            layout.setVerticalGroup(verticalGroup);
//            layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(horizontalGroup1).addGroup(horizontalGroup2));
            layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(horizontalGroup1).addGroup(horizontalGroup2));

            categoryPanel.validate();
            Style.apply(categoryPanel);
        }
        JPanel mainPanel = new JPanel();
        GroupLayout mainLayout = new GroupLayout(mainPanel);
//        setLayout(mainLayout);
        JButton saveChanges = new JButton("Save");
        saveChanges.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doSaveChanges();
            }
        });
        JButton cancelChanges = new JButton("Cancel");
        cancelChanges.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                doCancelChanges();
            }
        });

        mainLayout.setHorizontalGroup(mainLayout.createParallelGroup(GroupLayout.Alignment.CENTER).
                addComponent(tabPane).
                addGroup(mainLayout.createSequentialGroup().
                addComponent(saveChanges).
                addComponent(cancelChanges)));
        mainLayout.setVerticalGroup(mainLayout.createSequentialGroup().
                addComponent(tabPane).
                addGroup(mainLayout.createParallelGroup().
                addComponent(cancelChanges).
                addComponent(saveChanges)));
//        mainLayout.setHorizontalGroup(mainLayout.createParallelGroup(Alignment.CENTER).
//                addComponent(tabPane).
//                addGroup(mainLayout.createSequentialGroup().
//                    addComponent(saveChanges).
//                    addComponent(cancelChanges))
//                );
//        mainLayout.setVerticalGroup(mainLayout.createSequentialGroup().
//                addComponent(tabPane).
//                addGroup(mainLayout.createParallelGroup().
//                    addComponent(cancelChanges).
//                    addComponent(saveChanges)
//                ));
        Container c = getContentPane();
        c.add(mainPanel);
        setContentPane(c);
        final Dimension size = mainLayout.preferredLayoutSize(mainPanel);
        size.width += 20;
        size.height += 50;
        setPreferredSize(size);
    }

    @Override
    public void setVisible(boolean b) {
        try {
            super.setVisible(b);
            if (b) {
                setSize(getPreferredSize());
            }
        } catch (Throwable t) {
            System.out.println("Error when setting config panel visibility.");
            t.printStackTrace(System.err);
        }
    }

    private void doSaveChanges() {
        Configurator.saveValues();
        setVisible(false);
    }

    private void doCancelChanges() {
        Configurator.revertDefaults();
        setVisible(false);
    }

    public static void main(String[] args) {
        Style.applyDefaults();
        ConfigPanel test = new ConfigPanel();
        test.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        test.setVisible(true);
    }
}