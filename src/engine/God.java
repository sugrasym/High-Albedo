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
 * Responsible for keeping the universe active. Specifically,
 * 1. Manage patrols and traders - spawn replacements as needed
 * 2. Manage stations - spawn replacements as needed
 * 3. Add 'fun' disasters to the universe. - TODO
 */
package engine;

import celestial.Celestial;
import celestial.Jumphole;
import static celestial.Jumphole.MAX_FRONTIER_FLUX;
import static celestial.Jumphole.MIN_FRONTIER_FLUX;
import celestial.Ship.Ship;
import celestial.Ship.Ship.Behavior;
import celestial.Ship.Station;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;
import lib.Binling;
import lib.Faction;
import lib.Parser;
import lib.Parser.Term;
import lib.SuperFaction;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class God implements EngineElement {

    private final Universe universe;
    private final ArrayList<SuperFaction> factions = new ArrayList<>();
    private final Random rnd = new Random();
    boolean firstRun = true;
    //sample
    private final char[] basicSample = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
        'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '1', '2',
        '3', '4', '5', '6', '7', '8', '9', '0'};
    //timing
    private long lastFrame;

    public God(Universe universe) {
        this.universe = universe;
        //generate lists
        initFactions();

        //initialize frontier space if needed
        initFrontier();
    }

    private void initFactions() {
        //make a list of all factions
        Parser fParse = Universe.getCache().getFactionCache();
        ArrayList<Term> terms = fParse.getTermsOfType("Faction");
        for (int a = 0; a < terms.size(); a++) {
            factions.add(new SuperFaction(universe, terms.get(a).getValue("name")));
        }
    }

    @Override
    public void periodicUpdate() {
        //get time since last frame
        long dt = System.nanoTime() - lastFrame;
        //calculate time per frame
        double tpf = Math.abs(dt / 1000000000.0);
        //only run it every 16 minutes because it's a performance hog!
        if (tpf > 960 || firstRun) {
            firstRun = false;
            //store time
            lastFrame = System.nanoTime();
            //update
            Thread s = new Thread(() -> {
                checkStations();
                checkPatrols();
                checkTraders();
                checkMerchants();
                checkFrontierJumpholes();
                System.out.println("God cycled.");
            });
            s.start();
        }
    }

    /*
     * Hooks
     */
    private void checkFrontierJumpholes() {
        int count = 0;

        //count current number of frontier jumpholes
        for (int a = 0; a < universe.getSystems().size(); a++) {
            count += (int) universe.getSystems().get(a).getJumpholeList().stream()
                    .filter((jh) -> ((Jumphole) jh).getCurrentSystem().isFrontier()).count();
        }

        //add missing ones
        while (count < 200) {
            //start is always frontier
            SolarSystem start = universe.getFrontierSpace()
                    .get(rnd.nextInt(universe.getFrontierSpace().size() - 1));

            //end can be any other system
            SolarSystem end = universe.getSystems()
                    .get(rnd.nextInt(universe.getSystems().size() - 1));

            if (start != end) {
                //make sure there are no existing paths
                if (!start.getJumpholeList().stream()
                        .anyMatch((jh) -> ((Jumphole) jh)
                                .getOutGate().getCurrentSystem() == end)) {
                    if (!end.getJumpholeList().stream()
                            .anyMatch((jh) -> ((Jumphole) jh)
                                    .getOutGate().getCurrentSystem() == start)) {
                        //create jumpholes
                        Jumphole jh1 = new Jumphole("Transient Jumphole", universe);
                        Jumphole jh2 = new Jumphole("Transient Jumphole", universe);

                        jh1.setX((rnd.nextDouble() - 0.5) * 48000);
                        jh1.setY((rnd.nextDouble() - 0.5) * 48000);

                        jh2.setX((rnd.nextDouble() - 0.5) * 48000);
                        jh2.setY((rnd.nextDouble() - 0.5) * 48000);

                        //create connection
                        jh1.setOutGate(jh2);
                        jh2.setOutGate(jh1);

                        //setup flux for frontier jumpholes
                        double flux = (new Random().nextDouble()
                                * (MAX_FRONTIER_FLUX - MIN_FRONTIER_FLUX))
                                + MIN_FRONTIER_FLUX;

                        jh1.setFlux(flux);
                        jh1.setMaxFlux(flux);

                        jh2.setFlux(flux);
                        jh2.setMaxFlux(flux);

                        //add to systems
                        start.putEntityInSystem(jh1);
                        end.putEntityInSystem(jh2);

                        System.out.println("Added transient jumphole "
                                + start.getName() + " -> " + end.getName());

                        //increment counter
                        count++;
                    }
                }
            }
        }
    }

    private void checkMerchants() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doMerchants(factions.get(a));
        }
    }

    private void checkTraders() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doTraders(factions.get(a));
        }
    }

    private void checkPatrols() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doPatrols(factions.get(a));
        }
    }

    private void checkStations() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doStations(factions.get(a));
        }
        //make sure none are ontop of each other
        for (int a = 0; a < universe.getSystems().size(); a++) {
            SolarSystem curr = universe.getSystems().get(a);
            ArrayList<Entity> stations = curr.getStationList();
            for (int y = 0; y < stations.size(); y++) {
                Station prim = (Station) stations.get(y);
                for (int x = 0; x < stations.size(); x++) {
                    Station sub = (Station) stations.get(x);
                    //make sure they aren't the same
                    if (stations.get(y) != stations.get(x)) {
                        //check for collission
                        if (prim.collideWith(sub)) {
                            //push the sub station away
                            sub.setX(rnd.nextInt(64000) - 32000);
                            sub.setY(rnd.nextInt(64000) - 32000);
                            //report
                            System.out.println("Station " + sub + " was moved.");
                        }
                    }
                }
            }
        }
    }

    /*
     * Implementations
     */
    private void doStations(SuperFaction faction) {
        /*
         * 1. Make sure this faction has stations
         * 2. Count the number of each type
         * 3. Spawn more of each as needed.
         */
        if (faction.getStations().size() > 0) {
            int count[] = new int[faction.getStations().size()];
            if (faction.isEmpire()) {
                //this faction holds sov
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each station
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String type = faction.getStations().get(v).getString();
                        int num = countStations(faction, sys, type);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each station
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String type = faction.getStations().get(v).getString();
                        int num = countStations(faction, sys, type);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density;
                //calculate density based on entity type
                if (faction.isEmpire()) {
                    density = 1 + faction.getStations().get(a).getDouble() * faction.getSov().size();
                } else {
                    density = 1 + faction.getStations().get(a).getDouble() * faction.getSovHost().size();
                }
                //generate if needed
                //System.out.println(faction.getStations().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    //celestials
                    Celestial host;
                    SolarSystem pick;
                    //branch based on entity type
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //pick a point near the planet
                    double x = host.getX() + rnd.nextInt(10000) - 5000;
                    double y = host.getY() + rnd.nextInt(10000) - 5000;
                    //make a point
                    Point2D.Double pnt = new Point2D.Double(x, y);
                    //spawn
                    spawnStation(faction, pick, pnt, faction.getStations().get(a));
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private void doMerchants(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getMerchants().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getMerchants().size()];
            if (faction.isEmpire()) {
                //get a count of the number of traders in each system
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getMerchants().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getMerchants().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getMerchants().get(a).getDouble();
                //System.out.println(faction.getMerchants().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    Celestial host;
                    SolarSystem pick;
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //pick a point near the planet
                    double x = host.getX() + rnd.nextInt(10000) - 5000;
                    double y = host.getY() + rnd.nextInt(10000) - 5000;
                    //make a point
                    Point2D.Double pnt = new Point2D.Double(x, y);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getMerchants().get(a), Behavior.UNIVERSE_TRADE);
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private void doTraders(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getTraders().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getTraders().size()];
            if (faction.isEmpire()) {
                //get a count of the number of traders in each system
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getTraders().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getTraders().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getTraders().get(a).getDouble();
                //System.out.println(faction.getTraders().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    Celestial host;
                    SolarSystem pick;
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //pick a point near the planet
                    double x = host.getX() + rnd.nextInt(10000) - 5000;
                    double y = host.getY() + rnd.nextInt(10000) - 5000;
                    //make a point
                    Point2D.Double pnt = new Point2D.Double(x, y);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getTraders().get(a), Behavior.SECTOR_TRADE);
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    private void doPatrols(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getPatrols().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getPatrols().size()];
            if (faction.isEmpire()) {
                //get a count of the number of patrols in each system
                for (int a = 0; a < faction.getSov().size(); a++) {
                    SolarSystem sys = faction.getSov().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getPatrols().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            } else {
                //this faction does not own space - count the space it lives in
                for (int a = 0; a < faction.getSovHost().size(); a++) {
                    SolarSystem sys = faction.getSovHost().get(a);
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getPatrols().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getPatrols().get(a).getDouble();
                //System.out.println(faction.getPatrols().get(a).getString() + " " + count[a]);
                while (count[a] < density) {
                    Celestial host;
                    SolarSystem pick;
                    if (faction.isEmpire()) {
                        //pick a system this faction owns
                        ArrayList<SolarSystem> sov = faction.getSov();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no sov");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    } else {
                        //space belonging to this faction's host
                        ArrayList<SolarSystem> sov = faction.getSovHost();
                        if (sov.size() > 0) {
                            pick = sov.get(rnd.nextInt(sov.size()));
                        } else {
                            System.out.println(faction.getName() + " has no hosts");
                            pick = universe.getSystems().get(rnd.nextInt(universe.getSystems().size()));
                        }
                        //pick a planet in this system
                        ArrayList<Entity> planets = pick.getCelestialList();
                        host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    }
                    //pick a point near the planet
                    double x = host.getX() + rnd.nextInt(10000) - 5000;
                    double y = host.getY() + rnd.nextInt(10000) - 5000;
                    //make a point
                    Point2D.Double pnt = new Point2D.Double(x, y);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getPatrols().get(a), Behavior.PATROL);
                    //increment count
                    count[a]++;
                }
            }
        }
    }

    /*
     * Tools
     */
    public int countStations(Faction faction, SolarSystem system, String type) {
        int count = 0;
        {
            //get station list
            ArrayList<Entity> stations = system.getStationList();
            for (int a = 0; a < stations.size(); a++) {
                Station tmp = (Station) stations.get(a);
                if (tmp.getState() == Ship.State.ALIVE) {
                    if (tmp.getFaction().equals(faction.getName())) {
                        if (tmp.getType().equals(type)) {
                            count++;
                        }
                    }
                } else if (tmp.getState() == Ship.State.DEAD) {
                    System.out.println(tmp.getName() + " dead but not cleaned up");
                }
            }
        }
        return count;
    }

    public int countShipsByLoadout(Faction faction, SolarSystem system, String loadout) {
        int count = 0;
        {
            //get ship list
            ArrayList<Entity> ships = system.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp.getState() == Ship.State.ALIVE) {
                    if (tmp.getFaction().equals(faction.getName())) {
                        if (tmp.getTemplate().equals(loadout)) {
                            count++;
                        }
                    }
                } else if (tmp.getState() == Ship.State.DEAD) {
                    System.out.println(tmp.getName() + " dead but not cleaned up");
                }
            }
        }
        return count;
    }

    public int countShipsByRole(Faction faction, SolarSystem system, Behavior behavior) {
        int count = 0;
        {
            //get ship list
            ArrayList<Entity> ships = system.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp.getFaction().equals(faction.getName())) {
                    if (tmp.getBehavior() == behavior) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private Station makeStation(String type, String name, String faction) {
        Station ret = new Station(name, type);
        ret.setFaction(faction);
        ret.init(false);
        return ret;
    }

    private Ship makeShip(String template, String name, String faction) {
        /*
         * Generates a ship from a template.
         */
        Ship ret;
        {
            String cargo = "";
            String install = "";
            String ship = "Mass Testing Brick";
            String cargoScan = "false";
            String minCourage = null;
            String maxCourage = null;
            boolean plotOffer = false;
            if (template != null) {
                //load this template
                Parser lParse = Universe.getCache().getLoadoutCache();
                ArrayList<Term> lods = lParse.getTermsOfType("Loadout");
                for (int a = 0; a < lods.size(); a++) {
                    if (lods.get(a).getValue("name").equals(template)) {
                        //get terms
                        cargo = lods.get(a).getValue("cargo");
                        install = lods.get(a).getValue("install");
                        ship = lods.get(a).getValue("ship");
                        String cs = lods.get(a).getValue("cargoScan");
                        if (cs != null) {
                            cargoScan = cs;
                        }
                        //bailing
                        minCourage = lods.get(a).getValue("minCourage");
                        maxCourage = lods.get(a).getValue("maxCourage");
                        //plot offers
                        String pOffer = lods.get(a).getValue("plotOffer");
                        if (pOffer != null) {
                            plotOffer = Boolean.parseBoolean(pOffer);
                        }
                        break;
                    }
                }
            }

            //create ship
            ret = new Ship(name, ship);
            if (template != null) {
                ret.setTemplate(template);
            }
            //check template
            ret.setEquip(install);
            ret.setFaction(faction);
            ret.init(false);
            ret.addInitialCargo(cargo);
            ret.setScanForContraband(Boolean.parseBoolean(cargoScan));
            ret.setPlotOffer(plotOffer);
            //bailing
            if (minCourage != null && maxCourage != null) {
                double lowC = Double.parseDouble(minCourage);
                double hiC = Double.parseDouble(maxCourage);
                //pick a random number between these
                double d = hiC - lowC;
                double del = rnd.nextDouble() * d;
                double pick = lowC + del;
                //store courage
                ret.setCourage(pick);
            }
        }
        return ret;
    }

    public String randomIDTag(int size, char[] sample) {
        String ret = "";
        {
            for (int a = 0; a < size; a++) {
                char pick = sample[rnd.nextInt(sample.length)];
                ret += pick;
            }
        }
        return ret;
    }

    public void spawnShip(Faction faction, SolarSystem system, Point2D.Double loc, Binling loadout, Behavior behavior) {
        String name = loadout.getString() + " " + randomIDTag(5, basicSample);
        //get a basic ship to work with
        Ship tmp = makeShip(loadout.getString(), name, faction.getName());
        //push coordinates
        tmp.setX(loc.getX());
        tmp.setY(loc.getY());
        //push behavior
        tmp.setBehavior(behavior);
        //finalize
        tmp.setCurrentSystem(system);
        system.putEntityInSystem(tmp);
        //report
        //System.out.println("Spawned " + loadout.getString() + " in " + system.getName() + " for " + faction.getName());
    }

    public void spawnShip(Faction faction, SolarSystem system, Point2D.Double loc, String loadout, String name, Behavior behavior, String group, boolean plotShip) {
        //get a basic ship to work with
        Ship tmp = makeShip(loadout, name, faction.getName());
        //push coordinates
        tmp.setX(loc.getX());
        tmp.setY(loc.getY());
        //push behavior
        tmp.setBehavior(behavior);
        //finalize
        tmp.setCurrentSystem(system);
        tmp.setGroup(group);
        tmp.setPlotShip(plotShip);
        system.putEntityInSystem(tmp);
        //report
        //System.out.println("Spawned " + loadout.getString() + " in " + system.getName() + " for " + faction.getName());
    }

    public void spawnStation(Faction faction, SolarSystem system, Point2D.Double loc, Binling loadout) {
        String name = loadout.getString() + " " + randomIDTag(5, basicSample);
        //get a basic ship to work with
        Station tmp = makeStation(loadout.getString(), name, faction.getName());
        //push coordinates
        tmp.setX(loc.getX());
        tmp.setY(loc.getY());
        //finalize
        tmp.setCurrentSystem(system);
        system.putEntityInSystem(tmp);
        //report
        System.out.println("Spawned " + loadout.getString() + " in " + system.getName() + " for " + faction.getName());
    }

    public void spawnStation(Faction faction, SolarSystem system, Point2D.Double loc, String type, String name, String group, boolean plotShip) {
        //get a basic ship to work with
        Station tmp = makeStation(type, name, faction.getName());
        //push coordinates
        tmp.setX(loc.getX());
        tmp.setY(loc.getY());
        //finalize
        tmp.setCurrentSystem(system);
        tmp.setGroup(group);
        tmp.setPlotShip(plotShip);
        system.putEntityInSystem(tmp);
        //report
        System.out.println("Spawned " + type + " in " + system.getName() + " for " + faction.getName());
    }

    /*
    * Frontier Space
     */
    private void initFrontier() {
        //check if there is already frontier space
        if (universe.getFrontierSpace().isEmpty()) {
            //initialize frontier space
            /*
            * Although everyone will have the same settled systems, I'd like the
            * frontier systems to be different each time you create a new game.
            * This will make sure they are something you can always go explore.
             */

            int count = (int) (new Random().nextDouble() * (Universe.MAX_FRONTIER_SYSTEMS
                    - Universe.MIN_FRONTIER_SYSTEMS)) + Universe.MIN_FRONTIER_SYSTEMS;

            for (int a = 0; a < count; a++) {
                System.out.println("Generating frontier system " + (a + 1) + "/" + count);
                universe.getSystems().add(FrontierUtil.randomFrontierSystem(universe));
            }
        }
    }

    private static class FrontierUtil {

        private static final Random RND = new Random();

        public static SolarSystem randomFrontierSystem(Universe universe) {
            SolarSystem system = new SolarSystem(universe, "TODO - Name");
            system.setSystemType(SolarSystem.Type.FRONTIER);
            system.setOwner("Frontier");

            //set position
            system.setX(RND.nextInt());
            system.setY(RND.nextInt());

            //set name
            ArrayList<SolarSystem> frontier = universe.getFrontierSpace();
            while (true) {
                String n = "F" + RND.nextInt(5 * Universe.MAX_FRONTIER_SYSTEMS);
                if (frontier.stream().anyMatch((l) -> l.getName().equals(n))) {
                    continue;
                } else {
                    system.setName(n);
                    break;
                }
            }

            //set backplate
            ArrayList<Term> backs = Universe.getCache().getSkyCache().getTermsOfType("Skybox");
            system.setBack(backs.get(RND.nextInt(backs.size() - 1)).getValue("asset"));

            //todo: add celestials
            return system;
        }
    }
}
