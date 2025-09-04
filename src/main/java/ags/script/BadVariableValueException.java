package ags.script;

/**
 * Signals that a variable is being set to a value that will cause errors down the line.
 * This is normally used to prevent the user from entering file or directory names that will break otherwise.
 * This is also used to flag a default value as inappropriate so the user can enter something more correct.
 * @author brober, vps
 */
public class BadVariableValueException extends Exception {
    
    /**
     * Creates a new instance of BadVariableValueException
     * @param cause Root cause of this error (file not found, usually)
     */
    public BadVariableValueException(Exception cause) {
        super(cause);
    }    
    
    /**
     * Get causal message of this error
     * @return Cause.getMessage()
     */
    public String getMessage() {
        return getCause().getMessage();
    }
}
