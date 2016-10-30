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
 * Extensible framework for in-game items.
 * Nathan Wiehoff, masternerdguy@yahoo.com
 */
package cargo;

import java.io.Serializable;
import java.util.ArrayList;
import lib.Parser;
import lib.Parser.Term;
import universe.Universe;

public class Item implements Serializable {
    //parameters

    public static final String TYPE_CANNON = "cannon";
    public static final String TYPE_MISSILE = "missile";
    public static final String TYPE_TURRET = "turret";
    public static final String TYPE_BATTERY = "battery";
    public static final String TYPE_COMMODITY = "commodity";
    private int volume;
    private int store;
    private double mass;
    private double HP;
    private String name;
    private String description;
    private String type;
    protected String group;
    protected int quantity = 1;
    //flags
    private boolean alive;
    //economic parameters
    private int minPrice;
    private int maxPrice;

    public Item(String name) {
        this.name = name;
        init();
    }

    private void init() {
        Parser parse = Universe.getCache().getItemCache();
        ArrayList<Term> terms = parse.getTermsOfType("Item");
        Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.equals(getName())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            //extract
            type = relevant.getValue("type");
            volume = Integer.parseInt(relevant.getValue("volume"));
            mass = Double.parseDouble(relevant.getValue("mass"));
            HP = Double.parseDouble(relevant.getValue("HP"));
            minPrice = Integer.parseInt(relevant.getValue("minPrice"));
            maxPrice = Integer.parseInt(relevant.getValue("maxPrice"));
            description = relevant.getValue("description");
            group = relevant.getValue("group");
            String st = relevant.getValue("store");
            if (st != null) {
                store = Integer.parseInt(relevant.getValue("store"));
            } else {
                store = 1000;
            }
        } else {
            System.out.println("The item " + getName() + " does not exist in ITEMS.txt");
        }
    }

    public void periodicUpdate(double tpf) {
    }

    //Utility
    public void damageItem(double damage) {
        HP -= damage;
        if (HP <= 0) {
            alive = false;
            HP = 0;
        }
    }

    //Reusable Parsing
    public String removeLeftSpaces(String s) {
        return s.replaceAll("^\\s+", "");
    }

    //Accesors + Mutators
    public double getVolume() {
        return volume * quantity;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public double getMass() {
        return mass * quantity;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getHP() {
        return HP;
    }

    public void setHP(double HP) {
        this.HP = HP;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return name + "[" + quantity + "]";
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getStore() {
        return store;
    }

    public void setStore(int store) {
        this.store = store;
    }
}
