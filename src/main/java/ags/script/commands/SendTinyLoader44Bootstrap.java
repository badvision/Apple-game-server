package ags.script.commands;

import ags.communication.GenericHost;
import ags.communication.TransferHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 * SendTinyLoader44Bootstrap - Send 4+4 encoded TinyLoader bootstrap program
 * Much faster to type than hex, replaces initial hex typing phase
 * 
 * @author AGS
 */
public class SendTinyLoader44Bootstrap extends AbstractCommand {
    
    /** Creates a new instance of SendTinyLoader44Bootstrap */
    public SendTinyLoader44Bootstrap() {
    }
    
    String tinyLoaderPath = "ags/asm/tinyloader_{driver}.o";
    
    protected void init(String[] args) throws InitalizationException {
        // No arguments needed for 4+4 bootstrap - uses default TinyLoader path
        if (args.length != 1) {
            throw new InitalizationException("SendTinyLoader44Bootstrap takes no arguments");
        }
        
        // Track dependency on {driver} variable
        trackVariableDependencies(tinyLoaderPath);
        
        try {
            checkPaths();
        } catch (BadVariableValueException ex) {
            ex.printStackTrace(Engine.getLogOut());
            throw new InitalizationException(ex.getMessage());
        }
    }

    public void checkPaths() throws BadVariableValueException {
        verifyFile(tinyLoaderPath);
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost genericHost = GenericHost.getInstance();
        
        // Ensure we have a TransferHost instance for 4+4 encoding
        if (!(genericHost instanceof TransferHost)) {
            throw new FatalScriptException("SendTinyLoader44Bootstrap requires TransferHost instance");
        }
        
        TransferHost host = (TransferHost) genericHost;
        
        try {
            System.out.println("SendTinyLoader44Bootstrap: Sending 4+4 encoded TinyLoader bootstrap");
            
            // Resolve the {driver} variable to get actual file path  
            String resolvedPath = translateValue(tinyLoaderPath);
            
            // Send 4+4 encoded TinyLoader bootstrap program
            host.sendTinyLoader44Bootstrap(resolvedPath);
            
            System.out.println("SendTinyLoader44Bootstrap: 4+4 bootstrap sent successfully");
            
        } catch (IOException ex) {
            System.err.println("SendTinyLoader44Bootstrap failed with IOException: " + ex.getMessage());
            ex.printStackTrace();
            throw new FatalScriptException("Error sending 4+4 TinyLoader bootstrap: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            System.err.println("SendTinyLoader44Bootstrap failed with unexpected exception: " + ex.getMessage());
            ex.printStackTrace();
            throw new FatalScriptException("Unexpected error in 4+4 TinyLoader bootstrap: " + ex.getMessage(), ex);
        }
    }    
}