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
 * Program for automatically converting a set of images into widescreen and
 * standard backplates.
 */
package util;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 *
 * @author nwiehoff
 */
public class PlateMaker {

    private static final int WIDE_X = 1920;
    private static final int WIDE_Y = 1080;
    private static final int STD_X = 1280;
    private static final int STD_Y = 1024;

    private String name;
    private String inputDirectory;
    private String outputDirectory;

    public PlateMaker() {
        if (setup()) {
            File inDir = new File(inputDirectory);
            //create std and wide subfolders
            File outDir = new File(outputDirectory);
            String stdPath = outputDirectory + "/std/";
            String widePath = outputDirectory + "/wide/";
            File stdOutDir = new File(stdPath);
            File wideOutDir = new File(widePath);
            if (!outDir.exists()) {
                outDir.mkdir();
            }
            if (!stdOutDir.exists()) {
                stdOutDir.mkdir();
            }
            if (!wideOutDir.exists()) {
                wideOutDir.mkdir();
            }
            //get a list of files in the directory
            File[] listOfFiles = inDir.listFiles();
            for (int a = 0; a < listOfFiles.length; a++) {
                File raw = listOfFiles[a];
                try {
                    //load the file into memory
                    Image ico = loadImage(raw);
                    BufferedImage buff = new BufferedImage(ico.getWidth(null), ico.getHeight(null), BufferedImage.TYPE_INT_ARGB);
                    //draw
                    Graphics z = buff.getGraphics();
                    z.drawImage(ico, 0, 0, null);
                    //create the std subsection
                    int sx = (ico.getWidth(null) / 2) - (STD_X / 2);
                    int sy = (ico.getHeight(null) / 2) - (STD_Y / 2);
                    BufferedImage std = buff.getSubimage(sx, sy, STD_X, STD_Y);
                    //create the wide subsection
                    int wx = (ico.getWidth(null) / 2) - (WIDE_X / 2);
                    int wy = (ico.getHeight(null) / 2) - (WIDE_Y / 2);
                    BufferedImage wide = buff.getSubimage(wx, wy, WIDE_X, WIDE_Y);
                    //save the std image
                    File stdOut = new File(stdPath + name + a + ".png");
                    ImageIO.write(std, "png", stdOut);
                    //save the wide image
                    File wideOut = new File(widePath + name + a + ".png");
                    ImageIO.write(wide, "png", wideOut);
                    //report
                    System.out.println("Finished processing file " + (a + 1) + " / " + listOfFiles.length);
                } catch (NullPointerException | URISyntaxException | IOException e) {
                    System.out.println("Failed to convert image " + raw);
                    //e.printStackTrace();
                }
            }
        } else {
            System.out.println("Aborted.");
        }
    }

    private boolean setup() {
        Scanner scan = new Scanner(System.in);
        System.out.print("Input directory: ");
        inputDirectory = scan.nextLine();
        System.out.print("Output directory: ");
        outputDirectory = scan.nextLine();
        System.out.print("Name: ");
        name = scan.nextLine();
        //verify
        System.out.println("You have selected");
        System.out.println("In:   " + inputDirectory);
        System.out.println("Out:  " + outputDirectory);
        System.out.println("Name: " + name);
        System.out.println("Y/N?: ");
        String pick = scan.nextLine();
        return pick.trim().toUpperCase().matches("Y");
    }

    public static Image loadImage(File target) throws NullPointerException, URISyntaxException {
        System.out.println("Loading image resource " + target);
        ImageIcon ico = new ImageIcon(target.getAbsolutePath());
        //extract image
        Image test = ico.getImage();
        return test;
    }

    public static void main(String[] args) {
        new PlateMaker();
    }
}
