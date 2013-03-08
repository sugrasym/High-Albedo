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
 * Reusable interface for game entities
 */
package engine;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public interface Entity {
    public enum State {

        ALIVE,
        DYING,
        DEAD
    }
    /*
     * Initialization and updating
     */

    public void init(boolean loadedGame);

    public void periodicUpdate(double tpf);

    public void render(Graphics f, double dx, double dy);
    /*
     * State information
     */

    public State getState();

    public ArrayList<Rectangle> getBounds();

    public boolean collideWith(Entity target);

    public boolean collideWith(Rectangle target);
    
    public void informOfCollisionWith(Entity target);
    /*
     * Position information
     */

    public double getX();

    public double getY();

    public void setX(double x);

    public void setY(double y);
    /*
     * Name....
     */

    public String getName();

    public void setName(String name);
}
