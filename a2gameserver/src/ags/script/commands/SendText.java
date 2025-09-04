package ags.script.commands;

import ags.communication.GenericHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 *
 * @author blurry
 */
public class SendText extends AbstractCommand {
    
    /** Creates a new instance of SendText */
    public SendText() {
    }

    String message = "";
    boolean cr = true;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length < 2 || args.length > 3)
            throw new InitalizationException("SendText only expects one argument, the message to echo to the screen -- a second argmenut will prevent it from sending carriage return at the end of the line");
        if (args.length > 1) message = args[1];
        if (args.length > 2) cr = false;        
        trackVariableDependencies(message);
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost host = GenericHost.getInstance();
        try {
            if (cr) host.writeParanoid(translateValue(message) + "\r");
            else host.writeParanoid(translateValue(message));
        } catch (IOException ex) {
            throw new FatalScriptException("Error when sending text "+ex.getMessage(), ex);
        }
    }    
}