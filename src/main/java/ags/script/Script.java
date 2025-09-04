package ags.script;

import ags.controller.Configurable;
import ags.controller.Configurable.CATEGORY;
import ags.script.commands.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * The script parser
 * @author blurry
 */
public class Script {    
    /**
     * Default target to execute if none specified
     */
    @Configurable(category=CATEGORY.ADVANCED, isRequired=false)
    public static String DEFAULT_TARGET = "default";
    public static String USE_TARGET = null;
    /**
     * This class contains static methods only... no constructor
     */
    private Script() {}

    /**
     * Hash of all supported commands (used to map script commands and easily determine syntax errors in script)
     */
    public enum Command {
        baud(Baud.class),
        call(Call.class),
        confirm(Confirm.class),
        echo(Echo.class),
        echoCheck(EchoCheck.class),
        expect(Expect.class),
        expectPrompt(ExpectPrompt.class),
        flowControl(FlowControl.class),
        flow(FlowControl.class),
        jumpTo(Goto.class),
        jump(Goto.class),
        legacy(LegacyMode.class),
        legacyMode(LegacyMode.class),
        onError(OnError.class),
        require(Require.class),
        sendBinary(SendBinary.class),
        sendChar(SendChar.class),
        sendMessage(SendMessage.class),
        sendText(SendText.class),
        sendTextBlind(SendTextBlind.class),
        set(SetVariable.class),
        wait(Wait.class);
        
        private Class myclass;
        Command(Class c) {myclass = c;}
        public Class getCommandClass() {return myclass;}
    }
    
    /**
     * Parse a script file into a series of targets and commands.
     * @param scriptPath full path to script file on the filesystem, or relative path to file in the archive (e.g. scripts/siab)
     * @throws InitalizationException If there was a problem parsing the script or initalizing any of the commands in the script
     */
    @SuppressWarnings("unchecked")
    static public void parseScript(String scriptPath) throws InitalizationException {
        InputStream in = null;
        File f = new File(scriptPath);
        if (f.exists() && f.isFile()) {
            try {
                in = new FileInputStream(f);
            } catch (FileNotFoundException ex) {
                // This should never happen because we checked this already!
                in = null;
            }
        } else {
            // Try multiple class loaders to find the resource
            in = ClassLoader.getSystemResourceAsStream(scriptPath);
            if (in == null) {
                in = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptPath);
            }
            if (in == null) {
                in = Script.class.getClassLoader().getResourceAsStream(scriptPath);
            }
        }
        // If we don't have input, then stop right now!
        if (in == null) throw new InitalizationException("Specified script not found: "+scriptPath);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        Target defaultTarget = new Target(DEFAULT_TARGET);
        List currentTarget = defaultTarget.getCommands();
        String currentLine = null;
        int lineNumber = 0;
        try {
            while ((currentLine = reader.readLine()) != null) {
                lineNumber++;
                // strip off comments
                if (currentLine.indexOf(';') >= 0)
                    currentLine = currentLine.substring(0, currentLine.indexOf(';'));
                currentLine = currentLine.trim();
                
                // See if this is a new target name
                if (currentLine.matches("^\\[.*?\\]$")) {
                    // Yes it is, set the current target
                    String targetName = currentLine.substring(1, currentLine.length()-1);
                    Target t = new Target(targetName);
                    currentTarget = t.getCommands();
                    continue;
                }
                    
                // ignore anything else unless there's a target for it to go to
                if (currentTarget == null || "".equals(currentLine)) continue;
                String[] commandArgs = splitCommand(currentLine);
                if (commandArgs.length == 0) continue;
                Command command = null;
                try {
                    command = Command.valueOf(commandArgs[0]);
                } catch (Throwable t) {
                    // ignore
                }
                if (command == null)
                    throw new InitalizationException("Error parsing script "+scriptPath+" at line "+lineNumber+": Unknown command '"+commandArgs[0]+"' -- full line = "+currentLine);
                Class commandClass = command.getCommandClass();
                AbstractCommand newCommand = null;
                try {
                    newCommand = (AbstractCommand) commandClass.newInstance();
                } catch (InstantiationException ex) {
                    throw new InitalizationException("Error parsing script "+scriptPath+" at line "+lineNumber+": "+ex.getMessage()+" -- full line = "+currentLine);
                } catch (IllegalAccessException ex) {
                    throw new InitalizationException("Error parsing script "+scriptPath+" at line "+lineNumber+": "+ex.getMessage()+" -- full line = "+currentLine);
                }
                newCommand.init(commandArgs, lineNumber);
                currentTarget.add(newCommand);
            }
        } catch (IOException ex) {
            throw new InitalizationException("Error when reading script: "+ex.getMessage());
        }
    }

    /**
     * Split a line up into multiple arguments, preserving spaces within quotes.  The general format of a line is:
     * 
     * arg1 arg2 "arg3 with spaces" arg4 ;comment that gets ignored
     * 
     * which would yield the following list of arguments:
     * arg1
     * arg2
     * arg3 with spaces
     * arg4
     * @param line Line to split up
     * @return List of args
     */
    @SuppressWarnings("unchecked")
    static public String[] splitCommand(String line) {
        List args = new ArrayList();
        StringBuffer current = new StringBuffer();
        boolean inQuotes = false;
        boolean ignore = false;
        for (int i=0; i < line.length(); i++) {
            char c = line.charAt(i);
            switch (c) {
                case ' ':
                    if (inQuotes && !ignore) {
                        current.append(c);
                    } else {
                        ignore=false;
                        args.add(current.toString());
                        current = new StringBuffer();
                    }
                    break;
                case '"':
                    inQuotes = !inQuotes;
                    if (!inQuotes) ignore=true;
                    break;
                default:
                    if (!ignore) current.append(c);
            }
        }
        args.add(current.toString());
        return (String[]) args.toArray(new String[0]);
    }    
}