/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.communication;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author brobert
 */
public class TCPTransferHost extends TransferHost {
    private Socket socket;
    /**
     * Constructor
     * @param host 
     * @param port tcp port to use
     * @throws IOException 
     */
    public TCPTransferHost(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true); // Disable Nagle's algorithm for immediate transmission
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (UnknownHostException ex) {
            Logger.getLogger(TCPTransferHost.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException(ex.getMessage());
        } catch (IOException ex) {
            Logger.getLogger(TCPTransferHost.class.getName()).log(Level.SEVERE, null, ex);
            throw ex;
        }
    }

    @Override
    void waitToSend(int timeout) throws IOException {
        // We're TCP based, so no need to check flow control
        return;
    }

    @Override
    public void setBaud(int baudRate) {
        // We're TCP based, so no baud rate!
        currentBaud = baudRate;
    }


}
