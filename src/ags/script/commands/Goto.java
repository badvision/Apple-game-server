package ags.script.commands;

import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.Target;
import ags.script.exception.FatalScriptException;

/**
 *
 * @author Administrator
 */
public class Goto extends AbstractCommand {
    
    /** Creates a new instance of Goto */
    public Goto() {
    }

    String targetName;
    Target target;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("Goto expects one argument, the name of the target to jump to");
        targetName = args[1];
    }

    public void checkPaths() throws BadVariableValueException {
        target = Target.getTarget(targetName);
        if (target == null) 
            throw new BadVariableValueException(new Exception("Cannot find target named "+targetName));
    }

    protected void doExecute() throws FatalScriptException {
        Engine.getInstance().setGotoNext(target);
    }    
}