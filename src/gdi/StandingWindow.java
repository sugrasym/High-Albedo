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
 * Allows the standings of the player to be viewed.
 * Nathan Wiehoff
 */
package gdi;

import celestial.Ship.Ship;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import lib.Binling;
import lib.Faction;

public class StandingWindow extends AstralWindow {

    public static final String PLAYER_FACTION = "Player";
    public static final int HOSTILE_STANDING = -2;
    AstralList factionList = new AstralList(this);
    AstralList infoList = new AstralList(this);
    Faction viewing = null;
    protected Ship ship;

    public StandingWindow() {
        super();
        generate();
    }

    private void generate() {
        backColor = windowBlue;
        //size this window
        width = 500;
        height = 400;
        setVisible(true);
        //setup the cargo list
        factionList.setX(0);
        factionList.setY(0);
        factionList.setWidth(width);
        factionList.setHeight((height / 2) - 1);
        factionList.setVisible(true);
        //setup the property list
        infoList.setX(0);
        infoList.setY(height / 2);
        infoList.setWidth((int) (width));
        infoList.setHeight((height / 2) - 1);
        infoList.setVisible(true);
        //pack
        addComponent(factionList);
        addComponent(infoList);
    }

    public void update(Ship ship) {
        setShip(ship);
        factionList.clearList();
        infoList.clearList();
        ArrayList<Binling> logicalFactionList = new ArrayList<>();
        if (ship != null) {
            //add factions
            Faction fac = ship.getMyFaction();
            ArrayList<Binling> standings = fac.getStandings();
            for (int a = 0; a < standings.size(); a++) {
                logicalFactionList.add(standings.get(a));
            }
            //sort by standings
            logicalFactionList = sort(logicalFactionList);
            //add to display
            for (int a = 0; a < logicalFactionList.size(); a++) {
                //don't show the internal neutral and frontier factions
                if (!"Neutral".equals(logicalFactionList.get(a).getString())
                        && !"Frontier".equals(logicalFactionList.get(a).getString())) {
                    factionList.addToList(logicalFactionList.get(a));
                }
            }
            //display detailed information about the selected item
            int index = factionList.getIndex();
            Binling bin = (Binling) factionList.getItemAtIndex(index);
            if (index < logicalFactionList.size()) {
                //fill
                fillFactionLines(viewing, bin);
                fillDescriptionLines(viewing);
            }
        }
    }

    private ArrayList<Binling> sort(ArrayList<Binling> list) {
        ArrayList<Binling> sorted = new ArrayList<>();
        {
            Binling[] arr = (Binling[]) list.toArray(new Binling[0]);
            for (int a = 0; a < arr.length; a++) {
                for (int b = 1; b < arr.length - a; b++) {
                    if (arr[b - 1].getDouble() < arr[b].getDouble()) {
                        Binling tmp = arr[b];
                        arr[b] = arr[b - 1];
                        arr[b - 1] = tmp;
                    }
                }
            }
            sorted.addAll(Arrays.asList(arr));
        }
        return sorted;
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    private void fillFactionLines(Faction selected, Binling simple) {
        if (selected != null) {
            infoList.addToList("--Basic--");
            infoList.addToList(" ");
            infoList.addToList("Name:         " + selected.getName());
            infoList.addToList("Empire:       " + selected.isEmpire());
            if (selected.isEmpire()) {
                infoList.addToList("Extent:       " + (100 * (selected.getSpread())) + "%");
            }
            infoList.addToList(" ");
            infoList.addToList("--Standings--");
            infoList.addToList(" ");
            infoList.addToList("You:          "
                    + ship.getUniverse().getPlayerShip().getStandingsToMe(simple.getString()));
            infoList.addToList(" ");
            infoList.addToList("--Likes--");
            infoList.addToList(" ");
            for (int a = 0; a < selected.getStandings().size(); a++) {
                if (selected.getStandings().get(a).getDouble() > 0) {
                    if (!selected.getStandings().get(a).getString().equals(PLAYER_FACTION)) {
                        infoList.addToList(selected.getStandings().get(a).getString());
                    }
                }
            }
            infoList.addToList(" ");
            infoList.addToList("--Dislikes--");
            infoList.addToList(" ");
            for (int a = 0; a < selected.getStandings().size(); a++) {
                if (selected.getStandings().get(a).getDouble() < 0) {
                    if (!selected.getStandings().get(a).getString().equals(PLAYER_FACTION)) {
                        infoList.addToList(selected.getStandings().get(a).getString());
                    }
                }
            }
            infoList.addToList(" ");
            infoList.addToList("--Will Attack--");
            infoList.addToList(" ");
            for (int a = 0; a < selected.getStandings().size(); a++) {
                if (selected.getStandings().get(a).getDouble() < HOSTILE_STANDING) {
                    if (!selected.getStandings().get(a).getString().equals(PLAYER_FACTION)) {
                        infoList.addToList(selected.getStandings().get(a).getString());
                    }
                }
            }
            if (selected.getContraband().size() > 0) {
                infoList.addToList(" ");
                infoList.addToList("--Contraband--");
                infoList.addToList(" ");
                for (int a = 0; a < selected.getContraband().size(); a++) {
                    infoList.addToList(selected.getContraband().get(a));
                }
            }
        }
    }

    private void fillDescriptionLines(Faction selected) {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        if (selected != null) {
            infoList.addToList(" ");
            infoList.addToList("--Description--");
            infoList.addToList(" ");
            //
            String description = selected.getDescription();
            int lineWidth = (((infoList.getWidth() - 10) / (infoList.getFont().getSize())));
            int cursor = 0;
            String tmp = "";
            String[] words = description.split(" ");
            for (int a = 0; a < words.length; a++) {
                if (a < 0) {
                    a = 0;
                }
                int len = words[a].length();
                if (cursor < lineWidth && !words[a].equals("/br/")) {
                    if (cursor + len <= lineWidth) {
                        tmp += " " + words[a];
                        cursor += len;
                    } else if (lineWidth > len) {
                        infoList.addToList(tmp);
                        tmp = "";
                        cursor = 0;
                        a--;
                    } else {
                        tmp += "[LEN!]";
                    }
                } else {
                    infoList.addToList(tmp);
                    tmp = "";
                    cursor = 0;
                    if (!words[a].equals("/br/")) {
                        a--;
                    }
                }
            }
            infoList.addToList(tmp);
        }
    }

    @Override
    public void handleMouseClickedEvent(MouseEvent me) {
        super.handleMouseClickedEvent(me);
        if (factionList.isFocused()) {
            //get the faction
            int index = factionList.getIndex();
            Binling tmp = (Binling) factionList.getItemAtIndex(index);
            //build the superfaction
            if (tmp.getString().equals(PLAYER_FACTION)) {
                viewing = ship.getUniverse().getPlayerShip().getMyFaction();
            } else {
                viewing = new Faction(tmp.getString());
            }
        }
    }
}
