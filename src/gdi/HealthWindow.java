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
 * This displays ship health using two progress bars.
 */
package gdi;

import gdi.component.AstralBar;
import gdi.component.AstralWindow;
import java.awt.Color;

/**
 *
 * @author Nathan Wiehoff
 */
public class HealthWindow extends AstralWindow {
    AstralBar shieldBar = new AstralBar();
    AstralBar hullBar = new AstralBar();
    public HealthWindow() {
        super();
        create();
    }
    
    private void create() {
        //color
        backColor = new Color(25, 25, 25, 200);
        //size this window
        width = 300;
        height = 10;
        setVisible(true);
        //create shield bar
        shieldBar.setName("shieldbar");
        shieldBar.setX(0);
        shieldBar.setY(0);
        shieldBar.setWidth(140);
        shieldBar.setHeight(10);
        shieldBar.setVisible(true);
        shieldBar.setBarColor(Color.GREEN);
        //create hull bar
        hullBar.setName("hullbar");
        hullBar.setX(160);
        hullBar.setY(0);
        hullBar.setWidth(140);
        hullBar.setHeight(10);
        hullBar.setVisible(true);
        hullBar.setBarColor(Color.RED);
        //pack
        addComponent(shieldBar);
        addComponent(hullBar);
    }
    
    public void updateHealth(double shieldPercent, double hullPercent) {
        shieldBar.setPercentage(shieldPercent);
        hullBar.setPercentage(hullPercent);
    }
}
