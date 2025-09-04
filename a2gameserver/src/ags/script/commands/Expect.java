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
public class Expect extends AbstractCommand {
    
    /** Creates a new instance of Expect */
    public Expect() {
    }

    String message = "";
    int timeout = 1000;
    boolean noConversion = false;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length > 4 || args.length < 2)
            throw new InitalizationException("Expectonly expects up to three arguments:" +
                    "the message to expect from the remote host (required), " +
                    "the time to wait for the response in milliseconds (default is 1000), " +
                    "and if the value should be translated from apple hi-order (false, default) or treated as-is (true)");
        if (args.length > 1) message = args[1];
        if (args.length > 2) 
            try {
                timeout = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                throw new InitalizationException("Value "+args[1]+" is not a valid integer!");
            }
        if (args.length > 3) noConversion = getBoolean(args[3], false);
        
        trackVariableDependencies(message);
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        try {
            GenericHost.getInstance().expect(translateValue(message), timeout, noConversion);
        } catch (IOException ex) {
            throw new FatalScriptException("Error when processing expect command.  Expected string: "+translateValue(message), ex);
        }
    }    
}
