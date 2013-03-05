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
 * This window displays the speed and direction of the ship in both a
 * magnitude and vector component method.
 */
package gdi;

import gdi.component.AstralLabel;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.text.DecimalFormat;

/**
 *
 * @author Nathan Wiehoff
 */
public class VelocityWindow extends AstralWindow {

    AstralLabel vxLabel = new AstralLabel();
    AstralLabel vyLabel = new AstralLabel();
    AstralLabel maLabel = new AstralLabel();

    public VelocityWindow() {
        super();
        generateVectorInfoWindow();
    }

    private void generateVectorInfoWindow() {
        //color
        backColor = new Color(25, 25, 25, 200);
        //size this window
        width = 300;
        height = 20;
        setVisible(true);
        //add x label
        vxLabel.setText("vx");
        vxLabel.setName("vx");
        vxLabel.setX(0);
        vxLabel.setY(0);
        vxLabel.setWidth(100);
        vxLabel.setHeight(25);
        vxLabel.setVisible(true);
        //add y label
        vyLabel.setText("vy");
        vyLabel.setName("vy");
        vyLabel.setX(100);
        vyLabel.setY(0);
        vyLabel.setWidth(100);
        vyLabel.setHeight(25);
        vyLabel.setVisible(true);
        //add ma label
        maLabel.setText("ma");
        maLabel.setName("ma");
        maLabel.setX(200);
        maLabel.setY(0);
        maLabel.setWidth(100);
        maLabel.setHeight(25);
        maLabel.setVisible(true);
        //pack
        addComponent(vxLabel);
        addComponent(vyLabel);
        addComponent(maLabel);
    }

    public synchronized void updateVelocity(double vx, double vy) {
        vxLabel.setText("DX : " + roundTwoDecimal(vx));
        vyLabel.setText("DY : " + roundTwoDecimal(vy));
        maLabel.setText("|V|: " + roundTwoDecimal(magnitude(vx, vy)));
    }

    private synchronized double magnitude(double dx, double dy) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    private double roundTwoDecimal(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.parseDouble(twoDForm.format(d));
    }
}
