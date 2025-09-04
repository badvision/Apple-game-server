package ags.script;

/**
 * This describes a fatal error that occurs when initalizing the installer
 * @author brobert, vps
 */
public class InitalizationException extends Exception {
    
    /**
     * Creates a new instance of FatalScriptException
     * 
     * @param message Error message to relay back to user
     */    
    public InitalizationException(String message) {
        super(message);
    }
}