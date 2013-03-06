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
package celestial;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 *
 * @author nwiehoff
 */
public class Planet extends Celestial {

    private String texture;
    protected int diameter;
    protected transient Image tex;
    private ArrayList<Rectangle> bound = new ArrayList<>();

    public Planet(String name, String texture, int radius) {
        setName(name);
        this.texture = texture;
        this.diameter = radius;
    }

    @Override
    public void init(boolean loadedGame) {
        state = State.ALIVE;
    }

    public void initGraphics() {
        /*
         * Load the image for this planet and scale it
         */
        try {
            tex = io.loadImage("planet/" + texture);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disposeGraphics() {
        tex = null;
    }

    @Override
    public ArrayList<Rectangle> getBounds() {
        return bound;
    }

    @Override
    public void alive() {
        //update bound
        bound.clear();
        bound.add(new Rectangle((int) getX(), (int) getY(), getDiameter(), getDiameter()));
    }

    @Override
    public synchronized void render(Graphics f, double dx, double dy) {
        if (tex != null) {
            Graphics2D s = (Graphics2D) (f);
            s.drawImage(tex, (int) (getX() - dx), (int) (getY() - dy), getDiameter(), getDiameter(), null);
        } else {
            initGraphics();
        }
    }

    public int getDiameter() {
        return diameter;
    }

    public void setRadius(int radius) {
        this.diameter = radius;
    }
}
