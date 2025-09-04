package ags.script.commands;

import ags.communication.GenericHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;

/**
 *
 * @author Administrator
 */
public class ExpectPrompt extends AbstractCommand {
    
    /** Creates a new instance of ExpectPrompt */
    public ExpectPrompt() {
    }
    String prompt = "";
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("ExpectPrompt expects one argument, the prompt that is expected at the end of each SendMessage line henceforth.");
        prompt = args[0];
        if ("off".equalsIgnoreCase(prompt)) prompt = null;
        if ("false".equalsIgnoreCase(prompt)) prompt = null;
        else prompt = null;
        trackVariableDependencies(prompt);
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost.getInstance().setExpectedPrompt(translateValue(prompt));
    }
}