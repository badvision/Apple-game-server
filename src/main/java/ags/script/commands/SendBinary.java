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
public class SendBinary extends AbstractCommand {
    
    /** Creates a new instance of SendBinary */
    public SendBinary() {
    }

    String file = "";
    String start = "";
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 3)
            throw new InitalizationException("SendBinary expects two arguments, the path of the binary file to send, and the target address");
        start = args[2];
        file = args[1];
        trackVariableDependencies(file);
        try {
            checkPaths();
        } catch (BadVariableValueException ex) {
            ex.printStackTrace(Engine.getLogOut());
            throw new InitalizationException(ex.getMessage());
        }
    }

    public void checkPaths() throws BadVariableValueException {
        verifyFile(file);
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost host = GenericHost.getInstance();
        try {
            host.typeHex(translateValue(file), start);
        } catch (IOException ex) {
            throw new FatalScriptException("Error when sending binary data", ex);
        }
    }    
}