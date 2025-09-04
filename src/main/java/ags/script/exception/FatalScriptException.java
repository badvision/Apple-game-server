package ags.script.exception;

/**
 * This describes a fatal error that occurs when executing the script
 * @author brobert, vps
 */
public class FatalScriptException extends Exception {
    
    /**
     * Creates a new instance of FatalScriptException
     * 
     * @param message Error message to relay back to user
     * @deprecated Really, the causal exceptions should always be tracked so that the stack traces make more sense
     */    
    public FatalScriptException(String message) {
        super(message);
    }

    /**
     * Creates a new instance of FatalScriptException
     * 
     * @param message Error message to relay back to user
     * @param cause The exception that generated the problem
     */    
    public FatalScriptException(String message, Throwable cause) {
        super(message, cause);
    }
}