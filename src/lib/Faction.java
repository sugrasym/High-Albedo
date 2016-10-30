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
import universe.Universe;

/**
 *
 * @author Nathan Wiehoff
 */
public class Faction implements Serializable {

    public static final int PERMA_RED = -10;
    public static final int PERMA_GREEN = 10;
    private final String name;
    private String description = "No Information Found";
    //sov and distribution
    private boolean isEmpire = false;
    private double spread = 0;
    protected ArrayList<String> hosts = new ArrayList<>();
    private final ArrayList<Binling> standings = new ArrayList<>();
    //contraband
    private final ArrayList<String> contraband = new ArrayList<>();
    //comm hints
    private final ArrayList<String> contrabandNotifications = new ArrayList<>();
    private final ArrayList<String> hateNotifications = new ArrayList<>();
    private final ArrayList<String> neutralNotifications = new ArrayList<>();
    private final ArrayList<String> friendlyNotifications = new ArrayList<>();
    private final ArrayList<String> rumorList = new ArrayList<>();
    private final ArrayList<String> campaignList = new ArrayList<>();

    public Faction(String name) {
        this.name = name;
        init();
        initComms();
    }

    private void initComms() {
        Parser tmp = Universe.getCache().getFactionCache();
        ArrayList<Term> comms = tmp.getTermsOfType("Comm");
        for (int a = 0; a < comms.size(); a++) {
            if (comms.get(a).getValue("name").equals(name)) {
                /*
                 * Initialize contraband notifications
                 */
                {
                    int x = 0;
                    String type;
                    while ((type = comms.get(a).getValue("contraband" + x)) != null) {
                        //get station info
                        String ty = type;
                        contrabandNotifications.add(ty);
                        //iterate
                        x++;
                    }
                }
                /*
                 * Initialize bad standing (hate) notifications
                 */
                {
                    int x = 0;
                    String type;
                    while ((type = comms.get(a).getValue("hate" + x)) != null) {
                        //get station info
                        String ty = type;
                        hateNotifications.add(ty);
                        //iterate
                        x++;
                    }
                }
                /*
                 * Initialize neutral notifications
                 */
                {
                    int x = 0;
                    String type;
                    while ((type = comms.get(a).getValue("neut" + x)) != null) {
                        //get station info
                        String ty = type;
                        neutralNotifications.add(ty);
                        //iterate
                        x++;
                    }
                }
                /*
                 * Initialize friendly notifications
                 */
                {
                    int x = 0;
                    String type;
                    while ((type = comms.get(a).getValue("love" + x)) != null) {
                        //get station info
                        String ty = type;
                        getFriendlyNotifications().add(ty);
                        //iterate
                        x++;
                    }
                }
                /*
                 * Initialize rumor list
                 */
                {
                    int x = 0;
                    String type;
                    while ((type = comms.get(a).getValue("rumor" + x)) != null) {
                        //get station info
                        String ty = type;
                        getRumorList().add(ty);
                        //iterate
                        x++;
                    }
                }
                /*
                 * Initialize campaign list
                 */
                {
                    int x = 0;
                    String type;
                    while ((type = comms.get(a).getValue("campaign" + x)) != null) {
                        //get station info
                        String ty = type;
                        getCampaignList().add(ty);
                        //iterate
                        x++;
                    }
                }
                //quit
                break;
            }
        }
    }

    private void init() {
        Parser tmp = Universe.getCache().getFactionCache();
        ArrayList<Term> factions = tmp.getTermsOfType("Faction");
        for (int a = 0; a < factions.size(); a++) {
            if (factions.get(a).getValue("name").equals(name)) {
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
                        getHosts().addAll(Arrays.asList(arr));
                    }
                    //Store description
                    String desc = tmp2.getValue("var_description");
                    if (desc != null) {
                        description = desc;
                    }
                    //Store contraband
                    String cntr = tmp2.getValue("var_contraband");
                    if (cntr != null) {
                        String[] arr = cntr.split("/");
                        getContraband().addAll(Arrays.asList(arr));
                    }
                } catch (Exception e) {
                    System.out.println(name + " is missing information about spread and sov");
                }
                break;
            }
        }
    }

    public boolean isContraband(String item) {
        for (int a = 0; a < contraband.size(); a++) {
            if (contraband.get(a).equals(item)) {
                return true;
            }
        }
        return false;
    }

    public void derivedModification(Faction mod, double delta) {
        /*
         * When you destroy someone's ship you get a standings hit to them,
         * and a derived standings hit to their allies.
         * 
         * You also get a derived bonus to their enemies so long as they
         * are not -10 (perma red) to you.
         * 
         * A faction will always hate you if you are -10 to them and will
         * always like you if you are +10 to them. -9 to 9 is the normal
         * range for dynamic standings.
         * 
         */
        double standing = getStanding(mod.getName());
        //perform standing adjustment
        if (standing > PERMA_RED && standing < PERMA_GREEN) {
            //linear adjustment on the faction directly affected
            double newStanding = standing + delta;
            if (newStanding > PERMA_RED && newStanding < PERMA_GREEN) {
                setStanding(mod.getName(), newStanding);
            } else {
                //we don't want to push the player into a singularity
            }
            //calculate delta prime for each faction
            ArrayList<Binling> list = mod.getStandings();
            for (int a = 0; a < list.size(); a++) {
                String tmpName = list.get(a).getString();
                double tmpStanding = list.get(a).getDouble();
                //make sure they aren't neutral
                if (tmpStanding != 0) {
                    //make sure it's not this faction
                    if (!tmpName.equals(name) && !tmpName.equals(mod.getName())) {
                        //make sure they aren't -10 or +10 to this faction
                        double lS = getStanding(tmpName);
                        if (lS > PERMA_RED && lS < PERMA_GREEN) {
                            //get their relationship as a percentage
                            double per = tmpStanding / 10.0;
                            //multiply the delta by that percentage
                            double deltaPrime = (per) * delta;
                            //it's harder to make friends than lose them
                            if (deltaPrime > 0) {
                                deltaPrime /= 4;
                            }
                            //calculate new standings
                            newStanding = lS + deltaPrime;
                            //modify standings
                            if (newStanding > PERMA_RED && newStanding < PERMA_GREEN) {
                                setStanding(tmpName, lS + deltaPrime);
                            }
                        }
                    }
                } else {
                    //nobody cares
                }
            }
        } else {
            //ignore
        }
    }

    public double getStanding(String faction) {
        if (standings != null) {
            for (int a = 0; a < standings.size(); a++) {
                Binling test = standings.get(a);
                if (test.getString().hashCode() == faction.hashCode()) {
                    return test.getDouble();
                }
            }
        } else {
            return 0;
        }
        return 0;
    }

    public void setStanding(String faction, double value) {
        if (!faction.equals(name)) {
            if (value < PERMA_RED) {
                value = PERMA_RED;
            } else if (value > PERMA_GREEN) {
                value = PERMA_GREEN;
            }
            if (standings != null) {
                for (int a = 0; a < standings.size(); a++) {
                    Binling test = standings.get(a);
                    if (test.getString().equals(faction)) {
                        test.setDouble(value);
                    }
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

    public ArrayList<Binling> getStandings() {
        return standings;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<String> getContraband() {
        return contraband;
    }

    public ArrayList<String> getContrabandNotifications() {
        return contrabandNotifications;
    }

    public ArrayList<String> getHateNotifications() {
        return hateNotifications;
    }

    public ArrayList<String> getNeutralNotifications() {
        return neutralNotifications;
    }

    public ArrayList<String> getFriendlyNotifications() {
        return friendlyNotifications;
    }

    public ArrayList<String> getRumorList() {
        return rumorList;
    }

    public ArrayList<String> getCampaignList() {
        return campaignList;
    }

    public ArrayList<String> getHosts() {
        return hosts;
    }
}
