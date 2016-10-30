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
 * Basic class that represents a generic celestial object. It should NEVER EVER
 * be used directly, instead it should be extended.
 */
package celestial;

import engine.Entity;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import lib.AstralIO;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Celestial implements Serializable, Entity {

    //protected variables that need to be accessed by children
    protected double tpf;
    //position
    protected double x;
    protected double y;
    protected int width;
    protected int height;
    //identity
    protected State state;
    protected String name;
    protected static AstralIO io = new AstralIO();
    protected SolarSystem currentSystem;
    //physics
    protected double mass;
    //velocity
    protected double vx = 0;
    protected double vy = 0;
    protected double theta = 0;

    public Celestial() {
    }

    @Override
    public void init(boolean loadedGame) {
        name = System.currentTimeMillis() + "::generic";
    }

    public void initGraphics() {
        //
    }

    public void disposeGraphics() {
        //
    }

    @Override
    public void informOfCollisionWith(Entity target) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean collideWith(Entity target) {
        //do a simple rectangular test before the full test
        if (target instanceof Celestial) {
            Celestial tgt = (Celestial) target;
            Rectangle bnd1 = new Rectangle((int) x, (int) y,
                    (int) Math.max(getWidth(), 50), (int) Math.max(getHeight(), 50));
            Rectangle bnd2 = new Rectangle((int) tgt.getX(), (int) tgt.getY(),
                    (int) Math.max(tgt.getWidth(), 50), (int) Math.max(tgt.getHeight(), 50));

            //they can't collide
            if (!bnd1.intersects(bnd2)) {
                return false;
            }
        }

        //the full bounds check
        try {
            ArrayList<Rectangle> myBox = getBounds();
            ArrayList<Rectangle> targBox = target.getBounds();
            for (int a = 0; a < myBox.size(); a++) {
                for (int b = 0; b < targBox.size(); b++) {
                    if (myBox.get(a).intersects(targBox.get(b))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean collideWith(Rectangle target) {
        try {
            ArrayList<Rectangle> myBox = getBounds();
            if (myBox.size() > 0) {
                for (int a = 0; a < myBox.size(); a++) {
                    if (myBox.get(a).intersects(target)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println(getName() + " has a null box?!");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void periodicUpdate(double tpf) {
        this.tpf = tpf;
        if (null != state) {
            switch (state) {
                case ALIVE:
                    alive();
                    break;
                case DYING:
                    dying();
                    break;
                case DEAD:
                    dead(); //and why is this being updated?
                    break;
                default:
                    throw new UnsupportedOperationException(getName() + " is in an undefined state.");
            }
        }
    }

    public double distanceTo(Celestial celestial) {
        double cx = celestial.getCenterX();
        double cy = celestial.getCenterY();
        double off = Math.max(width / 2, height / 2) + Math.max(celestial.getWidth() / 2, celestial.getHeight() / 2);
        return Math.max(Math.sqrt((cx - getCenterX()) * (cx - getCenterX()) + (cy - getCenterY()) * (cy - getCenterY())) - off, 0);
    }

    /*
     * For compartmentalizing behaviors. This is a cleaner solution than
     * overriding the periodicUpdate method itself.
     */
    protected void alive() {
    }

    public void dying() {
    }

    public void dead() {
    }

    @Override
    public void render(Graphics f, double dx, double dy) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public ArrayList<Rectangle> getBounds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    public double getCenterX() {
        return getX() + getWidth() / 2;
    }

    public double getCenterY() {
        return getY() + getHeight() / 2;
    }

    @Override
    public void setX(double x) {
        this.x = x;
    }

    @Override
    public void setY(double y) {
        this.y = y;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public SolarSystem getCurrentSystem() {
        return currentSystem;
    }

    public void setCurrentSystem(SolarSystem currentSystem) {
        this.currentSystem = currentSystem;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public double getVx() {
        return vx;
    }

    public void setVx(double vx) {
        this.vx = vx;
    }

    public double getVy() {
        return vy;
    }

    public void setVy(double vy) {
        this.vy = vy;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Universe getUniverse() {
        if (currentSystem != null) {
            return currentSystem.getUniverse();
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean quickCollideWith(Rectangle target) {
        Rectangle bnd = new Rectangle((int) x, (int) y, (int) width, (int) height);
        return target.intersects(bnd);
    }
}
