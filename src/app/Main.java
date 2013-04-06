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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import lib.AstralIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;

/**
 *
 * @author Nathan Wiehoff
 */
public class Main extends JFrame {
    //window size information

    private int uiX = 0;
    private int uiY = 0;
    //engine
    Engine engine;
    //safety
    boolean safe = true;

    public Main() {
        super("High Albedo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        new Main().execute();
    }

    public void execute() {
        //detect operating system
        String os = System.getProperty("os.name").toLowerCase();
        System.out.println("Running on " + os);
        if (os.contains("linux")) {
            System.out.println("Trying to unpack Linux libraries.");
            try {
                extractLib("liblwjgl.so");
                extractLib("libopenal.so");
                extractLib("libjinput-linux.so");
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("Failed to extract 32 bit libs");
            }
            try {
                extractLib("liblwjgl64.so");
                extractLib("libopenal64.so");
                extractLib("libjinput-linux64.so");
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("Failed to extract 64 bit libs");
            }
        }
        // Initialize OpenAL and clear the error bit.
        try {
            AL.create();
            AL10.alGetError();
        } catch (Throwable le) {
            safe = false;
            le.printStackTrace();
            System.out.println("Audio is unsupported on this system.");
            String st = JOptionPane.showInputDialog(this, "Greetings, space friend!"
                    + "\n\n"
                    + "High Albedo uses LWJGL for audio since the sound\n"
                    + "system provided by the stock JVM is too buggy.\n\n"
                    + "This is a message to inform you that the LWJGL\n"
                    + "libraries could not be located! This is normal if\n"
                    + "this is the first time you've run High Albedo.\n\n"
                    + "You need to help set this up. High Albedo planned\n"
                    + "ahead and extracted the libraries to the folder that\n"
                    + "High_Albedo.jar is located in. You should copy them\n"
                    + "to one of these directories, \n\n" + System.getProperty("java.library.path") + "\n\n"
                    + "and try again. Altenatively, you can launch with the\n"
                    + "'-Djava.library.path=path/to/dir' flag where path/to/dir\n"
                    + "is a folder containing these libraries.\n\n"
                    + "\n\nTo continue without sound, type 'true' (without quotes)!");
            safe = Boolean.parseBoolean(st);
        }
        //create
        if (safe) {
            //initialize display
            windowInit();
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

    private void extractLib(String name) {
        try {
            // have to use a stream
            InputStream in = AstralIO.getStream("native/" + name);
            // always write to different location
            File fileOut = new File(name);
            if (fileOut.exists()) {
                fileOut.delete();
            }
            System.out.println("Writing lib to: " + fileOut.getAbsolutePath());
            OutputStream out = FileUtils.openOutputStream(fileOut);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to load library " + name);
        }
    }

    /*
     * Initialize the window
     */
    public final void windowInit() {
        setVisible(false);
        //set properties
        System.setProperty("sun.java2d.transaccel", "True");
        System.setProperty("sun.java2d.opengl", "True");
        System.setProperty("sun.java2d.d3d", "True");
        System.setProperty("sun.java2d.ddforcevram", "True");
        System.setProperty("javax.sound.sampled.SourceDataLine", "com.sun.media.sound.DirectAudioDeviceProvider");
        //do no allow manual resizing
        setResizable(false);
        //enter full screen
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gs = ge.getDefaultScreenDevice();
        try {
            //throw new Exception();
            setUndecorated(true);
            gs.setFullScreenWindow(this);
            System.out.println("Sucessfully acquired full screen.");
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
        //push frame
        show();
        //create buffer strategy
        createBufferStrategy(2);
        System.out.println("Window size of " + uiX + "," + uiY + " stored.");
    }

    /*
     * Initialize the engine
     */
    public final void engineInit() {
        BufferStrategy bf = null;
        while (bf == null) {
            bf = getBufferStrategy();
        }
        engine = new Engine(bf, uiX, uiY);
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
                if (ke.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    destroy();
                } else {
                    engine.getHud().handleKeyPressedEvent(ke);
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_F11) {
                    quickSave();
                } else if (ke.getKeyCode() == KeyEvent.VK_F12) {
                    quickLoad();
                } else {
                    engine.getHud().handleKeyReleasedEvent(ke);
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
    }

    private void quickLoad() {
        engine.stop();
        engine.load("quicksave");
        engine.start();
    }
}
