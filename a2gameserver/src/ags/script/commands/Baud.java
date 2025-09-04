package ags.script.commands;

import ags.communication.GenericHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 *
 * @author blurry
 */
public class Baud extends AbstractCommand {
    
    /** Creates a new instance of Baud */
    public Baud() {
    }

    int baud;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("Baud expects one argument, the baud rate");
        try {
            baud = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            throw new InitalizationException("Provided value is not a valid integet "+args[1]);
        }
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost host = GenericHost.getInstance();
        try {
            host.setBaud(baud);
            host.cancelLine();
            Engine.getOut().println("Local baud set to "+baud);
        } catch (IOException ex) {
            throw new FatalScriptException("Error when setting baud rate", ex);
        }
    }
    
}
