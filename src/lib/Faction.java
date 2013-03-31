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
 * Maps a set of likes and dislikes to a celestial. Useful for starting fights
 * and restricting docking.
 * 
 * It is accepted that
 *  - Standings are symmetrical.
 * Therefore it is ok to ask the enemy how much they like you, because you
 * WILL get a brutally honest answer.
 */
package lib;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import lib.Parser.Param;
import lib.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class Faction implements Serializable {
    
    private String name;
    private boolean isEmpire = false;
    private double spread = 0;
    protected ArrayList<String> hosts = new ArrayList<>();
    private ArrayList<Binling> standings = new ArrayList<>();
    
    public Faction(String name) {
        this.name = name;
        init();
    }
    
    private void init() {
        Parser tmp = new Parser("FACTIONS.txt");
        ArrayList<Term> factions = tmp.getTermsOfType("Faction");
        for (int a = 0; a < factions.size(); a++) {
            if (factions.get(a).getValue("name").matches(name)) {
                Term tmp2 = factions.get(a);
                {
                    ArrayList<Param> vals = tmp2.getParams();
                    for (int q = 0; q < vals.size(); q++) {
                        if (!vals.get(q).getName().contains(("var_"))) {
                            try {
                                String fac = vals.get(q).getName();
                                int rel = Integer.parseInt(vals.get(q).getValue());
                                standings.add(new Binling(fac, rel));
                            } catch (Exception e) {
                                //
                            }
                        }
                    }
                }
                try {
                    isEmpire = Boolean.parseBoolean(tmp2.getValue("var_isEmpire"));
                    spread = Double.parseDouble((tmp2.getValue("var_worldPercent")));
                    //store hosts
                    String ho = tmp2.getValue("var_hosts");
                    if (ho != null) {
                        String[] arr = ho.split("/");
                        hosts.addAll(Arrays.asList(arr));
                    }
                } catch (Exception e) {
                    System.out.println(name + " is missing information about spread and sov");
                }
                break;
            }
        }
    }
    
    public int getStanding(String faction) {
        if (standings != null) {
            for (int a = 0; a < standings.size(); a++) {
                Binling test = standings.get(a);
                if (test.getString().hashCode() == faction.hashCode()) {
                    return (int) test.getDouble();
                }
            }
        } else {
            return 0;
        }
        return 0;
    }
    
    public void setStanding(String faction, int value) {
        if (value < -10) {
            value = -10;
        } else if (value > 10) {
            value = 10;
        }
        if (standings != null) {
            for (int a = 0; a < standings.size(); a++) {
                Binling test = standings.get(a);
                if (test.getString().matches(faction)) {
                    test.setDouble(value);
                }
            }
        }
    }
    
    public boolean isEmpire() {
        return isEmpire;
    }
    
    public String getName() {
        return name;
    }
    
    public double getSpread() {
        return spread;
    }
}
