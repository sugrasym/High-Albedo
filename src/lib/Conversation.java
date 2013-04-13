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
 * System for managing "conversations" between the player and NPCs. It is not
 * a messaging system for use by NPCs to talk to other NPCs.
 */
package lib;

import celestial.Ship.Ship;
import java.io.Serializable;
import java.util.ArrayList;
import lib.Parser.Term;
import universe.Mission;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Conversation implements Serializable {

    private ArrayList<AstralMessage> nodes = new ArrayList<>();
    private String name;
    private AstralMessage currentNode;
    private Ship owner;
    //mission
    private Mission tmpMission;

    public Conversation(Ship owner, String name, String startNode) {
        this.name = name;
        this.owner = owner;
        init(owner);
        findStart(startNode);
    }

    public void periodicUpdate(double tpf) {
        try {
            if (currentNode != null) {
                if (!currentNode.wasSent()) {
                    owner.getUniverse().getPlayerShip().receiveMessage(currentNode);
                } else {
                    //wait
                }
            } else {
                //done
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findStart(String startNode) {
        currentNode = findNode(startNode);
    }

    private void init(Ship owner) {
        Parser tmp = Universe.getCache().getConversationCache();
        ArrayList<Term> list = tmp.getTermsOfType("Node");
        for (int a = 0; a < list.size(); a++) {
            String nme = list.get(a).getValue("name");
            String sub = list.get(a).getValue("subject");
            String bod = list.get(a).getValue("body");
            //generate binlings
            ArrayList<Binling> cho = new ArrayList<>();
            int v = 0;
            String type = "";
            while ((type = list.get(a).getValue("choice" + v)) != null) {
                //get station info
                String msg = type.split("/")[0];
                String next = type.split("/")[1];
                Binling tv = new Binling(msg, v);
                tv.getStr().add(next);
                cho.add(tv);
                //iterate
                v++;
            }
            //make message
            AstralMessage node = new AstralMessage(owner, sub, bod, cho);
            node.setName(nme);
            //store
            nodes.add(node);
        }
    }

    public void reply(Binling choice) {
        if (choice != null) {
            //advance
            currentNode = findNode(choice.getStr().get(1));
            //handle
            handleBinling(choice);
        } else {
            currentNode = findNode("END");
        }
    }

    private void handleBinling(Binling choice) {
        //does this bindling have additional params?
        if (choice.getStr().size() > 1) {
            //is this a mission offer?
            if (choice.getStr().get(1).matches("MISSION")) {
                //generate a mission
                tmpMission = new Mission(owner);
                //append mission body
                String body = makeMissionDescription(tmpMission);
                currentNode.setMessage(currentNode.getMessage().replace("<#MISSION>", body));
            } else if (choice.getStr().get(1).matches("START_MISSION")) {
                //assign generated mission
                if(tmpMission != null) {
                    owner.getUniverse().getPlayerMissions().add(tmpMission);
                }
            }
        } else {
            //nope
        }
    }
    
    private String makeMissionDescription(Mission mission) {
        String ret = "";
        {
            ret += mission.getBriefing();
            //append reward info
            ret += " /br/ /br/ Cash Reward:     "+mission.getReward();
            ret += " /br/ /br/ Standings Bonus: "+mission.getDeltaStanding();
        }
        return ret;
    }

    public String getName() {
        return name;
    }

    public boolean isDone() {
        if (currentNode.getName().matches("END")) {
            return true;
        } else if (currentNode.getChoices().isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    private AstralMessage findNode(String startNode) {
        for (int a = 0; a < nodes.size(); a++) {
            if (nodes.get(a).getName().matches(startNode)) {
                return nodes.get(a);
            }
        }
        return findNode("END");
    }
}
