package ags.script.commands;

import ags.communication.GenericHost;
import ags.script.AbstractCommand;
import ags.script.BadVariableValueException;
import ags.script.Engine;
import ags.script.InitalizationException;
import ags.script.exception.FatalScriptException;

/**
 *
 * @author Administrator
 */
public class FlowControl extends AbstractCommand {
    
    /** Creates a new instance of FlowControl */
    public FlowControl() {
    }

    GenericHost.FlowControl flow = null;
    
    protected void init(String[] args) throws InitalizationException {
        if (args.length != 2)
            throw new InitalizationException("FlowControl one argument, the flow control mode to use.");
        try {
            flow = GenericHost.FlowControl.valueOf(args[1].toLowerCase());
        } catch (Throwable t) {
        }
        if (flow == null) {
            String values = "";
            for (GenericHost.FlowControl f:GenericHost.FlowControl.values()) {
                values += f + " ";
            }
            throw new InitalizationException("Flow control mode "+args[1]+" not understood.  Use one of the following: "+values.toString());
        }
    }

    public void checkPaths() throws BadVariableValueException {
    }

    protected void doExecute() throws FatalScriptException {
        GenericHost.getInstance().setFlowControl(flow);
    }    
}