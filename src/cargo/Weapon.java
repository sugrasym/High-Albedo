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
 * Now for some meat. This class represents a turret.
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

    public Weapon(String name) {
        super(name);
        init();
    }

    public void initGraphics() {
        //get the image
        raw_tex = new AstralIO().loadImage("projectile/" + getName() + ".png");
        //create the usable version
        ImageIcon icon = new ImageIcon(raw_tex);
        height = (icon.getIconHeight());
        width = (icon.getIconWidth());
        tex = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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
        } else {
            System.out.println("Hades: The item " + getName() + " does not exist in WEAPONS.txt");
        }
        initGraphics();
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
            double theta = host.getTheta();
            //create projectile
            Projectile pro = new Projectile(host, getName(), getName(), raw_tex, tex, width, height);
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
            //add to universe
            pro.setCurrentSystem(host.getCurrentSystem());
            host.getCurrentSystem().putEntityInSystem(pro);
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
}
