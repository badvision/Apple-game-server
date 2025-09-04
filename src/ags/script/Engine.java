package ags.script;

import ags.controller.Configurable;
import ags.controller.Configurable.CATEGORY;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Describes a resuable script engine, which provided a set of Targets defined in a script file to perform various sets of actions.
 * @author brobert, vps
 */
public class Engine {
    /**
     * Does the program absolutely need to print a newline for the client to see the last printed line?
     */
    @Configurable(category=CATEGORY.ADVANCED, isRequired=false)
    static public boolean FLUSH_REQUIRES_NEWLINE = false;
    /**
     * singleton installer instance
     */
    static private Engine instance = null;
    /**
     * input stream in use
     */
    static public BufferedReader in = null;
    /**
     * output stream in use
     */
    static public PrintStream out = null;
    
    /**
     * Get current input stream
     * @return Current input stream (Most likely System.in)
     */
    static public BufferedReader getIn() {return in;}
    /**
     * Flush anything that might be in the input stream.
     * This is used to ignore extra input that is entered before the user was prompted.  
     * This prevents the user from making silly mistakes like hitting enter too many times.
     */
    public static void flushInput() {
        try {
            while(in.ready()) in.readLine();
        } catch (IOException ex) {
            // Shouldn't happen
        }
    }

    /**
     * Force all written characters to be flushed to output.  In cases where something is printed without a newline, this ensures the output will actually be visible to the user right away.
     */
    public static void flushOutput() {
        if (FLUSH_REQUIRES_NEWLINE)
            out.println();
        out.flush();
    }
    
    /**
     * Get the currently used output stream for console and log output.
     * @return The appropriate output stream
     */
    static public PrintStream getOut() {return out;}

    /**
     * Get the log output stream.
     * @return The appropriate output stream
     */
    static public PrintStream getLogOut() {return out;}
    
    static {
        // Init in/out streams are mapped here in case we want to redirect output!
        in = new BufferedReader(new InputStreamReader(System.in));
        out = System.out;
    }
    
    /**
     * This is the main entry point
     * @param script the path of the script to execute
     */
    public static void start(String script) {
        instance = new Engine();
        instance.init(script);
        instance.run();
    }
    
    /**
     * Get the singleton instance of the installer
     * @return The installer that is running
     */
    public static Engine getInstance() {
        return instance;
    }
    
    //--------------------------------------------------
    /**
     * Creates a new instance of Engine
     */
    private Engine() {}
    
    /**
     * Initalize the installer and execute it
     * @param args command-line arguments to validate
     */
    private void init(String script) {
        try {
            Script.parseScript(script);
            Target.verifyAll();
        } catch (BadVariableValueException ex) {
            ex.printStackTrace(getOut());
            System.exit(1);
        } catch (InitalizationException ex) {
            ex.printStackTrace(getOut());
            System.exit(1);
        }        
    }

    /**
     * Run the script, starting with the specified target
     */
    private void run() {
        String targetName = Script.DEFAULT_TARGET;

        if (Script.USE_TARGET != null) targetName = Script.USE_TARGET;
        Target runMe = Target.getTarget(targetName);
        try {
            System.out.println("Executing script target "+targetName);
            if (runMe == null)
                throw new Exception("Target "+targetName+" not found!");
            runMe.call();
        } catch (Exception ex) {
            ex.printStackTrace(out);
            System.exit(1);
        }
    }    
    
    // Script flow control logic
    Target errorHandler = null;
    Target gotoNext = null;

    public void setGotoNext(Target gotoNext) {
        this.gotoNext = gotoNext;
    }

    public Target getGotoNext() {
        return gotoNext;
    }

    public void setErrorHandler(Target errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Target getErrorHandler() {
        return errorHandler;
    }    
}