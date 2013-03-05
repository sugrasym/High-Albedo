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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celestial.Ship;

import engine.Entity;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 *
 * @author Nathan Wiehoff
 */
public class Projectile extends Ship {

    protected double maxRange;
    protected double damage;
    protected Ship owner;
    protected double speed;
    private double traveled = 0;

    public Projectile(Ship owner, String name, String type, Image raw_tex, BufferedImage tex, int width, int height) {
        super(name, type);
        this.owner = owner;
        this.width = width;
        this.height = height;
        this.raw_tex = raw_tex;
        this.tex = tex;
    }

    @Override
    protected void initGraphics() {
        //do nothing
    }

    @Override
    public void informOfCollisionWith(Entity target) {
        if (target instanceof Projectile) {
        } else 
            if (target instanceof Ship) {
            Ship tmp = (Ship) target;
            if (tmp != owner) {
                state = State.DEAD;
            }
        } else {
            state = State.DEAD;
        }
    }

    @Override
    public void alive() {
        super.alive();
        //update range
        traveled += speed * tpf;
        if (traveled > maxRange) {
            state = State.DEAD;
        }
    }
    
    @Override
    protected void behaviorTest() {
        
    }

    @Override
    protected void initStats() {
        shield = maxShield = Double.MAX_VALUE;
        hull = maxHull = Double.MAX_VALUE;
    }

    @Override
    protected void drawHealthBars(Graphics g, double dx, double dy) {
    }

    public double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public Ship getOwner() {
        return owner;
    }

    public void setOwner(Ship owner) {
        this.owner = owner;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }
}
