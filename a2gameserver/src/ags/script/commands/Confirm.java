package ags.script.commands;

import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;
import java.io.BufferedReader;
import java.io.PrintStream;

/**
 * Confirm Command:<br>
 * This alerts the user to a specified message and halts the script until they confirm.
 * If they do not type "ok", the prompt will be repeated.
 * <br><br>
 * Arguments: <br>
 * Message
 * @author brobert, vps
 */
public class Confirm extends AbstractCommand {
    /**
     * message to show
     */
    private String message = null;
    /**
     * Ensure there is a message to show
     * @param args command arguments
     * @throws com.vignette.vps.install.InitalizationException If there is no message to show, or if there are too many arguments passed in
     */
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("Confirm called with wrong number of arguments!");
        message = args[1];
    }

    /**
     * Does nothing
     * @throws com.vignette.vps.install.BadVariableValueException never
     */
    public void checkPaths() throws BadVariableValueException {
        // Does nothing
    }

    /**
     * Display message and force them to type OK before exiting
     * 
     * @throws com.vignette.vps.install.FatalScriptException If there was trouble getting user input
     */
    protected void doExecute() throws FatalScriptException {
        BufferedReader in = Engine.getIn();
        PrintStream out = Engine.getOut();
        boolean prompt = true;
        // Loop until the user has entered a valid value that exists and meets requirements of all dependencies.
        while (prompt) {
            String read = promptUser(message + " (enter OK to continue)", null);
            if (String.valueOf(read).toUpperCase().equals("OK"))
                prompt = false;
            else
                out.println("Input not understood.  Please try again.");
        }
    }

    /**
     * Same as doExecute for this command
     * 
     * @throws com.vignette.vps.install.FatalScriptException If the retries run out
     */
    protected void doDebugExecute() throws FatalScriptException {
        // Safe to execute as-is
        doExecute();
    }
}