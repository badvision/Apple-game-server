package ags.script;

import ags.script.exception.FatalScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Definition of an abstract executable script command.  This provides a code skeleton to initalize, verify, and execute a script command.
 * There are also convienence methods used in various commands for user interaction and file verification.
 * @author brobert, vps
 */
public abstract class AbstractCommand {
    /**
     * Pattern used to identify variables during variable substitution, where a variable name is enclosed in curly-brackets {}
     */
    static public Pattern variablePattern;
    static {
        variablePattern = Pattern.compile("\\{(.*?)\\}");
    }
    
    /**
     * line number of this command
     */
    private int lineNumber=0;
    /**
     * variables used by this command
     */
    private java.util.Set requiredVariables=null;
    
    /**
     * Creates a new instance of AbstractCommand
     */
    public AbstractCommand() {
        requiredVariables = new HashSet();
    }
    
    /**
     * Initalize the command.  This calls the init(String[]) method to process and verify the actual command arguments.
     * 
     * This method is seperated from the constructor so that the no-paremeter constructor can be used to create a command using simple reflection.  Once the command object is created, this init method can be called without using reflection.
     * 
     * @param args Parameters passed in to the command
     * @param lineNumber Line number where this command appears (used for informative error messages)
     * @throws InitalizationException If there is a problem processing the parameters
     */
    public void init(String[] args, int lineNumber) throws InitalizationException {
        this.lineNumber = lineNumber;
        try {
            init(args);
        } catch (InitalizationException ex) {
            throw new InitalizationException("Error on scipt line "+lineNumber+": "+ex.getMessage());
        }
    }
    
    /**
     * Get the set of variables used via variable substution in the arguments
     * @return Set of all variables used
     */
    public java.util.Set getRequiredVariables() {
        return requiredVariables;
    }
    
    protected boolean getBoolean(String value, boolean defaultValue) {
        if (value == null || value.equals("")) return defaultValue;
        char val = value.toLowerCase().charAt(0);
        switch (val) {
            case 't':
            case '1':
            case 'y':
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets the line number this command appears in the script
     * @return The line number (1-based)
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Parse a string to find any variables mentioned, uses the variablePattern to identify the variables
     * @param val String that may or may not contain variable references
     */
    @SuppressWarnings("unchecked")
    protected void trackVariableDependencies(String val) {
        if (val == null) return;
        Engine installer = Engine.getInstance();
        Matcher m = variablePattern.matcher(val);
        while (m.find()) {
            String varName = m.group(1);
            Variable var = Variable.getVariable(varName);
            requiredVariables.add(var);
            var.addDependency(this);
        }
    }
    
    /**
     * Expand variable values if possible
     * @param val String value that may or may not contain references to script variables
     * @return Null if var null, or if val refers to variables that have not been initalized yet
     * a string if var contains no variables, or if all variable substutions were performed correctly.
     */
    protected String translateValue(String val) {
        if (val == null) return null;
        String newValue = new String(val);
        for (Iterator i=requiredVariables.iterator(); i.hasNext();) {
            Variable v = (Variable) i.next();
            // Don't replace variable value if it isn't offically initalized
            if (! v.isInitalized()) continue;
            newValue = newValue.replaceAll("\\{"+v.getName()+"\\}", v.getValue());
        }
        
        // Check if there are any unreplaced variables, and if so return null
        Matcher m = variablePattern.matcher(newValue);
        if (m.find()) return null;        
        
        // Looks like we matched everything, return the value!
        return newValue;
    }

    /**
     * Verify that a file exists, and has no spaces in the name
     * @param file Path of file to verify
     * @throws BadVariableValueException if file does not exist, or if file name has spaces
     */
    protected void verifyFile(String file) throws BadVariableValueException {
        String useFile = translateValue(file);
//        if (useFile != null) {
//            if (useFile.indexOf(' ') >= 0)
//                throw new BadVariableValueException(new IOException("Please do not use path names with spaces.  This will break external command-line driven applications."));
//            File f = new File(useFile);
//            if (!f.exists() || !f.isFile()) {
//                InputStream i = AbstractCommand.class.getClassLoader().getSystemResourceAsStream(useFile);
//                if (i == null)
//                    throw new BadVariableValueException(new FileNotFoundException("Could not find file "+useFile));
//            }
//        }
    }

    /**
     * Verify that a directory exists, and has no spaces in the name
     * @param file Path of directory to verify
     * @throws BadVariableValueException If directory does not exist, or if directory name has spaces
     */
    protected void verifyDirectory(String file) throws BadVariableValueException {
        String useFile = translateValue(file); 
        if (useFile != null) {
            if (useFile.indexOf(' ') >= 0)
                throw new BadVariableValueException(new FileNotFoundException("Please do not use path names with spaces.  This will break external command-line driven applications."));
            File f = new File(useFile);
            if (!f.exists() || !f.isDirectory())
                throw new BadVariableValueException(new FileNotFoundException("Could not find directory "+useFile));
        }
    }
    
    /**
     * Execute this command.  Runs checkPaths() to do a last-minute verification.  If the installer is in debug mode, then doDebugExecute is called.  Otherwise, doExecute is called.
     * 
     * @throws FatalScriptException If there was trouble verifying files used in this command, or if there was a problem executing the command itself.
     */
    public void execute() throws FatalScriptException {
        for (Iterator i=requiredVariables.iterator(); i.hasNext(); ) {
            Variable v = (Variable) i.next();
            if (!v.isInitalized()) throw new FatalScriptException("Script uses uninitalized variable "+v.getName(), null);
        }
        try {
            checkPaths();
        } catch (BadVariableValueException ex) {
            throw new FatalScriptException("Error during last-minute validation check", ex);
        }
        doExecute();
    }

    /**
     * Prompt a user for input, with or without a default value.
     * @return User-entered value
     * @param prompt Prompt message to display for user
     * @param defaultValue Default value to offer if any (or null for no default)
     * @throws FatalScriptException If there was trouble communicating with the user
     */
    protected static String promptUser(String prompt, String defaultValue) throws FatalScriptException {
        int retries = 100;
        try {
            while (retries > 0) {
                retries--;
                String enteredValue;
                Engine.flushInput();
                Engine.getOut().print(prompt);
                if (defaultValue != null)
                    Engine.getOut().println(" ["+defaultValue+"]");
                Engine.getOut().print(">");
                Engine.flushOutput();
                enteredValue = Engine.getIn().readLine();
                if (enteredValue == null || "".equals(enteredValue.trim())) {
                    enteredValue=defaultValue;
                }
                if (enteredValue != null) return enteredValue;
                Engine.getOut().println("Input not understood.  Please try again.");
            }
        } catch (IOException ex) {
            throw new FatalScriptException("I/O Error when waiting to receive user input", ex);
        }
        throw new FatalScriptException("Failed to receive user input", null);
    }
    
    /**
     * Abstract method that should process the arguments of a command
     * 
     * @param args list of arguments to process (where the first argument is the command name itself)
     * @throws InitalizationException if there was an error processing the arguments
     */
    protected abstract void init(String[] args) throws InitalizationException;
    /**
     * Abstract method that should verify that any required files or directories exist.
     * @throws BadVariableValueException If a refered file or directory does not exist
     */
    public abstract void checkPaths() throws BadVariableValueException;
    /**
     * Abstract method that should perform the functions of this command
     * 
     * @throws FatalScriptException If there was a fatal error went executing this command
     */
    protected abstract void doExecute() throws FatalScriptException;
}