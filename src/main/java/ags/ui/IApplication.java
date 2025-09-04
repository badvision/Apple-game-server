package ags.ui;

import ags.communication.TransferHost;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract application that uses widgets and a virtual screen buffer to product an interactive program capable of handling keystrokes
 * @author blurry
 */
public abstract class IApplication {

    /**
     * Minimum distance between framebuffer changes -- if value is too small the server will over-communicate and be less efficient.  If too large, the server will often send more data than is necessary and also be less efficient.
     */
    static final int MIN_CHANGE_DISTANCE = 2048;
    /**
     * List of widgets in use
     */
    protected List<IWidget> widgets;
    /**
     * Ordered list of widgets as they should appear (top-most widget is at the tail of the list)
     */
    protected List<IWidget> visible;
    /**
     * Virtual framebuffer to draw to
     */
    private IVirtualScreen screen;
    /**
     * Current active widget (index in widgets list)
     */
    protected int currentWidget = -1;
    /**
     * Copy of previous framebuffer, used to identify differences and ultimately which sections of the screen have changed and require sending
     */
    protected byte[] lastScreen;

    /**
     * Creates a new instance of IApplication
     * @param s Virtual screen buffer to use
     */
    public IApplication(IVirtualScreen s) {
        Thread.currentThread().setName("Game Selector");
        widgets = new ArrayList<IWidget>();
        visible = new ArrayList<IWidget>();
        setScreen(s);
        init();
    }

    /**
     * Redraw widgets in their visible order
     */
    public void redraw() {
        getScreen().clear();
        for (IWidget widget : visible) {
            widget.redraw();
        }
    }

    /**
     * Mark a widget as the currently active widget (focus)
     * @param w Widget to focus on
     */
    public void activate(IWidget w) {
        if (!visible.isEmpty()) {
            visible.get(visible.size() - 1).setActive(false);
        }
        w.setActive(true);
        w.moveToTop();
        currentWidget = widgets.indexOf(w);
    }

    /**
     * Add a widget to be used
     * @param w Widget to add
     */
    public void addWidget(IWidget w) {
        widgets.add(w);
        visible.add(w);
    }

    /**
     * Move a widget to the top of the visible stack
     * @param w Widget to move to the top
     */
    public void moveToTop(IWidget w) {
        visible.remove(w);
        visible.add(w);
        if (currentWidget >= 0) {
            widgets.get(currentWidget).setActive(false);
        }
        currentWidget = widgets.indexOf(w);
        w.setActive(true);
    }

    public void moveToBottom(IWidget w) {
        moveToTop(visible.get(0));
        visible.remove(w);
        visible.add(0, w);
        currentWidget = widgets.indexOf(visible.get(visible.size() - 1));
        visible.get(visible.size() - 1).setActive(true);
        w.setActive(false);
    }

    /**
     * Remove/destroy widget
     * @param w Widget to remove
     */
    public void removeWidget(IWidget w) {
        widgets.remove(w);
        visible.remove(w);
        w.setActive(false);
        if (currentWidget >= widgets.size()) {
            currentWidget = widgets.size() - 1;
        }
        if (currentWidget >= 0) {
            activate(widgets.get(currentWidget));
        }
    }

    /**
     * Delegate keypress to the active widget
     * @param b Keypresss value
     */
    public boolean handleKeypress(byte b) {
        if (handleGlobalKeypress(b)) {
            redraw();
            return true;
        }
        if (currentWidget >= 0 && widgets.get(currentWidget).handleKeypress(b)) {
            redraw();
            return true;
        }
        handleInvalidKeypress(b);
        return false;
    }

    /**
     * Initalize the application (abstract method -- subclass can use this to create all necessasry widgets, etc)
     */
    abstract protected void init();

    /**
     * Check if keypress is globally relevant (must be implemented by subclass)
     * @param b Keypress to evaluate
     * @return True if keypress was global and already handled, false if keypress should be passed to topmost widget
     */
    abstract protected boolean handleGlobalKeypress(byte b);

    /**
     * Handle unrecognized keypress
     * @param b Key pressed
     * @return true if handled (ignored for now)
     */
    abstract protected boolean handleInvalidKeypress(byte b);

    /**
     * Get current framebuffer
     * @return current framebuffer
     */
    public IVirtualScreen getScreen() {
        return screen;
    }

    /**
     * Set current framebuffer
     * @param screen current framebuffer
     */
    public void setScreen(IVirtualScreen screen) {
        this.screen = screen;
    }

    public void activateScreen(TransferHost host) {
        screen.activate(host);
    }
}