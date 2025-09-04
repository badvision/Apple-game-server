/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ags.ui.gameSelector;

import ags.ui.FrameBasedWidget;
import ags.ui.IApplication;
import ags.ui.Keyboard;

/**
 *
 * @author brobert
 */
public class TextWidget extends FrameBasedWidget {

    public TextWidget(IApplication app) {
        super(app);
        setYSize(1);
    }
    String value = "";
    int cursor = 0;

    @Override
    public boolean handleKeypress(byte b) {
        switch (b) {
            case Keyboard.KEY_UP:
                cursor = 0;
                return true;
            case Keyboard.KEY_RIGHT:
                cursor = Math.min(cursor + 1, value.length());
                cursor = Math.min(getXSize(), cursor);
                return true;
            case Keyboard.KEY_DOWN:
                cursor = Math.min(getXSize(), value.length());
                return true;
            case Keyboard.KEY_LEFT:
                cursor = Math.max(0, cursor - 1);
                return true;
            case Keyboard.KEY_CTRL_D:
                if (cursor < value.length()) {
                    value = value.substring(0, cursor) + value.substring(cursor+1);
                } else if (value.length() > 0) {
                    value = value.substring(0,value.length()-1);
                    cursor = Math.max(cursor-1, 0);
                }
                return true;
            case Keyboard.KEY_DELETE:
                if (cursor == 0) return true;
                if (cursor < value.length()-1) {
                    value = value.substring(0, cursor-1) + value.substring(cursor);
                    cursor = Math.max(cursor-1, 0);
                } else if (value.length() > 0) {
                    value = value.substring(0,value.length()-1);
                    cursor = Math.max(cursor-1, 0);
                }
                return true;
        }
        char c = (char) b;
        if (c >= ' ' && c <= 127) {
            if (cursor == value.length() && cursor < getXSize()) {
                value += c;
                cursor++;
                return true;
            } else if (value.length() < getXSize()) {
                value = value.substring(0, cursor) + c + value.substring(cursor);
                cursor++;
                return true;
            }
        }
        return false;
    }

    public void setValue(String value) {
        if (value == null) {
            value = "";
        }
        this.value = value;
        cursor = value.length();
    }

    public String getValue() {
        return value;
    }

    @Override
    public void redrawInside() {
        if (isActive()) {
            String val1 = value.substring(0, cursor);
            String val2 = " ";
            if (cursor < value.length()) {
                val2 = value.substring(cursor, cursor + 1);
            }
            String val3 = " ";
            if (cursor < value.length() - 1) {
                val3 = value.substring(cursor + 1);
            }

            if (!"".equals(val1)) {
                app.getScreen().drawText(getX(), getY(), val1, false);
            }
            app.getScreen().drawText(getX() + cursor, getY(), val2, true);
            if (!"".equals(val3)) {
                app.getScreen().drawText(getX() + cursor + 1, getY(), val3, false);
            }
        } else {
            if (!"".equals(value)) {
                app.getScreen().drawText(getX(), getY(), value, false);
            }
        }
    }
}