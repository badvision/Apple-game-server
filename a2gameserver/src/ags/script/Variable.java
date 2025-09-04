/*
 * Variable.java
 *
 * Created on October 18, 2006, 4:11 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A script variable.  These are created by using the SetVariable ("set") command and used via variable expansion within arguments passed to any number of commands.
 * @author brobert, vps
 */
public class Variable {
    /**
     * All variables defined in the script
     */
    private static Map allVariables;
    static {allVariables = new HashMap();}
    /**
     * Get a variable by its name.  If the variable has not been defined yet, it is created automatically.
     * If a variable is not tied to a corresponding Set command in the script, it will generate an error condition ahead of time rather than fail during script execution.
     * @param varName Name of variable to retrieve/create
     * @return Variable object
     */
    static public Variable getVariable(String varName) {
        Variable var = (Variable) allVariables.get(varName);
        if (var == null) {
            var = new Variable(varName);
        }
        return var;
    }
    
    /**
     * variable name
     */
    private String name;
    /**
     * variable description
     */
    private String description;
    /**
     * variable value
     */
    private String value;
    /**
     * was variable inialized by a set command?
     */
    private boolean initalized;
    /**
     * was this variable accounted for by a set command?
     */
    private boolean accountedFor;
    /**
     * list of commands that use this variable
     */
    private List dependencies;
    
    /**
     * Creates a new instance of Variable
     * @param name Variable name
     */
    public Variable(String name) {
        accountedFor = false;
        initalized = false;
        this.name = name;
        dependencies = new ArrayList();
        allVariables.put(name, this);
    }

    /**
     * Get the variable name
     * @return variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of the variable (if any)
     * @return Variable name
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the variable description
     * @param description New variable description to use
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the variable value
     * @return The variable's value
     */
    public String getValue() {
        return value;
    }

    /**
     * Set the variable value and verify it won't break anything (using checkDependencies)
     * @param value new value to consider
     * @throws com.vignette.vps.install.BadVariableValueException If the variable value is unacceptable
     * @see #checkDependencies()
     */
    public void setValue(String value) throws BadVariableValueException {
        this.value = value;
        try {
            checkDependencies();
        } catch (BadVariableValueException ex) {
            initalized=false;
            this.value=null;
            throw ex;
        }
    }

    /**
     * Was this variable properly initalized?  If so, it is considered as a canidate for variable expansion wherever it is used.  If not, it will not be used for variable expansion just yet.  This prevents unnecessary errors when setting other variables in the chain.
     * @return True if this variable has been initalized with its final value
     */
    public boolean isInitalized() {
        return initalized;
    }

    /**
     * Indicate the variable has been initalized
     * @param initalized New value for the initalized flag
     */
    public void setInitalized(boolean initalized) {
        this.initalized = initalized;
    }

    /**
     * Track a command that uses this variable for variable expansion -- this is used later by CheckDependencies when SetValue is called.
     * @param command Command to add as a dependency
     */
    public void addDependency(AbstractCommand command) {
        dependencies.add(command);
    }
    
    /**
     * Iterate through all commands that are dependent on this variable and call their CheckPaths methods to see if there are any errors with this value.
     * @throws com.vignette.vps.install.BadVariableValueException If one or more commands fail because of this variable's new value
     */
    public void checkDependencies() throws BadVariableValueException {
        for (Iterator i=dependencies.iterator(); i.hasNext(); ) {
            AbstractCommand command = (AbstractCommand) i.next();
            command.checkPaths();
        }
    }

    /**
     * Is this variable accounted for?  Meaning: Is there a set command that will initalize the value of this variable?
     * @return True if the variable has a corresponding set command in the script
     * False if the variable is the result of an errant typo in the script
     */
    public boolean isAccountedFor() {
        return accountedFor;
    }

    /**
     * Change the status of the accountedFor flag
     * @param accountedFor new accountedFor flag value
     */
    public void setAccountedFor(boolean accountedFor) {
        this.accountedFor = accountedFor;
    }
}