/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.ui;

import ags.communication.DataUtil;
import ags.communication.TransferHost;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author brobert
 */
public class Lores2BufferedScreen extends Lores2Screen {

    int activePage = 0;
    byte[][] lastScreen = new byte[2][0x0400];

    @Override
    public int getDisplayOffset() {
        return (activePage == 0 ? 0x0400 : 0x0800);
    }

    /**
     * Make a copy of the screen
     */
    protected void copyScreen() {
        byte[] buffer = getBuffer();
        for (int i = 0; i < buffer.length; i++) {
            lastScreen[activePage][i] = buffer[i];
        }
    }
    boolean[] isStale = {true, true};

    @Override
    public void markBufferStale() {
        isStale[0]=true;
        isStale[1]=true;
    }

    @Override
    public void send(TransferHost host) {
        activePage = (activePage == 0 ? 1 : 0);
        byte[] buffer = getBuffer();
        try {
            if (!USE_COMPRESSION) {
                host.sendRawData(buffer, getDisplayOffset(), 0, buffer.length);
            } else {
                byte[] send = DataUtil.packScreenUpdate(getDisplayOffset(), isStale[activePage] ? null : lastScreen[activePage], buffer);
                host.sendCompressedData(send);
            }
            isStale[activePage] = false;
            copyScreen();
            activate(host);
            if (activePage == 0) {
                host.toggleSwitch(0x0c054); // page1
            } else {
                host.toggleSwitch(0x0c055); // page2
            }
        } catch (IOException ex) {
            Logger.getLogger(TextScreen40.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}