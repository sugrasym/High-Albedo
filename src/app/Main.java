/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 /*
 *
 */
package app;

import engine.Engine;
import engine.Entity.State;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import lib.AstralIO;
import lib.AstralPrint;
import lib.AstralStream;
import universe.Universe;

/**
 *
 * @author Nathan Wiehoff
 */
public class Main extends JFrame {

    //window size information
    private boolean fullScreen;
    private int uiX = 0;
    private int uiY = 0;
    private int viewX = 0;
    private int viewY = 0;
    //engine
    Engine engine;
    //safety
    boolean safe = true;

    public Main() {
        super("High Albedo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //redirect console output to various endpoints in addition to console
        try {
            String prefix = AstralIO.getLogPrefix();

            redirectOutput(prefix);
            redirectError(prefix);
        } catch (Exception e) {
            System.out.println("Failed to redirect output.");
            e.printStackTrace();
        }
    }

    public void execute(boolean fullScreen, int wx, int wy) {
        this.fullScreen = fullScreen;
        //create
        if (safe) {
            //initialize display
            windowInit(wx, wy);
            //initialize engine
            engineInit();
            //add listeners
            listenerInit();
            //enter menu
            engine.menu();
        } else {
            setVisible(false);
            dispose();
            System.exit(0);
        }
    }

    /*
     * Initialize the window
     */
    public final void windowInit(int wx, int wy) {
        setVisible(false);
        //set properties
        System.setProperty("sun.java2d.transaccel", "True");
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            System.setProperty("sun.java2d.d3d", "True");
            System.out.println("Running on " + System.getProperty("os.name") + " using DirectX");
        } else {
            System.setProperty("sun.java2d.opengl", "True");
            System.out.println("Running on " + System.getProperty("os.name") + " using OpenGL");
        }
        System.setProperty("sun.java2d.ddforcevram", "True");
        System.setProperty("javax.sound.sampled.SourceDataLine", "com.sun.media.sound.DirectAudioDeviceProvider");
        //do no allow manual resizing
        setResizable(false);
        //enter full screen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        try {
            if (fullScreen) {
                //throw new Exception();
                setUndecorated(true);
                gs.setFullScreenWindow(this);
                System.out.println("Sucessfully acquired full screen.");
            } else {
                setUndecorated(false);
                setSize(wx, wy);
                System.out.println("Sucessfully acquired windowed mode.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            //fallback to a windowed mode
            setUndecorated(false);
            setSize(1024, 768);
            System.out.println("Failed to get full screen mode, falling back to windowed.");
        }
        //store size
        uiX = getWidth();
        uiY = getHeight();
        //store view
        viewX = wx;
        viewY = wy;
        //push frame
        show();
        //create buffer strategy
        createBufferStrategy(2);
        System.out.println("Window size of " + uiX + "," + uiY + " stored.");
        System.out.println("Frame size of " + viewX + "," + viewY + " stored.");
    }

    /*
     * Initialize the engine
     */
    public final void engineInit() {
        BufferStrategy bf = null;
        while (bf == null) {
            bf = getBufferStrategy();
        }
        engine = new Engine(bf, uiX, uiY, viewX, viewY, fullScreen,
                false, false);
    }

    /*
     * Initialize the listeners
     */
    public final void listenerInit() {
        addKeyListener(new KeyAdapter() {
            /*
             * Listeners
             */
            @Override
            public void keyTyped(KeyEvent ke) {
                engine.getHud().handleKeyTypedEvent(ke);
            }

            @Override
            public void keyPressed(KeyEvent ke) {
                switch (ke.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        destroy();
                        break;
                    case KeyEvent.VK_F9:
                        Universe.DEBUG_RENDER = !Universe.DEBUG_RENDER;
                        break;
                    default:
                        engine.getHud().handleKeyPressedEvent(ke);
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {
                switch (ke.getKeyCode()) {
                    case KeyEvent.VK_F11:
                        quickSave();
                        break;
                    case KeyEvent.VK_F12:
                        quickLoad();
                        break;
                    default:
                        engine.getHud().handleKeyReleasedEvent(ke);
                        break;
                }
            }
        });
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent me) {
                engine.getHud().handleMouseMovedEvent(me);
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                engine.getHud().handleMousePressedEvent(me);
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                engine.getHud().handleMouseReleasedEvent(me);
            }

            @Override
            public void mouseClicked(MouseEvent me) {
                engine.getHud().handleMouseClickedEvent(me);
            }
        });

    }

    /*
     * These are special event handlers
     */
    private void destroy() {
        //exit save
        exitSave();
        //escape closes the program always
        setVisible(false);
        dispose();
        System.exit(0);
    }

    private void quickSave() {
        //what would modern gaming be without save whoring?
        AstralIO io = new AstralIO();
        try {
            engine.stop();
            System.out.println("Starting Quicksave.");
            io.saveGame(engine.getUniverse(), "quicksave");
            System.out.println("Quicksave Complete.");
            engine.start();
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            engine.start();
        }
    }

    private void exitSave() {
        if (engine.getActivePlayerShip().getState() == State.ALIVE) {
            try {
                /*
                 * Saves game upon exit just in case you didn't mean to do that.
                 */
                AstralIO io = new AstralIO();
                engine.stop();
                System.out.println("Starting Exitsave.");
                io.saveGame(engine.getUniverse(), "autosave");
                System.out.println("Exitsave Complete.");
            } catch (Exception ex) {
                //escape closes the program always
                setVisible(false);
                dispose();
                System.exit(0);
            }
        } else {
            System.out.println("Not exit saving, player is dead.");
        }
    }

    private void quickLoad() {
        engine.stop();
        engine.load("quicksave");
        engine.start();
    }

    private static void redirectOutput(String prefix) throws FileNotFoundException {
        //setup outputs
        AstralStream master = new AstralStream();
        master.add(System.out);
        master.add(new PrintStream(new File(prefix + "-output-log.txt")));
        //todo: add stream for in-game console when implemented

        //apply new stream
        System.setOut(new AstralPrint(master));
    }

    private static void redirectError(String prefix) throws FileNotFoundException {
        //setup outputs
        AstralStream master = new AstralStream();
        master.add(System.err);
        master.add(new PrintStream(new File(prefix + "-error-log.txt")));
        //todo: add stream for in-game console when implemented

        //apply new stream
        System.setErr(new AstralPrint(master));
    }
}
