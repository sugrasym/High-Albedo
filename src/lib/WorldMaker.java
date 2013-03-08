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
        /*//sysling test
         syslingTest();*/
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
            //generate syslings
            ArrayList<Sysling> syslings = new ArrayList<>();
            for (int a = 0; a < numSystems; a++) {
                //determine map location
                double x = rnd.nextInt(worldSize * 2) - worldSize;
                double y = rnd.nextInt(worldSize * 2) - worldSize;
                //pick name
                String name = "System " + a;
                //make sysling
                Sysling test = new Sysling(name, new Point2D.Double(x, y));
                //check for collission
                boolean safe = true;
                for (int v = 0; v < syslings.size(); v++) {
                    if (syslings.get(v).collideWith(test)) {
                        safe = false;
                        break;
                    }
                }
                //add if safe
                if (safe) {
                    syslings.add(test);
                }
            }
            //stort syslings
            for (int a = 0; a < syslings.size(); a++) {
                syslings.get(a).sortBuddy(syslings);
            }
            //generate each system
            for (int a = 0; a < syslings.size(); a++) {
                System.out.println("Generating Universe - " + (a + 1) + "/" + numSystems);
                ArrayList<Simpling> objects = new ArrayList<>();
                String thisSystem = "";
                {
                    //get sysling
                    Sysling sys = syslings.get(a);
                    double x = sys.getLoc().x;
                    double y = sys.getLoc().y;
                    String systemName = sys.getName();
                    //pick size
                    int size = rnd.nextInt(maxSystemSize);
                    if (size < minSystemSize) {
                        size = minSystemSize;
                    }
                    //determine skybox
                    int pick = rnd.nextInt(skyTypes.size());
                    //add some randomization
                    x += 2.0 * rnd.nextDouble() - 1;
                    y += 2.0 * rnd.nextDouble() - 1;
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
                            + "d=" + 2 * r + "\n"
                            + "seed=" + seed + "\n"
                            + "[/Star]\n\n";
                    //add a simpling for testing
                    objects.add(new Simpling(new Point2D.Float((float) x, (float) y), 4 * r));
                    /*
                     * CREATE JUMPHOLES
                     */
                    //calculate the number of connections to make
                    int density = rnd.nextInt(4) + 1;
                    for (int v = 0; v < density; v++) {
                        //get the sysling to connect to
                        Sysling in = sys;
                        Sysling out = sys.findBuddy(null, v + 1);
                        if (!in.connectedTo(out) && !out.connectedTo(in)) {
                            //name holes
                            String inName = out.getName() + " Jumphole";
                            String outName = sys.getName() + " Jumphole";
                            //build a bridge
                        /*
                             * [Jumphole]
                             name=System 9 Gate
                             system=System 16
                             out=System 9/System 16 Gate
                             x=-6000
                             y=-2000
                             [/Jumphole]
                             */
                            //build in gate
                            x = rnd.nextInt(size * 2) - size;
                            y = rnd.nextInt(size * 2) - size;
                            thisSystem +=
                                    "[Jumphole]\n"
                                    + "name=" + inName + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "system=" + in.getName() + "\n"
                                    + "out=" + out.getName() + "/" + outName + "\n"
                                    + "[/Jumphole]\n\n";
                            //build out gate
                            x = rnd.nextInt(size * 2) - size;
                            y = rnd.nextInt(size * 2) - size;
                            thisSystem +=
                                    "[Jumphole]\n"
                                    + "name=" + outName + "\n"
                                    + "x=" + x + "\n"
                                    + "y=" + y + "\n"
                                    + "system=" + out.getName() + "\n"
                                    + "out=" + in.getName() + "/" + inName + "\n"
                                    + "[/Jumphole]\n\n";
                            //inform simplings
                            sys.addConnection(out);
                            out.addConnection(sys);
                        }
                    }
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
                        Simpling test = new Simpling(new Point2D.Float((float) x, (float) y), r);
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
                                    + "d=" + 2 * r + "\n"
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

    private void syslingTest() {
        ArrayList<Sysling> test = new ArrayList<>();
        Random rnd = new Random(5);
        //make some syslings
        for (int a = 0; a < 50; a++) {
            String name = "Sysling " + a;
            double sx = rnd.nextInt(1000) - 500;
            double sy = rnd.nextInt(1000) - 500;
            test.add(new Sysling(name, new Point2D.Double(sx, sy)));
        }
        //make them find their buddy
        for (int a = 0; a < test.size(); a++) {
            test.get(a).sortBuddy(test);
        }
        //get a print out
        Sysling t = test.get(0);
        System.out.println("Mapping from " + t.getName());
        for (int a = 0; a < test.size(); a++) {
            Sysling _0th = t.findBuddy(test, a);
            System.out.println(_0th.getName() + " " + _0th.distance(t));
        }
    }

    public class Sysling {
        /*
         * Solar system template used for jump hole mapping.
         */

        private Point2D.Double loc;
        private Sysling[] neighbors;
        private ArrayList<Sysling> connections = new ArrayList<>();
        private String name;

        public Sysling(String name, Point2D.Double loc) {
            this.loc = loc;
            this.name = name;
        }

        public void addConnection(Sysling sys) {
            connections.add(sys);
        }

        public boolean connectedTo(Sysling test) {
            return connections.contains(test);
        }

        public void sortBuddy(ArrayList<Sysling> verse) {
            /*
             * Sorts the syslings by distance ascending.
             */
            Sysling[] arr = (Sysling[]) verse.toArray(new Sysling[0]);
            for (int a = 0; a < arr.length; a++) {
                for (int b = 1; b < arr.length - a; b++) {
                    if (distance(arr[b - 1]) > distance(arr[b])) {
                        Sysling tmp = arr[b];
                        arr[b] = arr[b - 1];
                        arr[b - 1] = tmp;
                    }
                }
            }
            neighbors = arr;
        }

        public Sysling findBuddy(ArrayList<Sysling> verse, int place) {
            /*
             * Finds Nth closest sysling to this one. Nth = place.
             */
            if (neighbors == null) {
                sortBuddy(verse);
            }
            return neighbors[place];
        }

        public boolean collideWith(Sysling test) {
            if (test.getName().matches(name)) {
                return true;
            } else {
                return false;
            }
        }

        public double distance(Sysling comp) {
            return loc.distance(comp.getLoc());
        }

        public Point2D.Double getLoc() {
            return loc;
        }

        public String getName() {
            return name;
        }

        public String toString() {
            return name;
        }
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
            Point2D.Float testLoc = (Point2D.Float) test.getLoc().clone();
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
