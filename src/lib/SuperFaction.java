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

/**
 *
 * @author nwiehoff
 */
public class SuperFaction extends Faction {
    //loadout lists

    private ArrayList<Binling> patrols = new ArrayList<>();
    //station list
    private ArrayList<Binling> stations = new ArrayList<>();
    /*
     * Like a faction, except it stores information about loadout types,
     * station types, etc that god needs.
     */

    public SuperFaction(String name) {
        super(name);
        initStations();
        initLoadouts();
    }

    private void initStations() {
        //get a list of stations for this faction
        Parser sParse = new Parser("FACTIONS.txt");
        ArrayList<Parser.Term> terms = sParse.getTermsOfType("Stations");
        Parser.Term stat = null;
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
        //TODO
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

    public class Binling {
        /*
         * Class for storing a string paired with a double
         */

        private String str;
        private double num;

        public Binling(String str, double num) {
            this.str = str;
            this.num = num;
        }

        public String getString() {
            return str;
        }

        public void setString(String str) {
            this.str = str;
        }

        public double getDouble() {
            return num;
        }

        public void setDouble(double num) {
            this.num = num;
        }
    }
}