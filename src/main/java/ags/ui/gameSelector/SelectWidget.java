/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ags.ui.gameSelector;

import ags.ui.FrameBasedWidget;
import ags.ui.IApplication;
import ags.ui.IWidget;
import ags.ui.Keyboard;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author brobert
 */
public class SelectWidget extends FrameBasedWidget {

    public SelectWidget(IApplication app) {
        super(app);
        setYSize(1);
    }

    List<String> options;
    int currentOption = 0;

    @Override
    public boolean handleKeypress(byte b) {
        switch (b) {
            case Keyboard.KEY_UP:
            case Keyboard.KEY_LEFT:
                currentOption = Math.max(0, currentOption-1);
                return true;
            case Keyboard.KEY_DOWN:
            case Keyboard.KEY_RIGHT:
                currentOption = Math.min(options.size()-1, currentOption+1);
                return true;
        }
        return false;
    }

    public void setCurrentOption(String value) {
        currentOption = Math.max(0,options.indexOf(value));
    }

    public String getCurrentOption() {
        return options.get(currentOption);
    }

    public void setOptions(String... options) {
        this.options=Arrays.asList(options);
        int max = 0;
        for (String o : options) {
            if (o.length() > max) max = o.length();
        }
        setXSize(max);
    }

    @Override
    public void setActive(boolean active) {
        if (active) setYSize(options.size());
        else setYSize(1);
        super.setActive(active);
    }

    @Override
    public void redrawInside() {
        if (isActive()) {
            for (int i=0; i < options.size(); i++) {
                app.getScreen().drawText(getX(), getY()+i, options.get(i), currentOption == i);
            }
        } else {
            app.getScreen().drawText(getX(), getY(), options.get(currentOption), true);
        }
    }
}