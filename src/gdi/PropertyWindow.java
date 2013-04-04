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
 * Allows the management of a player's property.
 * Nathan Wiehoff
 */
package gdi;

import cargo.Equipment;
import cargo.Hardpoint;
import cargo.Item;
import cargo.Weapon;
import celestial.Ship.Ship;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class PropertyWindow extends AstralWindow {

    public static final String CMD_SWITCH = "Switch Ship";
    AstralList propertyList = new AstralList(this);
    AstralList infoList = new AstralList(this);
    AstralList optionList = new AstralList(this);
    protected Ship ship;

    public PropertyWindow() {
        super();
        generate();
    }

    private void generate() {
        backColor = windowGrey;
        //size this window
        width = 500;
        height = 400;
        setVisible(true);
        //setup the cargo list
        propertyList.setX(0);
        propertyList.setY(0);
        propertyList.setWidth(width);
        propertyList.setHeight((height / 2) - 1);
        propertyList.setVisible(true);
        //setup the property list
        infoList.setX(0);
        infoList.setY(height / 2);
        infoList.setWidth((int) (width / 1.5));
        infoList.setHeight((height / 2) - 1);
        infoList.setVisible(true);
        //setup the command list
        optionList.setX((int) (width / 1.5) + 1);
        optionList.setY(height / 2);
        optionList.setWidth((int) (width / 3));
        optionList.setHeight((height / 2) - 1);
        optionList.setVisible(true);
        //pack
        addComponent(propertyList);
        addComponent(infoList);
        addComponent(optionList);
    }

    public void update(Ship ship) {
        setShip(ship);
        propertyList.clearList();
        infoList.clearList();
        optionList.clearList();
        ArrayList<Ship> logicalPropertyList = new ArrayList<>();
        if (ship != null) {
            //get global list
            ArrayList<Ship> prop = ship.getUniverse().getPlayerProperty();
            for (int a = 0; a < prop.size(); a++) {
                logicalPropertyList.add(prop.get(a));
            }
            //push list to window
            for(int a = 0; a < prop.size(); a++) {
                propertyList.addToList(prop.get(a));
            }
            //display detailed information about the selected item
            int index = propertyList.getIndex();
            if (index < logicalPropertyList.size()) {
                Ship selected = (Ship) propertyList.getItemAtIndex(index);
                infoList.addToList("--GLOBAL--");
                infoList.addToList(" ");
                fillDescriptionLines(selected);
                fillCommandLines(selected);
            }
        }
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    private void fillDescriptionLines(Ship selected) {
        /*
         * Fills in the item's description being aware of things like line breaking on spaces.
         */
        if (selected != null) {
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
                if (cursor < lineWidth && !words[a].matches("/br/")) {
                    if (cursor + len <= lineWidth) {
                        tmp += " " + words[a];
                        cursor += len;
                    } else {
                        if (lineWidth > len) {
                            infoList.addToList(tmp);
                            tmp = "";
                            cursor = 0;
                            a--;
                        } else {
                            tmp += "[LEN!]";
                        }
                    }
                } else {
                    infoList.addToList(tmp);
                    tmp = "";
                    cursor = 0;
                    if (!words[a].matches("/br/")) {
                        a--;
                    }
                }
            }
            infoList.addToList(tmp.toString());
        }
    }

    private void fillCommandLines(Ship selected) {
        if (selected != null) {
        }
    }

    @Override
    public void handleMouseClickedEvent(MouseEvent me) {
        super.handleMouseClickedEvent(me);
        //get the module and toggle its enabled status
        if (optionList.isFocused()) {
            String command = (String) optionList.getItemAtIndex(optionList.getIndex());
            parseCommand(command);
        }
    }

    private void parseCommand(String command) {
        if (command != null) {
            if (command.matches(CMD_SWITCH)) {
                /*
                 * Switch to another ship.
                 */
                Ship selected = (Ship) propertyList.getItemAtIndex(propertyList.getIndex());
                ship.getUniverse().setPlayerShip(selected);
            }
        }
    }
}
