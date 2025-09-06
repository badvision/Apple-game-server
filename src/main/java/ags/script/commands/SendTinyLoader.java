package ags.script.commands;

import ags.communication.GenericHost;
import ags.communication.TransferHost;
import ags.communication.DataUtil;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.IOException;

/**
 * SendTinyLoader - Send binary data using the TinyLoader packet protocol
 * More resilient than the traditional hex-typing method
 * 
 * @author AGS
 */
public class SendTinyLoader extends AbstractCommand {
    
    /** Creates a new instance of SendTinyLoader */
    public SendTinyLoader() {
    }

    String file = "";
    String start = "";
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 3)
            throw new InitalizationException("SendTinyLoader expects two arguments, the path of the binary file to send, and the target address");
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
        GenericHost genericHost = GenericHost.getInstance();
        
        // Ensure we have a TransferHost instance for TinyLoader protocol
        if (!(genericHost instanceof TransferHost)) {
            throw new FatalScriptException("SendTinyLoader requires TransferHost instance");
        }
        
        TransferHost host = (TransferHost) genericHost;
        
        try {
            String translatedFile = translateValue(file);
            byte[] fileData = DataUtil.getFileAsBytes(translatedFile);
            int address = Integer.parseInt(start, 16);
            
            System.out.println("SendTinyLoader: Sending " + translatedFile + " to $" + start);
            
            // Use TinyLoader protocol for transfer
            int errors = host.sendRawDataTinyLoader(fileData, address);
            
            if (errors > 0) {
                System.out.println("SendTinyLoader: Transfer completed with " + errors + " errors");
            } else {
                System.out.println("SendTinyLoader: Transfer completed successfully");
            }
            
        } catch (IOException ex) {
            throw new FatalScriptException("Error when sending binary data via TinyLoader", ex);
        } catch (NumberFormatException ex) {
            throw new FatalScriptException("Invalid address format: " + start, ex);
        }
    }    
}