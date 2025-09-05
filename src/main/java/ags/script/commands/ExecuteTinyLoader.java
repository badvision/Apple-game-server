package ags.script.commands;

import ags.communication.GenericHost;
import ags.communication.TransferHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 * ExecuteTinyLoader - Execute code at a specified address using TinyLoader
 * 
 * @author AGS
 */
public class ExecuteTinyLoader extends AbstractCommand {
    
    /** Creates a new instance of ExecuteTinyLoader */
    public ExecuteTinyLoader() {
    }

    String address = "";
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("ExecuteTinyLoader expects one argument, the execution address in hex");
        address = args[1];
    }

    public void checkPaths() throws BadVariableValueException {
        // No file paths to check for execution command
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost genericHost = GenericHost.getInstance();
        
        // Ensure we have a TransferHost instance for TinyLoader protocol
        if (!(genericHost instanceof TransferHost)) {
            throw new FatalScriptException("ExecuteTinyLoader requires TransferHost instance");
        }
        
        TransferHost host = (TransferHost) genericHost;
        
        try {
            int execAddress = Integer.parseInt(address, 16);
            
            System.out.println("ExecuteTinyLoader: Executing at $" + address);
            
            // Use TinyLoader to execute code at address
            host.executeTinyLoader(execAddress);
            
            System.out.println("ExecuteTinyLoader: Execution command sent successfully");
            
        } catch (IOException ex) {
            throw new FatalScriptException("Error executing TinyLoader command", ex);
        } catch (NumberFormatException ex) {
            throw new FatalScriptException("Invalid address format: " + address, ex);
        }
    }    
}