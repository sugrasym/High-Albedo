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

package universe;

import celestial.Ship.Ship;
import engine.Entity;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author nwiehoff
 */
public class Player implements Serializable {
    
    //player globals
    private Ship playerShip;
    private ArrayList<Entity> playerProperty = new ArrayList<>();
    private ArrayList<Mission> playerMissions = new ArrayList<>();
    private ArrayList<Campaign> playerCampaigns = new ArrayList<>();
    private ArrayList<Campaign> completedCampaigns = new ArrayList<>();
    private ArrayList<SolarSystem> discoveredSpace = new ArrayList<>();

    public Player() {
    }

    public Ship getPlayerShip() {
        return playerShip;
    }

    public void setPlayerShip(Ship playerShip) {
        this.playerShip = playerShip;
    }

    public ArrayList<Entity> getPlayerProperty() {
        return playerProperty;
    }

    public void setPlayerProperty(ArrayList<Entity> playerProperty) {
        this.playerProperty = playerProperty;
    }

    public ArrayList<Mission> getPlayerMissions() {
        return playerMissions;
    }

    public void setPlayerMissions(ArrayList<Mission> playerMissions) {
        this.playerMissions = playerMissions;
    }

    public ArrayList<Campaign> getPlayerCampaigns() {
        return playerCampaigns;
    }

    public void setPlayerCampaigns(ArrayList<Campaign> playerCampaigns) {
        this.playerCampaigns = playerCampaigns;
    }

    public ArrayList<Campaign> getCompletedCampaigns() {
        return completedCampaigns;
    }

    public void setCompletedCampaigns(ArrayList<Campaign> completedCampaigns) {
        this.completedCampaigns = completedCampaigns;
    }

    public ArrayList<SolarSystem> getDiscoveredSpace() {
        return discoveredSpace;
    }

    public void setDiscoveredSpace(ArrayList<SolarSystem> discoveredSpace) {
        this.discoveredSpace = discoveredSpace;
    }   
}
