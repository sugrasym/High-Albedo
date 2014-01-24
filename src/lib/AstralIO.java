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
 * Reads/writes file data. Nuff said. Nathan Wiehoff, masternerdguy@yahoo.com
 */
package lib;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ConcurrentModificationException;
import javax.swing.ImageIcon;
import universe.Universe;

public class AstralIO implements Serializable {

    //resource folder (should be in jar file)
    public static final String RESOURCE_DIR = "/resource/";
    //name of local storage directory
    public static final String STORE_DIR = "/.highalbedo/";
    //saved game and setting locations
    public static final String SAVE_GAME_DIR = STORE_DIR + "saves/";
    public static final String CONFIG_FILE_LOC = STORE_DIR + "config";

    /*
     * Text
     */
    public static String readFile(String target, boolean local) {
        String ret = "";
        //Attemps to load an external file (local = false) or a file from within the archive
        if (local) {
            try {
                ret = readTextFromJar(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                try (BufferedReader in = new BufferedReader(new FileReader(target))) {
                    String str;
                    while ((str = in.readLine()) != null) {
                        ret += str + "\n";
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static void writeFile(String target, String text) {
        try {
            FileWriter fstream = new FileWriter(target);
            try (BufferedWriter out = new BufferedWriter(fstream)) {
                out.write(text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readTextFromJar(String target) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        String ret = "";

        try {
            is = AstralIO.class.getResourceAsStream(RESOURCE_DIR + target);
            br = new BufferedReader(new InputStreamReader(is));
            while (null != (line = br.readLine())) {
                ret = ret + line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /*
     * Images
     */
    public static Image loadImage(String target) throws NullPointerException, URISyntaxException {
        System.out.println("Loading image resource " + RESOURCE_DIR + target);
        ImageIcon ico = new ImageIcon(AstralIO.class.getResource(RESOURCE_DIR + target));
        //extract image
        Image test = ico.getImage();
        return test;
    }

    /*
     * Binary data
     */
    public static InputStream getStream(String target) {
        //System.out.println("Loading stream resource " + RESOURCE_DIR + target);
        return AstralIO.class.getResourceAsStream(RESOURCE_DIR + "/" + target);
    }

    public static void setupGameDir() {
        String home = System.getProperty("user.home") + STORE_DIR;
        String saves = System.getProperty("user.home") + SAVE_GAME_DIR;
        //create the main folder
        File homeFolder = new File(home);
        if (!homeFolder.exists()) {
            homeFolder.mkdir();
        }
        //create the subfolder
        File saveFolder = new File(saves);
        if (!saveFolder.exists()) {
            saveFolder.mkdir();
        }
    }

    public void saveGame(Universe universe, String gameName) throws Exception {
        try {
            String home = System.getProperty("user.home") + SAVE_GAME_DIR;
            //generate serializable universe
            Everything everything = new Everything(universe);
            //serialize universe
            FileOutputStream fos = new FileOutputStream(home + gameName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(everything);
        } catch (ConcurrentModificationException e) {
            saveGame(universe, gameName);
        }
    }

    public class Everything implements Serializable {
        /*
         * This class contains everything in the universe in a temporary container
         * useful for serialization.
         */

        protected Universe universe;

        public Everything(Universe universe) {
            this.universe = universe;
        }

        public Universe getUniverse() {
            return universe;
        }

        public void setUniverse(Universe universe) {
            this.universe = universe;
        }
    }
}
