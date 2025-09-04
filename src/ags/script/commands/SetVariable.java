package ags.script.commands;

import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.Variable;
import ags.script.exception.FatalScriptException;
import java.io.PrintStream;

/**
 * Set Command:<br>
 * Sets a variable with a value, either automatically or via user entry.
 * User entry is verified and pre-validated to ensure the value entered by the user will not cause errors later in the script.
 * <br><br>
 * Default value logic:<br>
 * If no default value is provided, the user will always be prompted and will not be offered a default value.<br>
 * If a default is available, but prompting is requested, the default value is only offered if it is known not to cause errors.<br>
 * If defaults are not acceptable, the user will always be prompted for a value and the default value will not be offered.
 * <br><br>
 * Arguments:<br>
 * description<br>
 * variable name<br>
 * (optional) default value<br>
 * (optional) always prompt -- indicated by the question mark, and only applicable if default value is specified
 * <br><br>
 * Examples:<br>
 * set "variable 1" var1<br>
 * This will create a variable called var1 and will prompt the user for the value always
 * <br><br>
 * set "variable 2" var2 "some value"<br>
 * This will create a variable called var2 and will prompt the user only if "some value" is somehow unacceptable
 * <br><br>
 * set "variable 3" var1 "some value" ?<br>
 * This will create a variable called var3 and will always prompt the user, offering the default only if it is acceptable
 * @author brobert, vps
 */
public class SetVariable extends AbstractCommand {
    /**
     * untranslated default value (if any)
     */
    private String untranslatedValue = null;
    /**
     * variable being set
     */
    private Variable variable = null;
    /**
     * is explicit prompting enabled?
     */
    private boolean prompt = false;

    /**
     * Validates that correct number of arguments are passed in (see class description)
     * @param args description, name, [default value], [propmt?]
     * @throws com.vignette.vps.install.InitalizationException If the arguments passed in don't make sense
     */
    protected void init(String[] args) throws InitalizationException {
        if (args.length < 3) {
            throw new InitalizationException("Set command called with too few arguments!");
        }
        if (args.length > 5) {
            throw new InitalizationException("Set command called with too many arguments!");
        }
        if (args.length == 5) {
            if ("?".equals(args[4]))
                prompt = true;
            else
                throw new InitalizationException("Last parameter passed to Set command not understood: "+args[4]);
        }
        String varDesc = args[1];
        String varName = args[2];
        if (args.length > 3) {
            untranslatedValue = args[3];
            trackVariableDependencies(untranslatedValue);
        } else
            prompt = true;
        variable = Variable.getVariable(varName);
        variable.setAccountedFor(true);
        variable.setDescription(varDesc);
        variable.setInitalized(false);
    }

    /**
     * If default value is provided, and prompting is disabled, then the default value will be considered final if it doesn't cause errors directly.
     */
    public void checkPaths() {
        if (prompt == false) {
            String possibleValue = translateValue(untranslatedValue);
            if (possibleValue != null) {
                try {
                    variable.setInitalized(false);
                    variable.setValue(possibleValue);
                } catch (BadVariableValueException ex) {
                    // Whoops!  Value isn't good -- forget about it!
                    prompt = true;
                    possibleValue = null;
                    variable.setInitalized(false);
                }
            }
        }
    }

    /**
     * Set variable value, prompting the user as necessary
     * 
     * @throws com.vignette.vps.install.FatalScriptException If there is trouble communicating with the user
     */
    protected void doExecute() throws FatalScriptException {
        PrintStream out = Engine.getOut();
        String possibleValue = translateValue(untranslatedValue);
        if (possibleValue != null) {
            try {
                variable.setInitalized(true);
                variable.setValue(possibleValue);
            } catch (BadVariableValueException ex) {
                // Whoops!  Value isn't good -- forget about it!
                prompt = true;
                possibleValue = null;
            }
        }
        String enteredValue = possibleValue;
        // Loop until the user has entered a valid value that exists and meets requirements of all dependencies.
        while (prompt) {
            enteredValue = promptUser("Please enter "+variable.getDescription()+": ", possibleValue);
            if (enteredValue != null && !"".equals(enteredValue)) {
                enteredValue = enteredValue.trim();
                out.println("User entry: "+enteredValue);
                enteredValue = enteredValue.trim();
                try {
                    // Once the variable is inialized, changes will be checked for validity against dependencies
                    variable.setInitalized(true);
                    variable.setValue(enteredValue);
                    // This is the ONLY WAY OUT OF THE LOOP!  MWAHAHAHAH!
                    prompt = false;
                } catch (BadVariableValueException ex) {
                    out.println("A task will fail if you use this value: "+ex.getMessage());
                    ex.printStackTrace(Engine.getLogOut());
                }
            } else {
                out.println("No input received, please try again.");
            }
        }

        // If we're here then the variable was inialized successfully.
        variable.setInitalized(true);
    }

    /**
     * same as doExecute
     * 
     * @throws com.vignette.vps.install.FatalScriptException If there is trouble communicating with the user
     */
    protected void doDebugExecute() throws FatalScriptException {
        doExecute();
    }
}