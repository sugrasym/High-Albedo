/*
 * Program for generating universes. This allows decent looking, arbitrarily
 * large worlds to be made automatically, and then manaully tuned.
 */
package lib;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;
import lib.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class WorldMaker {

    public WorldMaker() {
        //generate universe
        String out = generate(1, 10, 80, 100, 100, 40000, 120000, 500, 1500, 1, 3, 25000, 200000);
        //save
        AstralIO tmp = new AstralIO();
        tmp.writeFile("/tmp/UNIVERSE.txt", out);
        System.out.println(out);
    }

    public static void main(String[] args) {
        new WorldMaker();
    }

    public final String generate(int minPlanetsPerSystem, int maxPlanetsPerSystem, int minSystems, int maxSystems,
            int worldSize, int minSystemSize, int maxSystemSize, int minPlanetSize,
            int maxPlanetSize, int minNebulaPerSystem, int maxNebulaPerSystem, int minNebulaSize, int maxNebulaSize) {
        String ret = "";
        {
            //precache parsers
            Parser sky = new Parser("SKY.txt");
            ArrayList<Term> skyTypes = sky.getTermsOfType("Skybox");
            Parser stars = new Parser("PLANET.txt");
            ArrayList<Term> starTypes = stars.getTermsOfType("Star");
            /*Parser particle = new Parser("PARTICLE.txt");
            ArrayList<Term> nebTypes = particle.getTermsOfType("Nebula");*/
            Parser planets = new Parser("PLANET.txt");
            ArrayList<Term> planetTypes = planets.getTermsOfType("Planet");
            //start rng
            Random rnd = new Random();
            //determine the number of systems to make
            int numSystems = rnd.nextInt(maxSystems);
            if (numSystems < minSystems) {
                numSystems = minSystems;
            }
            //generate each system
            for (int a = 0; a < numSystems; a++) {
                System.out.println("Generating Universe - " + (a + 1) + "/" + numSystems);
                ArrayList<Simpling> objects = new ArrayList<>();
                String thisSystem = "";
                {
                    //pick name
                    String systemName = "System " + a;
                    //pick size
                    int size = rnd.nextInt(maxSystemSize);
                    if (size < minSystemSize) {
                        size = minSystemSize;
                    }
                    //determine skybox
                    int pick = rnd.nextInt(skyTypes.size());
                    //determine map location
                    double x = rnd.nextInt(worldSize * 2) - worldSize;
                    double y = rnd.nextInt(worldSize * 2) - worldSize;
                    //add some randomization
                    x += 2.0*rnd.nextDouble()-1;
                    y += 2.0*rnd.nextDouble()-1;
                    //create the system entry
                    thisSystem +=
                            "[System]\n"
                            + "name=" + systemName + "\n"
                            + "x=" + x + "\n"
                            + "y=" + y + "\n"
                            + "sky=" + skyTypes.get(pick).getValue("name") + "\n"
                            + "[/System]\n\n";
                    //get star types
                    pick = rnd.nextInt(starTypes.size());
                    //create a star in the relative center of the system
                    x = rnd.nextInt(size / 4) - size / 8;
                    y = rnd.nextInt(size / 4) - size / 8;
                    int r = rnd.nextInt(2 * maxPlanetSize);
                    if (r < maxPlanetSize) {
                        r = maxPlanetSize;
                    }
                    int seed = rnd.nextInt();
                    thisSystem +=
                            "[Star]\n"
                            + "name=" + systemName + "\n"
                            + "system=" + systemName + "\n"
                            + "texture=" + starTypes.get(pick).getValue("name") + "\n"
                            + "x=" + x + "\n"
                            + "y=" + y + "\n"
                            + "d=" + 2*r + "\n"
                            + "seed=" + seed + "\n"
                            + "[/Star]\n\n";
                    //add a simpling for testing
                    objects.add(new Simpling(new Point2D.Float((float)x,(float)y), 4 * r));
                    /*
                     * CREATE NEBULA
                     * 
                     */
                    /*int numNebula = rnd.nextInt(maxNebulaPerSystem);
                    if (numNebula < minNebulaPerSystem) {
                        numNebula = minNebulaPerSystem;
                    }
                    for (int b = 0; b < numNebula; b++) {
                        //pick texture
                        pick = rnd.nextInt(nebTypes.size());
                        Term type = nebTypes.get(pick);
                        //pick name
                        String name = "Nebula " + b;
                        //pick position
                        x = rnd.nextInt(size * 4) - size * 2;
                        y = 0;
                        z = rnd.nextInt(size * 4) - size * 2;
                        //pick dimension
                        r = rnd.nextInt(Math.min(2 * size, maxNebulaSize));
                        if (r < minNebulaSize) {
                            r = minNebulaSize;
                        }
                        int hScale = rnd.nextInt(3) + 2;
                        int h = r / hScale;
                        //pick color
                        float red = rnd.nextFloat() / 2;
                        float green = rnd.nextFloat() / 2;
                        float blue = rnd.nextFloat() / 2;
                        //seed
                        seed = rnd.nextInt();
                        //generate entry
                        thisSystem +=
                                "[Nebula]\n"
                                + "name=" + name + "\n"
                                + "system=" + systemName + "\n"
                                + "type=" + type.getValue("name") + "\n"
                                + "color=" + red + "," + green + "," + blue + "," + 1 + "\n"
                                + "x=" + x + "\n"
                                + "y=" + y + "\n"
                                + "z=" + z + "\n"
                                + "l=" + r + "\n"
                                + "w=" + h + "\n"
                                + "h=" + r + "\n"
                                + "seed=" + seed + "\n"
                                + "[/Nebula]\n\n";
                    }*/
                    /*
                     * CREATE PLANETS
                     */
                    //get list of planet assets
                    int numPlanets = rnd.nextInt(maxPlanetsPerSystem);
                    if (numPlanets < minPlanetsPerSystem) {
                        numPlanets = minPlanetsPerSystem;
                    }
                    for (int b = 0; b < numPlanets; b++) {
                        //pick texture
                        pick = rnd.nextInt(planetTypes.size());
                        Term type = planetTypes.get(pick);
                        String texture = type.getValue("name");
                        //pick name
                        String name = "Planet " + b;
                        //pick seed
                        seed = rnd.nextInt();
                        //generate position
                        x = rnd.nextInt(size * 2) - size;
                        y = rnd.nextInt(size * 2) - size;
                        //generate the radius
                        r = rnd.nextInt(maxPlanetSize);
                        if (r < minPlanetSize) {
                            r = minPlanetSize;
                        }
                        //create a simpling for testing
                        Simpling test = new Simpling(new Point2D.Float((float)x,(float)y), r);
                        boolean safe = true;
                        for (int c = 0; c < objects.size(); c++) {
                            if (objects.get(c).collideWith(test)) {
                                safe = false;
                                break;
                            }
                        }
                        //if it is safe add it
                        if (safe) {
                            thisSystem +=
                                    "[Planet]\n"
                                    + "name=" + name + "\n"
                                    + "system=" + systemName + "\n"
                                    + "texture=" + texture + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "d=" + 2*r + "\n"
                                    + "seed=" + seed + "\n"
                                    + "[/Planet]\n\n";
                        }
                    }
                }
                ret += thisSystem;
            }
        }
        return ret;
    }

    public class Simpling {
        /*
         * Class used for storing the location and radius of an object for
         * the sole purpose of avoiding collisions.
         */

        private Point2D.Float loc;
        private float rad;

        public Simpling(Point2D.Float loc, float rad) {
            this.loc = loc;
            this.rad = rad;
        }

        public boolean collideWith(Simpling test) {
            Point2D.Float testLoc = (Point2D.Float)test.getLoc().clone();
            float testRad = test.getRad();
            //add the radii to get the min separation
            float minRad = testRad + rad;
            //multiply this rad by 1.5 so they can't be kissing each other
            minRad *= 1.5f;
            //determine if they are colliding using distance
            float dist = (float) loc.distance(testLoc);
            if (dist < minRad) {
                return true;
            }
            return false;
        }

        public Point2D getLoc() {
            return loc;
        }

        public float getRad() {
            return rad;
        }
    }
}
