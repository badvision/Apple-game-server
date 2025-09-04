/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import ags.game.Game;
import ags.game.GameUtil;
import ags.ui.HiresScreen;
import ags.ui.Keyboard;
import ags.ui.gameSelector.GameSelectorApplication;
import java.awt.Canvas;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import javax.swing.JFrame;

/**
 *
 * @author brobert
 */
public class UITest {

    public static void main(String... args) {
        initApplication();
    }
    static JFrame mainFrame;
    static Canvas drawArea;
    static GameSelectorApplication app;
    static HiresScreen screen;
    private static List<Game> games;

    private static void initApplication() {
        initFrame();
        screen = new HiresScreen();
        app = new GameSelectorApplication(screen) {
            @Override
            public void redraw() {
                super.redraw();
                drawArea.getGraphics().drawImage(HiresScreen.screen, 0, 0, 560, 192 * 2, null);
            }
        };
        app.setGames(GameUtil.readGames());
        app.redraw();
    }

    public static void initFrame() throws HeadlessException {
        mainFrame = new JFrame("UI Test");
        drawArea = new Canvas();
        drawArea.setSize(560, 192 * 2);
        mainFrame.add(drawArea);
        mainFrame.setSize(560 + 20, 192 * 2 + 30);
        mainFrame.validate();
        mainFrame.setVisible(true);
        mainFrame.setFocusTraversalKeysEnabled(false);
        drawArea.setFocusTraversalKeysEnabled(false);
        drawArea.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                byte value = (byte) e.getKeyChar();
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        value = Keyboard.KEY_LEFT;
                        break;
                    case KeyEvent.VK_RIGHT:
                        value = Keyboard.KEY_RIGHT;
                        break;
                    case KeyEvent.VK_UP:
                        value = Keyboard.KEY_UP;
                        break;
                    case KeyEvent.VK_DOWN:
                        value = Keyboard.KEY_DOWN;
                        break;
                    case KeyEvent.VK_ESCAPE:
                        value = Keyboard.KEY_ESCAPE;
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                        value = Keyboard.KEY_DELETE;
                        break;
                    case KeyEvent.VK_ENTER:
                        value = Keyboard.KEY_RETURN;
                        break;
                    case KeyEvent.VK_TAB:
                        value = Keyboard.KEY_TAB;
                        break;
                }
                app.handleKeypress(value);
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

        mainFrame.addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                app.redraw();
                drawArea.repaint();
                mainFrame.repaint();
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        });
    }
}
