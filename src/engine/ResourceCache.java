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
 * Contains a list of all sprites (THAT ARE NOT GENERATED ON THE FLY) for
 * easy retrieval.
 */
package engine;

import java.awt.Image;
import java.util.ArrayList;
import lib.AstralIO;
import lib.Parser;
import lib.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class ResourceCache {
    //sprite cache
    private ArrayList<Spriteling> ships = new ArrayList<>();
    private ArrayList<Spriteling> projectiles = new ArrayList<>();
    private ArrayList<Spriteling> explosions = new ArrayList<>();
    //parser cache
    private Parser universeCache = new Parser("UNIVERSE.txt");
    private Parser shipCache = new Parser("SHIPS.txt");
    private Parser itemCache = new Parser("ITEMS.txt");
    private Parser weaponCache = new Parser("WEAPONS.txt");
    private Parser factionCache = new Parser("FACTIONS.txt");
    private Parser stationCache = new Parser("STATIONS.txt");
    private Parser explosionCache = new Parser("EXPLOSIONS.txt");
    private Parser processCache = new Parser("PROCESSES.txt");
    private Parser skyCache = new Parser("SKY.txt");
    private Parser loadoutCache = new Parser("LOADOUTS.txt");
    private Parser conversationCache = new Parser("CONVERSATIONS.txt");
    private Parser planetCache = new Parser("PLANET.txt");
    private Parser missionCache = new Parser("MISSIONS.txt");
    private Parser nameCache = new Parser("NAMES.txt");

    public ResourceCache() {
        try {
            init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception {
        //load groups
        loadShipSprites();
        loadWeaponSprites();
        loadExplosionSprites();
    }

    public Image getShipSprite(String sprite) {
        for (int a = 0; a < ships.size(); a++) {
            if (ships.get(a).matches(sprite)) {
                return ships.get(a).getSprite();
            }
        }
        System.out.println("Warning: "+sprite+" not found in ship cache!");
        return null;
    }

    public Image getProjectileSprite(String sprite) {
        for (int a = 0; a < projectiles.size(); a++) {
            if (projectiles.get(a).matches(sprite)) {
                return projectiles.get(a).getSprite();
            }
        }
        System.out.println("Warning: "+sprite+" not found in projectile cache!");
        return null;
    }

    public Image getExplosionSprite(String sprite) {
        for (int a = 0; a < explosions.size(); a++) {
            if (explosions.get(a).matches(sprite)) {
                return explosions.get(a).getSprite();
            }
        }
        System.out.println("Warning: "+sprite+" not found in explosion cache!");
        return null;
    }

    private void loadShipSprites() throws Exception {
        //get list of ship types
        Parser shipParser = new Parser("SHIPS.txt");
        ArrayList<Term> shipTerms = shipParser.getTermsOfType("Ship");
        //load their sprites
        System.out.println("Loading ship sprites into cache");
        for (int a = 0; a < shipTerms.size(); a++) {
            //get ship type
            String type = shipTerms.get(a).getValue("type");
            //grab asset
            Image raw_tex = AstralIO.loadImage("ship/" + type + ".png");
            //make a new spriteling
            Spriteling tmp = new Spriteling(type, raw_tex);
            //store
            ships.add(tmp);
        }
    }

    private void loadWeaponSprites() throws Exception {
        //get list of weapon types
        Parser projectileParser = new Parser("WEAPONS.txt");
        ArrayList<Term> weaponTerms = projectileParser.getTermsOfType("Weapon");
        //load their sprites
        System.out.println("Loading weapon sprites into cache");
        for (int a = 0; a < weaponTerms.size(); a++) {
            //get weapon
            String type = weaponTerms.get(a).getValue("name");
            //does this weapon use ammo
            String ammo = weaponTerms.get(a).getValue("ammo");
            if (ammo != null) {
                //grab asset
                Image raw_tex = AstralIO.loadImage("projectile/" + ammo + ".png");
                Spriteling tmp = new Spriteling(ammo, raw_tex);
                projectiles.add(tmp);
            } else {
                //grab asset
                Image raw_tex = AstralIO.loadImage("projectile/" + type + ".png");
                //make a new spriteling
                Spriteling tmp = new Spriteling(type, raw_tex);
                //store
                projectiles.add(tmp);
            }
        }
    }

    private void loadExplosionSprites() throws Exception {
        //get list of ship types
        Parser explosionParser = new Parser("EXPLOSIONS.txt");
        ArrayList<Term> explosionTerms = explosionParser.getTermsOfType("Explosion");
        //load their sprites
        System.out.println("Loading explosion sprites into cache");
        for (int a = 0; a < explosionTerms.size(); a++) {
            //get ship type
            String type = explosionTerms.get(a).getValue("type");
            //grab asset
            Image raw_tex = AstralIO.loadImage("explosion/" + type + ".png");
            //make a new spriteling
            Spriteling tmp = new Spriteling(type, raw_tex);
            //store
            explosions.add(tmp);
        }
    }

    public Parser getUniverseCache() {
        return universeCache;
    }

    public Parser getShipCache() {
        return shipCache;
    }

    public Parser getItemCache() {
        return itemCache;
    }

    public Parser getWeaponCache() {
        return weaponCache;
    }

    public Parser getFactionCache() {
        return factionCache;
    }

    public Parser getStationCache() {
        return stationCache;
    }

    public Parser getExplosionCache() {
        return explosionCache;
    }

    public Parser getProcessCache() {
        return processCache;
    }

    public Parser getSkyCache() {
        return skyCache;
    }

    public Parser getLoadoutCache() {
        return loadoutCache;
    }

    public Parser getConversationCache() {
        return conversationCache;
    }

    public Parser getPlanetCache() {
        return planetCache;
    }

    public Parser getMissionCache() {
        return missionCache;
    }

    public Parser getNameCache() {
        return nameCache;
    }

    private class Spriteling {
        /*
         * Simple structure for linking a sprite to its name so it can be
         * easily referenced.
         */

        private String name;
        private Image sprite;
        private int hash;

        public Spriteling(String name, Image sprite) {
            //hash
            this.name = name;
            this.hash = name.hashCode();
            //store image
            this.sprite = sprite;
        }

        public boolean matches(String test) {
            if (test.hashCode() == hash) {
                return true;
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        public Image getSprite() {
            return sprite;
        }

        public int getHash() {
            return hash;
        }
    }
}
