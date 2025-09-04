package ags.script.commands;

import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.Target;
import ags.script.exception.FatalScriptException;

/**
 *
 * @author Administrator
 */
public class Call extends AbstractCommand {
    
    /** Creates a new instance of Call */
    public Call() {
    }

    String targetName;
    Target target;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("Call expects one argument, the name of the target to call");
        targetName = args[1];
        trackVariableDependencies(targetName);
    }

    public void checkPaths() throws BadVariableValueException {
        String translated = translateValue(targetName);
        if (targetName.equals(translated)) {
            target = Target.getTarget(targetName);
            if (target == null)
                throw new BadVariableValueException(new Exception("Cannot find target named "+targetName));
        }
    }

    protected void doExecute() throws FatalScriptException {
        try {
            String targetTranslatedName = translateValue(targetName);
            target = Target.getTarget(targetTranslatedName);
            if (target == null)
                throw new BadVariableValueException(new Exception("Cannot find target named "+targetTranslatedName));
            target.call();
        } catch (Exception ex) {
            throw new FatalScriptException("Error when calling target "+targetName, ex);
        }
    }    
}