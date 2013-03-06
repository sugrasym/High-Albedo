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
    //basic weapon info

    protected double maxRange;
    protected double damage;
    protected Ship owner;
    protected double speed;
    private double traveled = 0;
    //info for guided weapons
    protected boolean guided = false;

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
        } else if (target instanceof Ship) {
            if (!guided) {
                Ship tmp = (Ship) target;
                if (tmp != owner) {
                    state = State.DEAD;
                }
            } else {
                Ship tmp = (Ship) target;
                if (tmp == this.target) {
                    state = State.DEAD;
                }
            }
        } else {
            state = State.DEAD;
        }
    }

    @Override
    public void alive() {
        super.alive();
        if (guided) {
            seek();
        }
        //update range
        if (!guided) {
            traveled += speed * tpf;
        } else {
            traveled += accel * tpf;
        }
        if (traveled > maxRange) {
            state = State.DEAD;
        }
    }

    protected void seek() {
        behavior = Behavior.NONE;
        autopilot = Autopilot.NONE;
        /*
         * Go after the owner's current target.
         */
        target = owner.getTarget();
        fightTarget();
    }

    public double getNearWeaponRange() {
        /*
         * Returns the range of the closest range onlined weapon.
         */
        return -1;
    }
    
    @Override
    protected double getFireLeadX() {
        //get the center of the enemy
        double enemyX = (getX()) - (target.getX());
        return enemyX;
    }

    @Override
    protected double getFireLeadY() {
        double enemyY = (getY()) - (target.getY());
        return enemyY;
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

    public boolean isGuided() {
        return guided;
    }

    public void setGuided(boolean guided) {
        this.guided = guided;
    }
}
