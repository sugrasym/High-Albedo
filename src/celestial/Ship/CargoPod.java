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
 * This cargo pod will float around in space and contain something that may or may
 * not be worth collecting.
 */
package celestial.Ship;

import cargo.Item;
import engine.Entity;
import java.util.Random;

/**
 *
 * @author nwiehoff
 */
public class CargoPod extends Ship {

    protected Item ware = new Item("NOTHING");
    int maxLife = 720;
    double lifeLimit = new Random().nextInt(maxLife);
    double elapsed = 0;

    public CargoPod(Item ware) {
        super(ware.getName() + " [" + ware.getQuantity() + "]", "Cargo Pod");
        this.ware = ware;
    }

    @Override
    public void installLoadout() {
    }

    @Override
    public void alive() {
        super.alive();
        elapsed += tpf;
        if (elapsed >= lifeLimit) {
            state = State.DYING;
        }
    }

    @Override
    public void init(boolean loadedGame) {
        super.init(loadedGame);
    }

    @Override
    public void informOfCollisionWith(Entity target) {
        if (target instanceof CargoPod) {
            //do nothing
        } else {
            super.informOfCollisionWith(target);
        }
    }

    @Override
    public String toString() {
        String ret = "";
        {
            ret = (int) (100 - (elapsed / lifeLimit) * 100.0) + "% (" + type + ") - " + name + ", " + faction;
        }
        return ret;
    }

    public Item getWare() {
        return ware;
    }

    public void setWare(Item ware) {
        this.ware = ware;
    }
}
