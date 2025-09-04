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
public class SendChar extends AbstractCommand {
    
    /** Creates a new instance of SendBinary */
    public SendChar() {
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
        boolean oldEchoCheck = host.isEchoCheck();
        host.setEchoCheck(false);
        try {
            String m = translateValue(message);
            if (message.indexOf('^')==0) {
                char c = (char) (message.toUpperCase().charAt(1)-'A'+1);
                m = new String(new char[]{c});
            }
            host.writeParanoid(m);
        } catch (IOException ex) {
            throw new FatalScriptException("Error when sending text "+ex.getMessage(), ex);
        }
        host.setEchoCheck(oldEchoCheck);
    }    
}