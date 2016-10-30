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
 * It's kind of big
 */
package universe;

import celestial.Ship.Ship;
import engine.Entity;
import engine.God;
import engine.ResourceCache;
import engine.SettingsManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lib.Parser;
import lib.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class Universe implements Serializable {

    public static final int MAX_FRONTIER_SYSTEMS = 200;
    public static final int MIN_FRONTIER_SYSTEMS = 80;

    private ArrayList<SolarSystem> systems = new ArrayList<>();
    private SettingsManager settings;
    private static final transient ResourceCache CACHE;
    private transient God god;
    //player globals
    protected Ship playerShip;
    private final ArrayList<Entity> playerProperty = new ArrayList<>();
    private final ArrayList<Mission> playerMissions = new ArrayList<>();
    private final ArrayList<Campaign> playerCampaigns = new ArrayList<>();
    private final ArrayList<Campaign> completedCampaigns = new ArrayList<>();
    private final ArrayList<SolarSystem> discoveredSpace = new ArrayList<>();

    static {
        CACHE = new ResourceCache();
    }

    public Universe() {
        init();
    }

    private void init() {
        settings = new SettingsManager();
        //create the universe parser
        Parser parse = Universe.getCache().getUniverseCache();
        //get all the solar system terms
        ArrayList<Term> solars = parse.getTermsOfType("System");
        //generate the systems and add them
        System.out.println("Found " + solars.size() + " systems to make.");
        for (int a = 0; a < solars.size(); a++) {
            getSystems().add(makeSystem(parse, solars.get(a)));
        }
        //generate the player
        ArrayList<Term> games = parse.getTermsOfType("NewGame");
        System.out.println("Found " + games.size() + " games to read.");
        //there should only be of these, pick the first one
        makePlayer(games.get(0));
    }

    private SolarSystem makeSystem(Parser parse, Term thisSystem) {
        SolarSystem system;
        {
            String name = thisSystem.getValue("name");
            String owner = thisSystem.getValue("owner");
            //get position
            double sx = Double.parseDouble(thisSystem.getValue("x"));
            double sy = Double.parseDouble(thisSystem.getValue("y"));
            //get music
            String ambient = thisSystem.getValue("ambient");
            String danger = thisSystem.getValue("danger");
            //get list of backs
            String back = "base_plate.png";
            String target = thisSystem.getValue("sky");
            ArrayList<Term> backs = Universe.getCache().getSkyCache().getTermsOfType("Skybox");
            for (int a = 0; a < backs.size(); a++) {
                if (backs.get(a).getValue("name").equals(target)) {
                    back = backs.get(a).getValue("asset");
                    break;
                }
            }
            system = new SolarSystem(this, name, parse);
            system.setX(sx);
            system.setY(sy);
            system.setBack(back);
            system.setOwner(owner);
            if (ambient != null) {
                system.setAmbientMusic(ambient);
            }
            if (danger != null) {
                system.setDangerMusic(danger);
            }
            system.init(false);
        }
        System.out.println(system.getName() + " solar system created. ");
        return system;
    }

    private void makePlayer(Term start) {
        Ship player;
        {
            //get params
            String ship = start.getValue("ship");
            String system = start.getValue("system");
            String near = start.getValue("near");
            String loadout = start.getValue("install");
            String faction = start.getValue("faction");
            String cargo = start.getValue("cargo");
            String cash = start.getValue("cash");
            //create player
            player = new Ship("Player", ship);
            player.setEquip(loadout);
            player.setFaction(faction);
            player.init(false);
            player.addInitialCargo(cargo);
            player.setCash(Long.parseLong(cash));
            //put it in the right system next to the start object
            for (int a = 0; a < systems.size(); a++) {
                if (systems.get(a).getName().equals(system)) {
                    systems.get(a).putEntityInSystem(player);
                    if (near != null) {
                        for (int b = 0; b < systems.get(a).getEntities().size(); b++) {
                            if (systems.get(a).getEntities().get(b).getName().equals(near)) {
                                player.setX(systems.get(a).getEntities().get(b).getX());
                                player.setY(systems.get(a).getEntities().get(b).getY());
                                break;
                            }
                        }
                    } else {
                        double px = Double.parseDouble(start.getValue("x"));
                        double py = Double.parseDouble(start.getValue("y"));
                        player.setX(px);
                        player.setY(py);
                    }
                    player.setCurrentSystem(systems.get(a));
                    break;
                }
            }
            //store reference to player for quick access
            playerShip = player;
        }
    }

    public ArrayList<SolarSystem> getSystems() {
        return systems;
    }

    public void setSystems(ArrayList<SolarSystem> systems) {
        this.systems = systems;
    }

    public Ship getPlayerShip() {
        return playerShip;
    }

    public void setPlayerShip(Ship playerShip) {
        this.playerShip = playerShip;
    }

    public static ResourceCache getCache() {
        return CACHE;
    }

    public God getGod() {
        if (god != null) {
            return god;
        } else {
            god = new God(this);
            return god;
        }
    }

    public void setGod(God god) {
        this.god = god;
    }

    public ArrayList<Entity> getPlayerProperty() {
        return playerProperty;
    }

    public ArrayList<Mission> getPlayerMissions() {
        return playerMissions;
    }

    public SettingsManager getSettings() {
        return settings;
    }

    public boolean isReady() {
        return !systems.isEmpty();
    }

    public ArrayList<SolarSystem> getDiscoveredSpace() {
        return discoveredSpace;
    }

    public ArrayList<Campaign> getPlayerCampaigns() {
        return playerCampaigns;
    }

    public ArrayList<Campaign> getCompletedCampaigns() {
        return completedCampaigns;
    }

    public ArrayList<SolarSystem> getSettledSpace() {
        return (ArrayList<SolarSystem>) getSystems().stream()
                .filter((system) -> system.getSystemType() == SolarSystem.Type.SETTLED)
                .collect(Collectors.toList());
    }

    public ArrayList<SolarSystem> getFrontierSpace() {
        return (ArrayList<SolarSystem>) getSystems().stream()
                .filter((system) -> system.getSystemType() == SolarSystem.Type.FRONTIER)
                .collect(Collectors.toList());
    }
}
