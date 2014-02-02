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
            if (nodes.get(a).getValue("name").matches(name)) {
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
            if (advance.matches("none")) {
                //automatically advance to next node
                String next = node.getValue("next");
                advance(next);
            } else if (advance.matches("END")) {
                //this is the end of the campaign
                node = null;
                messagePlayer("Campaign Complete", "Congratulations! You've finished '" + name + "'");
            }
        }
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
        } else {
            messagePlayer("Error", "Unexpected end of story.");
        }
    }

    public String getName() {
        return name;
    }
}
