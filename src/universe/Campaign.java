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
 * A complex, scripted, mission that the player can embark on. Has multiple
 * nodes involved.
 */
package universe;

import celestial.Ship.Ship;
import celestial.Ship.Station;
import engine.Entity;
import java.io.Serializable;
import java.util.ArrayList;
import lib.Parser;
import lib.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class Campaign implements Serializable {

    private String name;
    private Parser script;
    private Term node;
    //universe
    private Universe universe;

    public Campaign(Universe universe, String name) {
        this.universe = universe;
        this.name = name;
        System.out.println("Starting campaign: " + name);
        //load campaign script
        script = new Parser("campaign/" + name + ".txt");
        //locate the start of the campaign
        node = findNode("CAMPAIGN_START");
        if (node != null) {
            //campaign started
        } else {
            messagePlayer("Error", "This campaign does not exist!");
        }
    }

    private Term findNode(String name) {
        ArrayList<Term> nodes = script.getTermsOfType("Node");
        for (int a = 0; a < nodes.size(); a++) {
            if (nodes.get(a).getValue("name").equals(name)) {
                return nodes.get(a);
            }
        }
        return null;
    }

    private void messagePlayer(String name, String body) {
        universe.playerShip.composeMessage(universe.playerShip, name, body, null);
    }

    public void periodicUpdate(double tpf) {
        if (node != null) {
            //check to see if we can advance
            String advance = node.getValue("advance");
            if (advance.equals("none")) {
                next();
            } else if (advance.equals("END")) {
                //this is the end of the campaign
                node = null;
                messagePlayer("Campaign Complete", "Congratulations! You've finished '" + name + "'");
                //stop campaign by removing reference
                universe.getPlayerCampaigns().remove(this);
            } else if (advance.equals("SILENT_END")) {
                //this is the end of the campaign, but doesn't say anything
                node = null;
                //stop campaign by removing reference
                universe.getPlayerCampaigns().remove(this);
            } else {
                //these are the more complex ones which have parameters
                String[] split = advance.split("::");
                //condition::parameter
                if (split.length == 2) {
                    String condition = split[0].trim();
                    String parameter = split[1].trim();
                    if (condition.equals("ENTERSYSTEM")) {
                        //triggered when the player is in a certain system
                        if (universe.getPlayerShip().getCurrentSystem().getName().equals(parameter)) {
                            //trigger reached
                            next();
                        } else {
                            //not yet
                        }
                    } else if (condition.equals("DOCK")) {
                        //triggered when a player docks at a certain station
                        Ship player = universe.getPlayerShip();
                        if (player.isDocked()) {
                            Station host = player.getPort().getParent();
                            if (host.getName().equals(parameter)) {
                                //trigger reached
                                next();
                            } else {
                                //not yet
                            }
                        }
                    }
                }
            }
        }
    }

    private void next() {
        //automatically advance to next node
        String next = node.getValue("next");
        advance(next);
    }

    private void advance(String next) {
        Term tmp = findNode(next);
        if (tmp != null) {
            node = tmp;
            //display the chapter message
            String chapter = node.getValue("chapter");
            String objective = node.getValue("objective");
            String body = node.getValue("body");
            //compose
            String main = chapter + " /br/ /br/ ";
            if (body != null) {
                main += objective + " /br/ /br/ " + body;
            } else {
                main += objective;
            }
            //send
            messagePlayer(chapter, main);
            //call any functions this node has
            int a = 0;
            String function = null;
            while ((function = node.getValue("call" + a)) != null) {
                System.out.println("Calling " + function);
                //call this function
                String[] arr = function.split("::");
                if (arr.length == 2) {
                    do2FieldFunction(arr);
                } else if (arr.length == 4) {
                    do4FieldFunction(arr);
                }
                //increment
                a++;
            }
        } else {
            messagePlayer("Error", "Unexpected end of story.");
        }
    }

    private void do2FieldFunction(String[] arr) {
        //object::method
        String object = arr[0].trim();
        String method = arr[1].trim();
        if (object.equals("CURRENT_STATION")) {
            //modify the current station the player is at
            Station station = universe.getPlayerShip().getPort().getParent();
            if (station != null && universe.getPlayerShip().isDocked()) {
                if (method.equals("makeMortal()")) {
                    //make this station mortal
                    station.makeMortal();
                }
            } else {
                //do nothing
            }
        }
    }

    private void do4FieldFunction(String[] arr) {
        //type::system::entity::method
        String type = arr[0];
        String system = arr[1];
        String entity = arr[2];
        String method = arr[3];
        if (type.equals("STATION")) {
            //working with a space station
            for (int x = 0; x < universe.getSystems().size(); x++) {
                if (universe.getSystems().get(x).getName().matches(system)) {
                    SolarSystem sys = universe.getSystems().get(x);
                    for (int b = 0; b < sys.getEntities().size(); b++) {
                        Entity working = sys.getEntities().get(b);
                        if (working instanceof Station) {
                            Station tmpStat = (Station) working;
                            if (tmpStat.getName().equals(entity)) {
                                if (method.equals("makeMortal()")) {
                                    //make this station mortal
                                    tmpStat.makeMortal();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }
}
