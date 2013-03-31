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
 * Extended version of the faction class for use by God when repairing and
 * maintaining the universe.
 */
package lib;

import java.util.ArrayList;
import lib.Parser.Term;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class SuperFaction extends Faction {
    //universe

    private Universe universe;
    //sov
    private ArrayList<SolarSystem> sov = new ArrayList<>();
    private ArrayList<SolarSystem> sovHost = new ArrayList<>();
    //loadout lists
    private ArrayList<Binling> patrols = new ArrayList<>();
    private ArrayList<Binling> traders = new ArrayList<>();
    private ArrayList<Binling> taxies = new ArrayList<>();
    //station list
    private ArrayList<Binling> stations = new ArrayList<>();
    /*
     * Like a faction, except it stores information about loadout types,
     * station types, etc that god needs.
     */

    public SuperFaction(Universe universe, String name) {
        super(name);
        this.universe = universe;
        initStations();
        initLoadouts();
        initSov();
        initSovHosts();
    }

    private void initStations() {
        //get a list of stations for this faction
        Parser sParse = new Parser("FACTIONS.txt");
        ArrayList<Term> terms = sParse.getTermsOfType("Stations");
        Term stat = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").matches(getName())) {
                stat = terms.get(a);
            }
        }
        if (stat != null) {
            //get types of stations
            int a = 0;
            String type = "";
            while ((type = stat.getValue("station" + a)) != null) {
                //get station info
                String ty = type.split(",")[0];
                double spread = Float.parseFloat(type.split(",")[1]);
                stations.add(new Binling(ty, spread));
                //iterate
                a++;
            }
        } else {
            System.out.println(getName() + " doesn't have any stations!");
        }
    }

    private void initLoadouts() {

        //get a list of patrol loadouts for this faction
        Parser sParse = new Parser("FACTIONS.txt");
        ArrayList<Term> terms = sParse.getTermsOfType("Loadout");
        Term stat = null;
        for (int a = 0; a < terms.size(); a++) {
            if (terms.get(a).getValue("name").matches(getName())) {
                stat = terms.get(a);
            }
        }
        if (stat != null) {
            /*
             * Patrol loadouts
             */
            {
                int a = 0;
                String type = "";
                while ((type = stat.getValue("patrol" + a)) != null) {
                    //get patrol info
                    String ty = type.split(",")[0];
                    double spread = Float.parseFloat(type.split(",")[1]);
                    patrols.add(new Binling(ty, spread));
                    //iterate
                    a++;
                }
            }
            /*
             * Trader loadouts
             */
            {
                int a = 0;
                String type = "";
                while ((type = stat.getValue("trader" + a)) != null) {
                    //get trader info
                    String ty = type.split(",")[0];
                    double spread = Float.parseFloat(type.split(",")[1]);
                    traders.add(new Binling(ty, spread));
                    //iterate
                    a++;
                }
            }
            /*
             * Taxie loadouts
             */
            {
                int a = 0;
                String type = "";
                while ((type = stat.getValue("taxie" + a)) != null) {
                    //get taxie info
                    String ty = type.split(",")[0];
                    double spread = Float.parseFloat(type.split(",")[1]);
                    taxies.add(new Binling(ty, spread));
                    //iterate
                    a++;
                }
            }
        } else {
            System.out.println(getName() + " doesn't have any loadouts!");
        }
    }

    private void initSov() {
        /*
         * Makes a list of all the systems this faction controls.
         */
        ArrayList<SolarSystem> systems = universe.getSystems();
        for (int a = 0; a < systems.size(); a++) {
            if (systems.get(a).getOwner().matches(getName())) {
                sov.add(systems.get(a));
            }
        }
    }
    
    private void initSovHosts() {
        /*
         * Makes a list of all the systems this faction can spawn in if it is
         * not a sov holder.
         */
        ArrayList<SolarSystem> systems = universe.getSystems();
        for (int a = 0; a < systems.size(); a++) {
            if (canSpawnIn(universe.getSystems().get(a))) {
                sovHost.add(systems.get(a));
            }
        }
    }

    public ArrayList<Binling> getPatrols() {
        return patrols;
    }

    public void setPatrols(ArrayList<Binling> patrols) {
        this.patrols = patrols;
    }

    public ArrayList<Binling> getStations() {
        return stations;
    }

    public void setStations(ArrayList<Binling> stations) {
        this.stations = stations;
    }

    public ArrayList<SolarSystem> getSov() {
        return sov;
    }

    public ArrayList<SolarSystem> getSovHost() {
        return sovHost;
    }

    private boolean canSpawnIn(SolarSystem get) {
        for(int a = 0; a < hosts.size(); a++) {
            if(get.getOwner().matches(hosts.get(a))) {
                return true;
            }
        }
        return false;
    }
}