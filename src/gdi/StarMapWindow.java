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
 * Displays a map of the universe.
 */
package gdi;

import celestial.Jumphole;
import engine.Entity;
import gdi.component.AstralComponent;
import gdi.component.AstralLabel;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class StarMapWindow extends AstralWindow {

    MapCanvas canvas = new MapCanvas();
    AstralLabel modeLabel = new AstralLabel();
    AstralLabel rangeLabel = new AstralLabel();
    Font radarFont = new Font("Monospaced", Font.BOLD, 11);
    private Universe universe;
    private double zoom = 0.25;
    private final Point2D.Double offset = new Point2D.Double(0, 0);

    public StarMapWindow() {
        super();
        generate();
    }

    private void generate() {
        //set size
        setWidth(500);
        setHeight(400);
        //set color
        backColor = windowBlue;
        //not visible at start
        setVisible(false);
        //setup map canvas
        canvas.setX(0);
        canvas.setY(0);
        canvas.setWidth(width);
        canvas.setHeight(height);
        canvas.setVisible(true);
        //setup mode label
        modeLabel.setText("NO MODE");
        modeLabel.setName("mode");
        modeLabel.setX(0);
        modeLabel.setY(15);
        modeLabel.setWidth(120);
        modeLabel.setHeight(25);
        modeLabel.setVisible(true);
        //setup range label
        rangeLabel.setText("range");
        rangeLabel.setName("range");
        rangeLabel.setX(0);
        rangeLabel.setY(0);
        rangeLabel.setWidth(width);
        rangeLabel.setHeight(25);
        rangeLabel.setVisible(true);
        //pack
        addComponent(canvas);
        addComponent(rangeLabel);
        addComponent(modeLabel);
    }

    public void updateMap(Universe universe) {
        this.universe = universe;
    }

    private class MapCanvas extends AstralComponent {

        @Override
        public void render(Graphics f) {
            modeLabel.setText("ZOOM: " + zoom + "x");
            rangeLabel.setText("SCROLL: (" + offset.x + " , " + offset.getY() + ")");
            BufferedImage frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            BufferedImage top = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            //get graphics
            Graphics2D gfx = (Graphics2D) frame.getGraphics();
            Graphics2D tfx = (Graphics2D) top.getGraphics();
            //draw stuff
            fillMap(gfx, tfx);
            //draw image
            f.drawImage(frame, getX(), getY(), getWidth(), getHeight(), null);
            f.drawImage(top, getX(), getY(), getWidth(), getHeight(), null);
        }

        private void fillMap(Graphics2D gfx, Graphics2D tfx) {
            if (universe != null) {
                //get a list of solar systems the player has visited
                //ArrayList<SolarSystem> systems = universe.getSystems(); //will show everything
                ArrayList<SolarSystem> systems = universe.getDiscoveredSpace(); //will show discovered space
                //iterate through and draw an icon for each one
                for (int a = 0; a < systems.size(); a++) {
                    //only render settled systems
                    if (systems.get(a).isSettled()) {
                        //compute offset
                        int ox = getWidth() / 2;
                        int oy = getHeight() / 2;
                        ox += offset.x;
                        oy += offset.y;
                        //select font
                        tfx.setFont(radarFont);
                        //get position
                        double sx = systems.get(a).getX();
                        double sy = systems.get(a).getY();
                        //zoom
                        sx *= zoom;
                        sy *= zoom;
                        //compute final render position
                        double rx = sx + ox;
                        double ry = sy + oy;
                        {
                            //search system for jump holes
                            ArrayList<Entity> cel = systems.get(a).getJumpholeList();
                            for (int v = 0; v < cel.size(); v++) {
                                if (cel.get(v) instanceof Jumphole) {
                                    Jumphole tmp = (Jumphole) cel.get(v);
                                    try {
                                        //get exit system
                                        SolarSystem exit = tmp.getOutGate().getCurrentSystem();
                                        //only draw connections between settled systems
                                        if (exit.isSettled()) {
                                            //figure out where it is on our map
                                            double tx = (exit.getX() * zoom) + ox;
                                            double ty = (exit.getY() * zoom) + oy;
                                            //draw a line
                                            gfx.setColor(Color.LIGHT_GRAY);
                                            gfx.drawLine((int) rx, (int) ry, (int) tx, (int) ty);
                                        }
                                    } catch (Exception e) {
                                        System.out.println("Forcing " + tmp.getName() + " to link with partner");
                                        tmp.createLink(tmp.getOut());
                                    }
                                }
                            }
                        }
                        //map
                        if (systems.get(a) == universe.getPlayerShip().getCurrentSystem()) {
                            tfx.setColor(whiteForeground);
                            tfx.fillRect((int) rx - 2, (int) ry - 2, 4, 4);
                            tfx.setColor(Color.GREEN);
                        } else {
                            if (systems.get(a).getOwner().equals("Player")) {
                                tfx.setColor(Color.MAGENTA);
                            } else {
                                tfx.setColor(Color.GRAY);
                            }
                            tfx.fillRect((int) rx - 2, (int) ry - 2, 4, 4);
                            tfx.setColor(Color.WHITE);
                        }
                        tfx.drawString(systems.get(a).getName(), (int) rx - 2, (int) ry - 2);
                    }
                }
            }
        }
    }

    public void incrementMode() {
        zoom *= 2;
        offset.x = offset.x * 2;
        offset.y = offset.y * 2;
    }

    public void decrementMode() {
        zoom /= 2;
        /*if (zoom < 0.25) {
         zoom = 0.25;
         }*/
        offset.x = offset.x / 2;
        offset.y = offset.y / 2;
        if (Math.abs(offset.x) < 1) {
            offset.x = 0;
        }
        if (Math.abs(offset.y) < 1) {
            offset.y = 0;
        }
    }

    @Override
    public void handleKeyReleasedEvent(KeyEvent ke) {
        /*
         * navmap keys
         */
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_END:
                incrementMode();
                break;
            case KeyEvent.VK_HOME:
                decrementMode();
                break;
            case KeyEvent.VK_UP:
                offset.y += 20;
                offset.x += 0;
                break;
            default:
                break;
        }
        if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
            offset.y -= 20;
            offset.x += 0;
        }
        if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
            offset.y += 0;
            offset.x += 20;
        }
        if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
            offset.y += 0;
            offset.x -= 20;
        }
    }
}
