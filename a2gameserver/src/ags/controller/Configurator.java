/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.controller;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 *
 * @author brobert
 */
public class Configurator {

    /**
     * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
     *
     * @param packageName The base package
     * @return The classes
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private static Class[] getClasses(String packageName)
            throws ClassNotFoundException, IOException {
//        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        assert classLoader != null;
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<File>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<Class>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        return classes.toArray(new Class[classes.size()]);
    }

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
    private static List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        if (!directory.exists()) {
            String path = directory.getPath();
            if (path.toLowerCase().contains(".jar!")) {
                path = path.substring(path.indexOf(':') + 1, path.lastIndexOf('!'));
                return findClassInJar(new File(path));
            }
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    private static List<Class> findClassInJar(File jarFile) {
        List<Class> classes = new ArrayList<Class>();
        try {
            JarFile jar = new JarFile(jarFile, false);
            Enumeration e = jar.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = (JarEntry) e.nextElement();
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6);
                    name = name.replace('/', '.');
                    classes.add(Class.forName(name));
                }
            }

        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return classes;
    }

    public static List<Field> findVariables() {
        List<Field> out = new ArrayList<Field>();
        try {
            Class[] allClasses = getClasses("ags");
            for (Class c : allClasses) {
                for (Field f : c.getDeclaredFields()) {
                    Configurable annotation = getAnnotation(f);
                    if (annotation != null) {
                        out.add(f);
                    }
                }
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static void setVariable(Field f, String value) {
//        System.out.println("Called setVariable with "+f.getName()+" and value "+value);
        Configurable annotation = getAnnotation(f);
        if (annotation == null) {
            return;
        }
        try {
            if (f.getType().equals(Boolean.class) || f.getType().equals(boolean.class)) {
                boolean b = Boolean.parseBoolean(value);
                f.setBoolean(null, b);
            } else if (f.getType().equals(Integer.class) || f.getType().equals(int.class)) {
                int i = Integer.parseInt(value);
                f.setInt(null, i);
            } else if (f.getType().equals(String.class)) {
                f.set(null, String.valueOf(value));
            } else if (f.getType().isEnum()) {
                f.set(null, Enum.valueOf((Class<? extends Enum>) f.getType(), value));
            }
        } catch (IllegalAccessException ex) {
            Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void setVariable(Field field, Object value) {
        setVariable(field, String.valueOf(value));
    }

    public static Configurable getAnnotation(Field f) {
        Configurable annotation = f.getAnnotation(Configurable.class);
        return annotation;
    }

    public static String getName(Field var) {
        return var.getDeclaringClass().getCanonicalName() + "." + var.getName();
    }

    public static void main(String[] args) {
        List<Field> vars = findVariables();
        for (Field var : vars) {
            System.out.println("Field " + getName(var));
        }
    }

    // Capture object default values before they're modified by anything
    public static Map<Field, Object> DEFAULT_VALUES;


    static {
        DEFAULT_VALUES = new HashMap<Field, Object>();
        for (Field f : findVariables()) {
            try {
                Object defaultValue = f.get(null);
                DEFAULT_VALUES.put(f, defaultValue);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NullPointerException ex) {
                // Do nothing...
            }
        }
    }

    /**
     * Set configurable variables back to their default (code) values
     */
    public static void revertDefaults() {
        for (Field f : findVariables()) {
            try {
                f.set(null, DEFAULT_VALUES.get(f));
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void loadValues() {
        Preferences prefs = Preferences.userNodeForPackage(Configurator.class);
        for (Field f : findVariables()) {
            String value = prefs.get(getName(f), null);
            if (value != null) {
                setVariable(f, value);
            }
        }
    }

    public static void saveValues() {
        Preferences prefs = Preferences.userNodeForPackage(Configurator.class);
        for (Field f : findVariables()) {
            Object currentValue = null;
            try {
                currentValue = f.get(null);
            } catch (IllegalArgumentException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(Configurator.class.getName()).log(Level.SEVERE, null, ex);
            }
            boolean saveValue = true;
            if ((currentValue == null) && (DEFAULT_VALUES.get(f) == null)) {
                saveValue = false;
            } else if (currentValue.equals(DEFAULT_VALUES.get(f))) {
                saveValue = false;
            }
            if (saveValue) {
                prefs.put(getName(f), String.valueOf(currentValue));
            } else {
                prefs.remove(getName(f));
            }
        }
    }
}