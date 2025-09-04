/*
 * Target.java
 *
 * Created on October 18, 2006, 4:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.script;

import ags.script.commands.Require;
import ags.script.exception.FatalScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Target is a list of commands to execute.  Those commands might also invoke other targets (see the Require command)
 * @author brobert, vps
 */
public class Target {
    /**
     * Map of all defined targets
     */
    private static Map allTargets;
    static {allTargets = new HashMap();}

    /**
     * Get a registered target by name
     * @param name Name of target to get
     * @return null if target does not exist, otherwise relevant Target with the provided name
     */
    public static Target getTarget(String name) {
        Target t = (Target) allTargets.get(name);
        return t;
    }

    /**
     * Verify all variables used within this target are defined properly by a set command somewhere
     * @throws com.vignette.vps.install.BadVariableValueException If there are references to undefined variables
     */
    static void verifyAll() throws BadVariableValueException {
        for (Iterator i = allTargets.values().iterator(); i.hasNext(); ) {
            Target t = (Target) i.next();
            for (Iterator j = t.getCommands().iterator(); j.hasNext(); ) {
                AbstractCommand c = (AbstractCommand) j.next();
                for (Iterator k = c.getRequiredVariables().iterator(); k.hasNext(); ) {
                    Variable v = (Variable) k.next();
                    if (!v.isAccountedFor()) {
                        throw new BadVariableValueException(new NullPointerException("Line "+c.getLineNumber()+" refers to a variable named "+v.getName()+" which is not initalized anywhere in the script!"));
                    }
                }
// Let's skip this for now -- it's probably less user-friendly if you do this!                
//                c.checkPaths();
            }
        }
    }

    //---------------------------------
    
    /**
     * target name
     */
    private String name;
    /**
     * target commands
     */
    private List commands;
    /**
     * target was run at least once already
     */
    private boolean runAlready;

    /**
     * Creates a new instance of Target and registers it automatically
     * @param name Name of target to create
     */
    @SuppressWarnings("unchecked")
    public Target(String name) {
        this.name=name;
        runAlready = false;
        commands = new ArrayList();
        allTargets.put(name, this);
    }    

    /**
     * Get the name of this target
     * @return This target's name
     */
    public String getName() {
        return name;
    }

    /**
     * Get list of commands in this target, in the order they appear in the script
     * @return List of commands
     */
    public List getCommands() {
        return commands;
    }

    /**
     * Was this target run already?
     * @return True if the target has already been executed
     */
    public boolean isRunAlready() {
        return runAlready;
    }
    
    /**
     * Require will execute the commands in this target only once.  Once the target has already been called, subsequent calls to Require will do nothing.
     * (this was inspired by Ant, can you tell?)
     * @throws java.lang.Exception If there was a fatal exception when processing this target
     */
    public void require() throws Exception {
        if (runAlready) return;
        call();
    }
    
    /**
     * Call processes the commands in this target every time it is invoked.
     * Before the target is executed, each command is checked to ensure that required variables are defined in the script somewhere.
     * This helps catch errant typos in the script before it is too late.
     * @throws java.lang.Exception If there was a fatal error executing the commands in this target.
     */
    public void call() throws Exception {
        runAlready = true;
        Engine.getLogOut().println("/* running target "+name+" */");
        for (Iterator i=commands.iterator(); i.hasNext(); ) {
            AbstractCommand command = (AbstractCommand) i.next();
            try {
                Engine.getLogOut().println("/* executing line "+command.getLineNumber()+" */");
                command.execute();
                if (Engine.getInstance().getGotoNext() != null) {
                    Target t = Engine.getInstance().getGotoNext();
                    Engine.getInstance().setGotoNext(null);
                    t.call();
                    return;
                }
            } catch (FatalScriptException ex) {
                if (Engine.getInstance().getErrorHandler() != null) {
                    Engine.getOut().println("Error in line "+command.getLineNumber()+": "+ex.getMessage());
                    Engine.getInstance().getErrorHandler().call();
                    return;
                } else {
                    if (! (command instanceof Require)) {
                        Engine.getOut().println("Error in line "+command.getLineNumber()+": "+ex.getMessage());
                    }
                    throw ex;
                }
            }
        }
    }
}