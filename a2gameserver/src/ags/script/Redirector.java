package ags.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

/**
 * The redirector lets you take an input stream and point it back to one or more output streams.
 * For example, this can be used to pipe the stdout of a subprocess (Process object) to one or more other output streams.
 * @author brobert, vps
 */
public class Redirector {
    /**
     * worker thread to perform redirection
     */
    private Thread t;
    /**
     * input stream reader
     */
    private BufferedReader reader;
    /**
     * output streams to redirect input to
     */
    private PrintStream[] outStreams;
        
    /**
     * Creates a new instance of Redirector to multiplex output to multiple pipes
     * @param in InputStream to read from
     * @param out List of output streams to pipe input to
     * @throws java.io.IOException if there was a problem setting up the pipe
     */
    public Redirector(InputStream in, OutputStream[] out) throws IOException {
        outStreams = new PrintStream[out.length];
        for (int i=0; i < outStreams.length; i++)
            outStreams[i]=new PrintStream(out[i]);
        init(in);
    }
        
    /**
     * Creates a new instance of Redirector with a single pipe
     * @param in Input stream to read from
     * @param out Single writer to write to
     * @throws java.io.IOException If there was a problem setting up the pipe
     */
    public Redirector(InputStream in, PrintStream out) throws IOException {
        outStreams = new PrintStream[1];
        outStreams[0]=out;
        init(in);
    }
    
    /**
     * initalize output pipe and start the worker thread
     * @param in Input stream to redirect
     */
    private void init(InputStream in) {
        reader = new BufferedReader( new InputStreamReader(in));        
        t = new Thread(new Runnable() {
            public void run() {
                try {
                    /*
                    try {
                        // First give the underlying stream a chance to fill up a little
                        Thread.sleep(25);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                     */
                    String line = null;
                    while ((line = reader.readLine()) != null)                        
                        for (int i=0; i < outStreams.length; i++)
                            outStreams[i].println(line);
                    for (int i=0; i < outStreams.length; i++)
                        outStreams[i].flush();                    
                } catch (IOException e) {
                    // Stream closed -- ignore this
                }
            }
        });
        t.start();
    }
    
    /**
     * Is the pipe thread still alive?
     * @return True if the pipe is still redirecting output
     */
    public boolean isAlive() {
        return t.isAlive();
    }
}