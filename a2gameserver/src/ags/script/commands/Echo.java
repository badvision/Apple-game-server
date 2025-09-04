package ags.script.commands;

import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;

/**
 *
 * @author Administrator
 */
public class Echo extends AbstractCommand {
    
    /** Creates a new instance of Echo */
    public Echo() {
    }

    String message = "";
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("Echo only expects one argument, the message to echo to the screen");
        message = args[1];
        trackVariableDependencies(message);
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        Engine.getOut().println(translateValue(message));
    }    
}
