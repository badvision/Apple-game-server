package ags.script.commands;

import ags.communication.GenericHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 *
 * @author Administrator
 */
public class SendMessage extends AbstractCommand {
    
    /** Creates a new instance of SendMessage */
    public SendMessage() {
    }

    String message = "";
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("SendMessage only expects one argument, the message to display on the target computer");
        message = args[1];
        trackVariableDependencies(message);
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost host = GenericHost.getInstance();
        try {
            host.cancelLine();
            host.writeSlowly(translateValue(message));
            host.cancelLine();
        } catch (IOException ex) {
            throw new FatalScriptException("Error when sending text "+ex.getMessage(), ex);
        }
    }    
}