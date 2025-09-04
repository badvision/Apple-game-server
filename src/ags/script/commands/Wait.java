package ags.script.commands;

import ags.script.AbstractCommand;
import ags.script.InitalizationException;
import ags.script.Engine;

/**
 * Wait command<br>
 * Pauses execution for a specified number of milliseconds
 * <br><br>
 * Arguments:<br>
 * number of milliseconds to wait
 */
public class Wait extends AbstractCommand {
    /**
     * number of seconds to wait
     */
    int waitTime = 0;
    
    /**
     * Validates argument is a valid number
     * 
     * @param args numSeconds
     * @throws com.vignette.vps.install.InitalizationException If parameters passed in don't make sense
     */
    protected void init(String[] args) throws InitalizationException {
       if (args.length != 2)
            throw new InitalizationException("Message called with wrong number of arguments!");
        try {
            waitTime = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            throw new InitalizationException("Wait called with non-numeric argument: "+args[1]);
        }
     }

    /**
     * Does nothing
     */
    public void checkPaths() {}

    /**
     * Waits for the specified number of seconds
     */
    protected void doExecute() {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException ex) {
            // Yeah, that's not really likely to happen...
            ex.printStackTrace(Engine.getLogOut());
        }
    }
}