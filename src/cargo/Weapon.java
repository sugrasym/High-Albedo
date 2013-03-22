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
 * Now for some meat. This class represents a cannon.
 */
package cargo;

import celestial.Ship.Projectile;
import engine.Entity;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import lib.AstralIO;
import lib.Parser;

/**
 *
 * @author Nathan Wiehoff
 */
public class Weapon extends Equipment {
    //for storing the texture so it doesn't have to be reloaded every time

    transient Image raw_tex;
    transient BufferedImage tex;
    int width;
    int height;
    //weapon properties
    protected double damage;
    protected double speed;
    protected boolean guided;
    protected Item ammoType;
    //guided weapons
    double accel;
    double turning;

    public Weapon(String name) {
        super(name);
        init();
    }

    public void initGraphics() {
        try {
            if (host.getUniverse() != null) {
                if (ammoType == null) {
                    //the projectile name is the image
                    //get the image
                    raw_tex = host.getUniverse().getCache().getProjectileSprite(getName());
                } else {
                    //the ammo type is the image
                    raw_tex = host.getUniverse().getCache().getProjectileSprite(ammoType.getName());
                }
                //create the usable version
                ImageIcon icon = new ImageIcon(raw_tex);
                height = (icon.getIconHeight());
                width = (icon.getIconWidth());
                tex = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disposeGraphics() {
        raw_tex = null;
        tex = null;
    }

    private void init() {
        //get weapon stuff now
        Parser parse = new Parser("WEAPONS.txt");
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Weapon");
        Parser.Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.matches(getName())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            setName(relevant.getValue("name"));
            setType(relevant.getValue("type"));
            setMass(Double.parseDouble(relevant.getValue("mass")));
            setDamage(Double.parseDouble(relevant.getValue("damage")));
            setRange(Double.parseDouble(relevant.getValue("range")));
            setSpeed(Double.parseDouble(relevant.getValue("speed")));
            setCoolDown(Double.parseDouble(relevant.getValue("refire")));
            //guided?
            {
                String test = relevant.getValue("guided");
                if (test != null) {
                    guided = Boolean.parseBoolean(test);
                }
                if (guided) {
                    //retrieve all the stats
                    accel = Double.parseDouble(relevant.getValue("accel"));
                    turning = Double.parseDouble(relevant.getValue("turning"));
                }
            }
            //ammo?
            {
                String test = relevant.getValue("ammo");
                if (test != null) {
                    //get the item
                    ammoType = new Item(test);
                }
            }
        } else {
            System.out.println("The item " + getName() + " does not exist in WEAPONS.txt");
        }
    }

    @Override
    public void activate(Entity target) {
        if (getCoolDown() <= getActivationTimer() && enabled) {
            setActivationTimer(0); //restart cooldown
            fire();
        }
    }

    private void fire() {
        if (enabled) {
            if (hasAmmo()) {
                double theta = host.getTheta();
                //use any ammo
                useAmmo();
                //create projectile
                String tName = "";
                if(ammoType != null) {
                    tName = ammoType.getName();
                } else {
                    tName = getName();
                }
                Projectile pro = new Projectile(host, tName, tName, raw_tex, tex, width, height);
                pro.init(false);
                //calculate relative position from hardpoint
                double hT = getSocket().getT();
                double hR = getSocket().getR();
                double dx = Math.cos(hT + (theta) - Math.PI) * hR;
                double dy = Math.sin(hT + (theta - Math.PI)) * hR;
                //store position
                pro.setX((host.getX() + host.getWidth() / 2) - pro.getWidth() / 2 + dx);
                pro.setY((host.getY() + host.getHeight() / 2) - pro.getHeight() / 2 + dy);
                //calculate speed
                double pdx = speed * Math.cos(theta - Math.PI);
                double pdy = speed * Math.sin(theta - Math.PI);
                //add to host vector
                pro.setVx(host.getVx() + pdx);
                pro.setVy(host.getVy() + pdy);
                //store angle
                pro.setTheta(host.getTheta());
                //store physics
                pro.setDamage(damage);
                pro.setMaxRange(getRange());
                pro.setMass(getMass());
                pro.setSpeed(speed);
                //store AI
                pro.setGuided(guided);
                if (guided) {
                    pro.setFuel(Double.MAX_VALUE);
                    pro.setMaxFuel(Double.MAX_VALUE);
                    //store stats
                    pro.setSensor(range);
                    pro.setAccel(accel);
                    pro.setMaxRange(getRange() * 1.5);
                    pro.setTurning(turning);
                }
                //add to universe
                pro.setCurrentSystem(host.getCurrentSystem());
                host.getCurrentSystem().putEntityInSystem(pro);
            }
        }
    }

    public void useAmmo() {
        if (ammoType != null) {
            ArrayList<Item> cargo = host.getCargoBay();
            for (int a = 0; a < cargo.size(); a++) {
                Item tmp = cargo.get(a);
                if (tmp.getName().matches(ammoType.getName())) {
                    if (tmp.getGroup().matches(ammoType.getGroup())) {
                        if (tmp.getType().matches(ammoType.getType())) {
                            if (tmp.getQuantity() > 1) {
                                tmp.setQuantity(tmp.getQuantity() - 1);
                            } else {
                                cargo.remove(tmp);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean hasAmmo() {
        if (ammoType != null) {
            if (host.getNumInCargoBay(ammoType) > 0) {
                //and return true;
                return true;
            }
            return false;
        } else {
            return true;
        }
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String toString() {
        String ret = "";
        if (ammoType == null) {
            ret = super.toString();
        } else {
            ret = super.toString();
            ret += " <" + host.getNumInCargoBay(ammoType) + ">";
        }
        return ret;
    }
}
