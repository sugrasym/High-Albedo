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
 * Window for displaying the status of a ship's equipment.
 */
package gdi;

import cargo.Hardpoint;
import celestial.Ship.Explosion;
import celestial.Ship.Projectile;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import engine.Entity;
import engine.Entity.State;
import gdi.component.AstralBar;
import gdi.component.AstralComponent;
import gdi.component.AstralLabel;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class EquipmentWindow extends AstralWindow {

    AstralList weaponList = new AstralList(this);
    AstralList targetList = new AstralList(this);
    OverviewCanvas overview = new OverviewCanvas();
    AstralLabel targetName = new AstralLabel();
    AstralLabel targetType = new AstralLabel();
    AstralLabel targetFaction = new AstralLabel();
    AstralLabel targetDistance = new AstralLabel();
    AstralLabel commInfo = new AstralLabel();
    AstralBar targetShield = new AstralBar();
    AstralBar targetHull = new AstralBar();
    Font targetFont = new Font("Monospaced", Font.PLAIN, 9);
    private Ship ship;

    public EquipmentWindow() {
        super();
        generate();
    }

    public void update(Ship ship) {
        this.ship = ship;
        //clear list
        weaponList.clearList();
        targetList.clearList();
        for (int a = 0; a < ship.getHardpoints().size(); a++) {
            weaponList.addToList(ship.getHardpoints().get(a));
        }
        ArrayList<Entity> celestials = ship.getCurrentSystem().getEntities();
        for (int a = 0; a < celestials.size(); a++) {
            if (celestials.get(a) instanceof Ship) {
                if (!(celestials.get(a) instanceof Projectile)) {
                    if (!(celestials.get(a) instanceof Explosion)) {
                        Ship tmp = (Ship) celestials.get(a);
                        if (ship.distanceTo(tmp) < ship.getSensor() && ship != tmp) {
                            targetList.addToList(tmp);
                        }
                    }
                }
            }
        }
        //update targeting components
        Ship tmp = ship.getTarget();
        if (tmp != null) {
            targetName.setText(tmp.getName());
            targetType.setText(tmp.getType());
            targetFaction.setText("[" + ship.getStandingsToMe(tmp) + "] " + tmp.getFaction());
            targetDistance.setText((int) ship.distanceTo(ship.getTarget()) + "");
            if (tmp instanceof Station) {
                Station st = (Station) tmp;
                if (ship.isDocked()) {
                    commInfo.setText("Press D To Launch");
                } else {
                    if (ship.getPort() != null) {
                        commInfo.setText("Request Accepted");
                    } else {
                        if (st.canDock(ship)) {
                            commInfo.setText("Press D For Docking");
                        } else {
                            commInfo.setText("Docking Denied");
                        }
                    }
                }
            } else if (tmp instanceof Ship) {
                commInfo.setText("Press H To Hail");
            } else {
                commInfo.setText("");
            }
            targetShield.setPercentage(ship.getTarget().getShield() / ship.getTarget().getMaxShield());
            targetHull.setPercentage(ship.getTarget().getHull() / ship.getTarget().getMaxHull());
        } else {
            targetName.setText("NO AIM");
            targetType.setText("");
            targetFaction.setText("");
            targetDistance.setText("");
            commInfo.setText("");
            targetShield.setPercentage(0);
            targetHull.setPercentage(0);
        }
    }

    private void generate() {
        backColor = windowGrey;
        //size this window
        width = 300;
        height = 300;
        setVisible(true);
        //setup the list
        weaponList.setX(0);
        weaponList.setY(0);
        weaponList.setWidth(width);
        weaponList.setHeight((height / 4) - 1);
        weaponList.setVisible(true);
        //setup the list
        targetList.setX(0);
        targetList.setY(height / 4);
        targetList.setWidth(width);
        targetList.setHeight((height / 4));
        targetList.setVisible(true);
        //setup the target name label
        targetName.setName("target");
        targetName.setText("TARGET");
        targetName.setX(0);
        targetName.setY((height / 2) + 1);
        targetName.setFont(targetFont);
        targetName.setWidth(width / 2);
        targetName.setHeight(targetFont.getSize() + 1);
        targetName.setVisible(true);
        //setup the target type label
        targetType.setName("type");
        targetType.setText("TYPE");
        targetType.setX(0);
        targetType.setY((height / 2) + 1 + targetFont.getSize());
        targetType.setFont(targetFont);
        targetType.setWidth(width / 2);
        targetType.setHeight(targetFont.getSize() + 1);
        targetType.setVisible(true);
        //setup the target faction label
        targetFaction.setName("faction");
        targetFaction.setText("FACTION");
        targetFaction.setX(0);
        targetFaction.setY((height / 2) + 1 + 2 * targetFont.getSize());
        targetFaction.setFont(targetFont);
        targetFaction.setWidth(width / 2);
        targetFaction.setHeight(targetFont.getSize() + 1);
        targetFaction.setVisible(true);
        //setup the target distance
        targetDistance.setName("distance");
        targetDistance.setText("DISTANCE");
        targetDistance.setX(0);
        targetDistance.setY((height / 2) + 1 + 3 * targetFont.getSize());
        targetDistance.setFont(targetFont);
        targetDistance.setWidth(width / 2);
        targetDistance.setHeight(targetFont.getSize() + 1);
        targetDistance.setVisible(true);
        //setup the target distance
        commInfo.setName("dock");
        commInfo.setText("DOCK");
        commInfo.setX(0);
        commInfo.setY((height / 2) + 1 + 5 * targetFont.getSize());
        commInfo.setFont(targetFont);
        commInfo.setWidth(width / 2);
        commInfo.setHeight((targetFont.getSize() + 1) * 2);
        commInfo.setVisible(true);
        //setup the shield bar
        targetShield.setX(0);
        targetShield.setName("shield");
        targetShield.setY(height - 2 * targetFont.getSize());
        targetShield.setWidth(width / 2);
        targetShield.setHeight(targetFont.getSize() + 1);
        targetShield.setBarColor(Color.GREEN);
        targetShield.setVisible(true);
        //setup the hull
        targetHull.setX(0);
        targetHull.setName("shield");
        targetHull.setY(height - 1 * targetFont.getSize());
        targetHull.setWidth(width / 2);
        targetHull.setHeight(targetFont.getSize() + 1);
        targetHull.setBarColor(Color.RED);
        targetHull.setVisible(true);
        //setup the overview canvas
        overview.setName("overview");
        overview.setX(width / 2);
        overview.setY(height / 2 + 1);
        overview.setWidth(width / 2);
        overview.setHeight(height / 2 - 1);
        overview.setVisible(true);
        //pack
        addComponent(weaponList);
        addComponent(targetList);
        addComponent(targetName);
        addComponent(targetType);
        addComponent(targetFaction);
        addComponent(targetDistance);
        addComponent(commInfo);
        addComponent(targetShield);
        addComponent(targetHull);
        addComponent(overview);
    }

    private class OverviewCanvas extends AstralComponent {

        Font radarFont = new Font("Monospaced", Font.PLAIN, 9);

        public OverviewCanvas() {
            super();
        }

        @Override
        public void render(Graphics f) {
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (ship != null) {
                //get graphics
                Graphics2D gfx = (Graphics2D) frame.getGraphics();
                //draw stuff
                if (ship.getTarget() != null) {
                    fillRadar(gfx);
                }
                //draw circle
                gfx.setColor(Color.BLUE);
                gfx.drawOval(0, 0, width, height);
                //draw border
                gfx.setColor(amber);
                gfx.drawRect(0, 0, width - 1, height - 1);
            }
            f.drawImage(frame, getX(), getY(), width, height, null);
        }

        private void fillRadar(Graphics2D gfx) {
            //get sensor strength
            double range = ship.getSensor();
            //get coordinates
            double ex = ship.getTarget().getX() + (ship.getTarget().getWidth() / 2);
            double ey = ship.getTarget().getY() + (ship.getTarget().getHeight() / 2);
            //adjust for player loc
            ex -= (ship.getX() + ship.getWidth() / 2);
            ey -= (ship.getY() + ship.getHeight() / 2);
            //calculate distance
            double dist = magnitude(ex, ey);
            if (dist <= range && ship.getTarget().getState() == State.ALIVE) {
                //adjust for size
                ex /= range;
                ey /= range;
                ex *= width / 2;
                ey *= height / 2;
                /*
                 * Draw the ship and its vector lines
                 */
                drawShipOnRadar(gfx, ex, ey);
                drawVectorLines(gfx, ex, ey);
            } else {
                ship.setTarget(null);
            }
        }

        protected void drawShipOnRadar(Graphics2D gfx, double ex, double ey) {
            gfx.setColor(Color.WHITE);
            gfx.fillRect((int) ex + (width / 2) - 2, (int) ey + (height / 2) - 2, 4, 4);
            gfx.setFont(radarFont);
        }

        protected void drawVectorLines(Graphics2D gfx, double ex, double ey) {
            /*
             * Shows the vectors of the target ship, useful in an intercept or
             * a fight.
             */
            //draw the range of your craft's selected equipment
            Hardpoint tmp = (Hardpoint) weaponList.getItemAtIndex(weaponList.getIndex());
            if (tmp != null) {
                double range = tmp.getMounted().getRange();
                range /= ship.getSensor();
                gfx.setColor(Color.YELLOW);
                int w = (int) (width * range);
                int h = (int) (height * range);
                gfx.drawOval((width / 2) - w / 2, (height / 2) - h / 2, w, h);
            }
            //line of sight of your craft
            gfx.setColor(Color.CYAN);
            double dTheta = ship.getTheta() - (Math.PI);
            double dpx = Math.cos(dTheta) * width / 2;
            double dpy = Math.sin(dTheta) * height / 2;
            gfx.drawLine(width / 2, (height / 2), (int) dpx + (width / 2), (int) dpy + (height / 2));
            //line between your craft and the target
            gfx.setColor(Color.PINK);
            gfx.drawLine(width / 2, height / 2, (int) ex + (width / 2), (int) ey + (height / 2));
        }
    }

    @Override
    public void handleMouseClickedEvent(MouseEvent me) {
        super.handleMouseClickedEvent(me);
        //get the module and toggle its enabled status
        if (weaponList.isFocused()) {
            Hardpoint tmp = (Hardpoint) weaponList.getItemAtIndex(weaponList.getIndex());
            tmp.setEnabled(!tmp.isEnabled());
        }
        if (targetList.isFocused()) {
            ship.setTarget((Ship) targetList.getItemAtIndex(targetList.getIndex()));
        }
    }

    public void scrollUp() {
        weaponList.scrollUp();
    }

    public void scrollDown() {
        weaponList.scrollDown();
    }

    private double magnitude(double dx, double dy) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }
}
