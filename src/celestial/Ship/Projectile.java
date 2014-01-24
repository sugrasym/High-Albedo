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

import cargo.Equipment;
import cargo.Weapon;
import engine.Entity;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import lib.FastMath;
import universe.Universe;

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
    private double lastX = 0;
    private double lastY = 0;
    private double range = 0;

    public Projectile(Ship owner, String name, String type, Image raw_tex, BufferedImage tex, int width, int height) {
        super(name, type);
        this.owner = owner;
        this.width = width;
        this.height = height;
        this.raw_tex = raw_tex;
        this.tex = tex;
        this.faction = owner.getFaction();
    }

    @Override
    protected void explode() {
        /*
         * Generates explosion effect
         */
        if (tex != null) {
            Point2D.Double size = new Point2D.Double(width, height);
            int count = rnd.nextInt(3) + 1;
            for (int a = 0; a < count; a++) {
                Explosion exp = new Explosion(size, getExplosion(), 0.5);
                exp.setFaction(faction);
                exp.init(false);
                //calculate helpers
                double dT = rnd.nextInt() % (Math.PI * 2.0);
                double ew = 2 * rnd.nextInt(getWidth() + 1) - getWidth();
                double dx = ew * Math.cos(dT);
                double dy = ew * Math.sin(dT);
                //store position
                exp.setX((getX() + getWidth() / 2) - exp.getWidth() / 2 + dx);
                exp.setY((getY() + getHeight() / 2) - exp.getHeight() / 2 + dy);
                //calculate speed
                double speed = rnd.nextInt(40) + 50;
                double pdx = speed * Math.cos(dT);
                double pdy = speed * Math.sin(dT);
                //add to host vector
                exp.setVx(-getVx() / 8 + pdx);
                exp.setVy(-getVy() / 8 + pdy);
                exp.setCurrentSystem(currentSystem);
                //randomize rotation
                exp.setTheta(rnd.nextDouble() * (2 * Math.PI));
                //deploy
                getCurrentSystem().putEntityInSystem(exp);
            }
        }
    }

    @Override
    protected void fightTarget() {
        /*
         * Modified routine for use by missiles
         */
        if (getTarget() != null) {
            //attack
            if (getTarget().getState() == State.ALIVE) {
                double distance = distanceTo(target);
                double range = -10000000;
                //fire thrusters based on range
                fireRearThrusters();
                double enemyX = getFireLeadX();
                double enemyY = getFireLeadY();
                double desired = 0;
                if (currentSystem != getUniverse().getPlayerShip().getCurrentSystem()) {
                    desired = FastMath.atan2(enemyY, enemyX);
                } else {
                    desired = Math.atan2(enemyY, enemyX);
                }
                desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                //rotate to face the enemy
                if (Math.abs(theta - desired) > turning * tpf) {
                    if (distance > width && distance > height) {
                        if (theta - desired > -0.05) {
                            rotateMinus();
                        } else if (theta - desired < 0.05) {
                            rotatePlus();
                        }
                    }
                } else if (distance <= range) {
                    fireActiveGuns(target);
                }
            } else {
                setTarget(null);
            }
        }
    }

    @Override
    protected Ship avoidCollission() {
        return null;
    }

    @Override
    protected void autopilotAvoidBlock(Ship avoid) {
        //lol
    }

    @Override
    public void initGraphics() {
        try {
            if (getUniverse() != null) {
                //get the image
                raw_tex = Universe.getCache().getProjectileSprite(getType());
                //create the usable version
                ImageIcon icon = new ImageIcon(raw_tex);
                setHeight(icon.getIconHeight());
                setWidth(icon.getIconWidth());
                tex = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int a = 0; a < hardpoints.size(); a++) {
                    Equipment mount = hardpoints.get(a).getMounted();
                    if (mount != null) {
                        if (mount instanceof Weapon) {
                            Weapon tmp = (Weapon) mount;
                            tmp.initGraphics();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void informOfCollisionWith(Entity target) {
        if (target instanceof Projectile) {
        } else if (target instanceof Ship) {
            if (!guided) {
                Ship tmp = (Ship) target;
                if (tmp != owner) {
                    state = State.DYING;
                }
            } else {
                Ship tmp = (Ship) target;
                if (tmp == this.target) {
                    state = State.DYING;
                }
            }
        } else {
            state = State.DYING;
        }
    }

    @Override
    public void alive() {
        super.alive();
        if (guided) {
            if (traveled < 1) {
                fireRearThrusters();
            }
            seek();
        }
        //update range
        if (!guided) {
            traveled += speed * tpf;
            if (traveled > maxRange) {
                state = State.DEAD;
            }
        } else {
            traveled += Math.max(magnitude(getX() - getLastX(), getY() - getLastY()), 1);
            setLastX(getX());
            setLastY(getY());
            if (traveled > getRange() * 8) {
                state = State.DYING;
            }
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

    @Override
    public void fireForwardThrusters() {
        //ramming speed only
    }

    @Override
    public double getNearWeaponRange() {
        /*
         * Returns the range of the closest range onlined weapon.
         */
        return -1000000;
    }

    @Override
    protected double getFireLeadX() {
        //get the center of the enemy
        double enemyX = (getX()) - (target.getX() + target.getWidth() / 2) + (getVx() - target.getVx()) * tpf;
        return enemyX;
    }

    @Override
    protected double getFireLeadY() {
        double enemyY = (getY()) - (target.getY() + target.getHeight() / 2) + (getVy() - target.getVy()) * tpf;
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

    public double getLastX() {
        return lastX;
    }

    public void setLastX(double lastX) {
        this.lastX = lastX;
    }

    public double getLastY() {
        return lastY;
    }

    public void setLastY(double lastY) {
        this.lastY = lastY;
    }

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }
}
