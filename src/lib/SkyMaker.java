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
 * Program for automatically taking the plates found in the resource directory
 * and generating a sky file for them.
 */
package lib;

import java.io.File;
import java.util.Scanner;

/**
 *
 * @author nwiehoff
 */
public class SkyMaker {

    String inputDirectory;
    String outputFile;

    public SkyMaker() {
        if (setup()) {
            //this string will hold the final product
            String out = "";
            //get a list of all files in the input directory
            File inDir = new File(inputDirectory);
            File[] listOfFiles = inDir.listFiles();
            for (int a = 0; a < listOfFiles.length; a++) {
                try {
                    //get file
                    File raw = listOfFiles[a];
                    //pull the name from before the file extension
                    String name = raw.getName().replaceFirst("[.][^.]+$", "");
                    if (!name.trim().toLowerCase().matches("base_plate")) {
                        //construct this entry
                        String entry = "[Skybox]\n"
                                + "name=" + name + "\n"
                                + "asset=" + raw.getName() + "\n"
                                + "[/Skybox]\n\n";
                        //append
                        out += entry;
                    } else {
                        System.out.println("Skipping the base plate.");
                    }
                } catch (Exception e) {
                    System.out.println("Processing failed for file " + listOfFiles[a].getName());
                    e.printStackTrace();
                }
                System.out.println("Processed file " + (a + 1) + " / " + listOfFiles.length);
            }
            //write out the file
            AstralIO.writeFile(outputFile, out);
            System.out.println("Sky file generation complete.");
        } else {
            System.out.println("Aborted.");
        }
    }

    private boolean setup() {
        Scanner scan = new Scanner(System.in);
        System.out.print("Input directory: ");
        inputDirectory = scan.nextLine();
        System.out.print("Output file: ");
        outputFile = scan.nextLine();
        //verify
        System.out.println("You have selected");
        System.out.println("In:   " + inputDirectory);
        System.out.println("Out:  " + outputFile);
        System.out.println("Y/N?: ");
        String pick = scan.nextLine();
        if (pick.trim().toUpperCase().matches("Y")) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        new SkyMaker();
    }
}
