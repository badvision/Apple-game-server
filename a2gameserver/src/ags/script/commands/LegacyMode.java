/*
 * EchoCheck.java
 *
 * Created on December 6, 2006, 10:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package ags.script.commands;

import ags.communication.GenericHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;

/**
 *
 * @author brobert
 */
public class LegacyMode extends AbstractCommand {
    
    /** Creates a new instance of EchoCheck */
    public LegacyMode() {
    }
    
    boolean legacyModeValue = false;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("LegacyMode takes one parameter: yes|y|true|1 or no|n|false|0;");
        legacyModeValue = getBoolean(args[1], true);
    }
    
    public void checkPaths() throws BadVariableValueException {
    }
    
    protected void doExecute() throws FatalScriptException {
        GenericHost.getInstance().setLegacyMode(legacyModeValue);
        Engine.getOut().println("Setting legacy mode to "+legacyModeValue);
    }
}
