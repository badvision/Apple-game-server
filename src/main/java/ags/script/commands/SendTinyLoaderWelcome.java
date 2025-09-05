package ags.script.commands;

import ags.communication.GenericHost;
import ags.communication.TransferHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 * SendTinyLoaderWelcome - Display welcome message on Apple II screen using TinyLoader
 * This serves as both a transfer test and user feedback
 * 
 * @author AGS
 */
public class SendTinyLoaderWelcome extends AbstractCommand {
    
    /** Creates a new instance of SendTinyLoaderWelcome */
    public SendTinyLoaderWelcome() {
    }
    
    protected void init(String[] args) throws InitalizationException {
        // No arguments needed for welcome message
        if (args.length != 1) {
            throw new InitalizationException("SendTinyLoaderWelcome takes no arguments");
        }
    }

    public void checkPaths() throws BadVariableValueException {
        // No file paths to check for welcome message
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost genericHost = GenericHost.getInstance();
        
        // Ensure we have a TransferHost instance for TinyLoader protocol
        if (!(genericHost instanceof TransferHost)) {
            throw new FatalScriptException("SendTinyLoaderWelcome requires TransferHost instance");
        }
        
        TransferHost host = (TransferHost) genericHost;
        
        try {
            System.out.println("SendTinyLoaderWelcome: Displaying welcome message and testing transfer");
            
            // Use TinyLoader to display welcome message on screen
            host.initTinyLoaderWithWelcome();
            
            System.out.println("SendTinyLoaderWelcome: Welcome message displayed successfully");
            
        } catch (IOException ex) {
            System.err.println("SendTinyLoaderWelcome failed with IOException: " + ex.getMessage());
            ex.printStackTrace();
            throw new FatalScriptException("Error displaying TinyLoader welcome message: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            System.err.println("SendTinyLoaderWelcome failed with unexpected exception: " + ex.getMessage());
            ex.printStackTrace();
            throw new FatalScriptException("Unexpected error in TinyLoader welcome message: " + ex.getMessage(), ex);
        }
    }    
}