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
package gdi;

import celestial.Jumphole;
import celestial.Planet;
import celestial.Ship.Explosion;
import celestial.Ship.Projectile;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import engine.Entity;
import gdi.component.AstralComponent;
import gdi.component.AstralLabel;
import gdi.component.AstralWindow;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;
import lib.FastMath;

/**
 *
 * @author Nathan Wiehoff
 */
public class OverviewWindow extends AstralWindow {

    AstralLabel rangeLabel = new AstralLabel();
    AstralLabel modeLabel = new AstralLabel();
    AstralLabel stationLabel = new AstralLabel();
    AstralLabel shipLabel = new AstralLabel();
    AstralLabel velLabel = new AstralLabel();
    OverviewCanvas radar = new OverviewCanvas();
    private Ship sensorShip;
    private double area = 1;
    protected boolean showShipNames = true;
    protected boolean showStationNames = true;
    protected Color planetGrey = new Color(15, 15, 15, 200);
    Random rnd = new Random();

    public OverviewWindow() {
        super();
        generate();
    }

    public void updateOverview(Ship sensorShip) {
        this.sensorShip = sensorShip;
        shipLabel.setVisible(showShipNames);
        stationLabel.setVisible(showStationNames);
        velLabel.setText("REL SPEED: " + roundTwoDecimal(magnitude(sensorShip.getVx(), sensorShip.getVy())));
    }

    public void incrementMode() {
        area *= 2.0;
        if (area > 1024) {
            area = 1024;
        }
    }

    public void decrementMode() {
        area /= 2.0;
        if (area < 0.25) {
            area = 0.25;
        }
    }

    private class OverviewCanvas extends AstralComponent {

        public static final int PLANET_AIM_LIMIT = 32;
        public static final int SHIP_AIM_LIMIT = 2;
        public static final int STATION_AIM_LIMIT = 10;
        Font radarFont = new Font("Monospaced", Font.PLAIN, 9);

        public OverviewCanvas() {
            super();
        }

        @Override
        public void render(Graphics f) {
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            if (sensorShip != null) {
                modeLabel.setText("VIEW: " + area + "x");
                rangeLabel.setText("RANGE: " + sensorShip.getSensor() * area);
                //get graphics
                Graphics2D gfx = (Graphics2D) frame.getGraphics();
                //draw stuff
                fillRadar(gfx);
                //draw circle
                gfx.setColor(Color.BLUE);
                gfx.drawOval(0, 0, width, height);
            }
            f.drawImage(frame, getX(), getY(), width, height, null);
        }

        private void fillRadar(Graphics2D gfx) {
            //get entity list
            ArrayList<Entity> entities = sensorShip.getCurrentSystem().getEntities();
            drawVectorLines(gfx);
            for (int a = 0; a < entities.size(); a++) {
                double range = (sensorShip.getSensor() * area);
                //get coordinates
                double ex = entities.get(a).getX();
                double ey = entities.get(a).getY();
                //adjust for player loc
                ex -= sensorShip.getX();
                ey -= sensorShip.getY();
                //calculate distance
                double dist = magnitude(ex, ey);
                if (dist <= range || entities.get(a) instanceof Planet) {
                    //adjust for size
                    ex /= range;
                    ey /= range;
                    ex *= width / 2;
                    ey *= height / 2;
                    /*
                     * Does the final drawing based on what exactly the object is
                     */
                    if (entities.get(a) instanceof Jumphole) {
                        doJumphole(entities, a, range, gfx);
                    } else if (entities.get(a) instanceof Planet) {
                        doPlanet(entities, a, range, gfx, ex, ey);
                    } else if (entities.get(a) == sensorShip) {
                        doSensorShip(gfx, ex, ey);
                    } else if (entities.get(a) instanceof Ship) {
                        if (!(entities.get(a) instanceof Explosion)) {
                            doShip(gfx, ex, ey, entities, a);
                        }
                    }
                }
            }
        }

        protected void doJumphole(ArrayList<Entity> entities, int a, double range, Graphics2D gfx) {
            //get radius
            Jumphole pl = (Jumphole) entities.get(a);
            double diam = pl.getDiameter();
            //get coordinates
            double ex = entities.get(a).getX() - diam / 2;
            double ey = entities.get(a).getY() - diam / 2;
            //adjust
            diam /= (range);
            diam *= width / 2;
            //adjust for player loc
            ex -= sensorShip.getX();
            ey -= sensorShip.getY();
            //adjust for size
            ex /= range;
            ey /= range;
            ex *= width / 2;
            ey *= height / 2;;
            //determine whether to draw the name based on range
            double fuzz = 1.0 - rnd.nextDouble() * 0.1f;
            double view = PLANET_AIM_LIMIT * sensorShip.getSensor() * fuzz;
            double dist = sensorShip.distanceTo(pl);
            if ((area < PLANET_AIM_LIMIT) || dist < view) {
                gfx.setColor(planetGrey);
                gfx.fillOval((int) ex + (width / 2), (int) ey + (height / 2), (int) diam, (int) diam);
                gfx.setColor(Color.MAGENTA);
                gfx.drawOval((int) ex + (width / 2), (int) ey + (height / 2), (int) diam, (int) diam);
                gfx.setColor(Color.pink);
                gfx.setFont(radarFont);
                gfx.drawString(pl.getName(), (int) (ex + diam / 2) + (width / 2) - 1, (int) (ey + diam / 2) + (height / 2) - 1);
            } else {
                gfx.setColor(Color.PINK);
                gfx.setFont(radarFont);
                gfx.drawString("NO AIM", (int) (ex + diam / 2) + (width / 2) - 1, (int) (ey + diam / 2) + (height / 2) - 1);
            }
        }

        protected void doPlanet(ArrayList<Entity> entities, int a, double range, Graphics2D gfx, double ex, double ey) {
            //get radius
            Planet pl = (Planet) entities.get(a);
            double diam = pl.getDiameter();
            diam /= (range);
            diam *= width / 2;
            //determine whether to draw the name based on range
            double fuzz = 1.0 - rnd.nextDouble() * 0.1f;
            double view = PLANET_AIM_LIMIT * sensorShip.getSensor() * fuzz;
            double dist = sensorShip.distanceTo(pl);
            if ((area < PLANET_AIM_LIMIT) || dist < view) {
                gfx.setColor(planetGrey);
                gfx.fillOval((int) ex + (width / 2), (int) ey + (height / 2), (int) diam, (int) diam);
                gfx.setColor(Color.DARK_GRAY);
                gfx.drawOval((int) ex + (width / 2), (int) ey + (height / 2), (int) diam, (int) diam);
                gfx.setColor(Color.pink);
                gfx.setFont(radarFont);
                gfx.drawString(pl.getName(), (int) (ex + diam / 2) + (width / 2) - 1, (int) (ey + diam / 2) + (height / 2) - 1);
            } else {
                gfx.setColor(Color.PINK);
                gfx.setFont(radarFont);
                gfx.drawString("NO AIM", (int) (ex + diam / 2) + (width / 2) - 1, (int) (ey + diam / 2) + (height / 2) - 1);
            }
        }

        protected void doSensorShip(Graphics2D gfx, double ex, double ey) {
            gfx.setColor(amber);
            gfx.drawRect((int) ex + (width / 2) - 2, (int) ey + (height / 2) - 2, 4, 4);
        }

        protected void doShip(Graphics2D gfx, double ex, double ey, ArrayList<Entity> entities, int a) {
            if (area < SHIP_AIM_LIMIT) {
                drawShipOnRadar(gfx, ex, ey, entities, a);
            } else if (area < STATION_AIM_LIMIT && entities.get(a) instanceof Station) {
                drawShipOnRadar(gfx, ex, ey, entities, a);
            }
            //player ships always visible
            Ship test = (Ship) entities.get(a);
            if (test.getUniverse().getPlayerProperty().contains(test)) {
                drawShipOnRadar(gfx, ex, ey, entities, a);
            }
        }

        protected void drawShipOnRadar(Graphics2D gfx, double ex, double ey, ArrayList<Entity> entities, int a) {
            //determine standings
            int standings = sensorShip.getStandingsToMe((Ship) entities.get(a));
            if (standings <= -3) {
                gfx.setColor(Color.RED);
            } else if (standings >= 3 && standings < 10) {
                gfx.setColor(Color.GREEN);
            } else if (standings == 10) {
                gfx.setColor(Color.MAGENTA);
            } else {
                gfx.setColor(Color.WHITE);
            }
            gfx.drawRect((int) ex + (width / 2) - 1, (int) ey + (height / 2) - 1, 2, 2);
            gfx.setFont(radarFont);
            if (entities.get(a) instanceof Station) {
                if (showStationNames) {
                    gfx.drawString(entities.get(a).getName(), (int) ex + (width / 2), (int) ey + (height / 2) - 1);
                }
            } else {
                if (showShipNames) {
                    if (!(entities.get(a) instanceof Projectile)) {
                        gfx.drawString(entities.get(a).getName(), (int) ex + (width / 2) - 1, (int) ey + (height / 2) - 1);
                    }
                }
            }
        }

        protected void drawVectorLines(Graphics2D gfx) {
            /*
             * IDEAS
             * 
             * - show the point on the map where you could be fully stopped at your current velocity
             * - allow you to "lock" a vector in red and keep it there for your own uses.
             */
            /*
             * These two lines represent the vector of your velocity and the vector
             * of your direction. They will simplify navigation in 2D space.
             */
            gfx.setColor(Color.CYAN);
            double dTheta = sensorShip.getTheta() - (Math.PI);
            double dpx = Math.cos(dTheta) * width / 2;
            double dpy = Math.sin(dTheta) * height / 2;
            gfx.drawLine(width / 2, (height / 2), (int) dpx + (width / 2), (int) dpy + (height / 2));
            //calculate direction vector
            gfx.setColor(Color.yellow);
            //calculate velocity vector
            double vTheta = FastMath.atan2(sensorShip.getVy(), sensorShip.getVx());
            double vpx = Math.cos(vTheta) * width / 2;
            double vpy = Math.sin(vTheta) * height / 2;
            if (!(sensorShip.getVx() == 0 && sensorShip.getVy() == 0)) {
                gfx.drawLine(width / 2, (height / 2), (int) vpx + (width / 2), (int) vpy + (height / 2));
            }
        }
    }

    private void generate() {
        backColor = windowGrey;
        //size this window
        width = 300;
        height = 300;
        setVisible(true);
        //setup range label
        rangeLabel.setText("range");
        rangeLabel.setName("range");
        rangeLabel.setX(0);
        rangeLabel.setY(0);
        rangeLabel.setWidth(120);
        rangeLabel.setHeight(25);
        rangeLabel.setVisible(true);
        //setup mode label
        modeLabel.setText("NO MODE");
        modeLabel.setName("mode");
        modeLabel.setX(0);
        modeLabel.setY(15);
        modeLabel.setWidth(120);
        modeLabel.setHeight(25);
        modeLabel.setVisible(true);
        //setup station label
        stationLabel.setText("STATIONS");
        stationLabel.setName("station");
        stationLabel.setX(width - 100);
        stationLabel.setY(0);
        stationLabel.setWidth(100);
        stationLabel.setHeight(25);
        stationLabel.setVisible(true);
        //setup ship label
        shipLabel.setText("SHIPS");
        shipLabel.setName("ship");
        shipLabel.setX(width - 100);
        shipLabel.setY(15);
        shipLabel.setWidth(100);
        shipLabel.setHeight(25);
        shipLabel.setVisible(true);
        //setup vel label
        velLabel.setText("REL SPEED");
        velLabel.setName("vel");
        velLabel.setX(0);
        velLabel.setY(getHeight() - 15);
        velLabel.setWidth(200);
        velLabel.setHeight(25);
        velLabel.setVisible(true);
        //setup radar
        radar.setName("radar");
        radar.setVisible(true);
        radar.setWidth(width);
        radar.setHeight(height);
        radar.setX(0);
        radar.setY(0);
        //pack
        addComponent(radar);
        addComponent(rangeLabel);
        addComponent(modeLabel);
        addComponent(shipLabel);
        addComponent(stationLabel);
        addComponent(velLabel);
    }

    private double magnitude(double dx, double dy) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    private double roundTwoDecimal(double d) {
        try {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            return Double.parseDouble(twoDForm.format(d));
        } catch (Exception e) {
            System.out.println("Not a Number");
            return 0;
        }
    }

    private double roundOneDecimal(double d) {
        try {
            DecimalFormat twoDForm = new DecimalFormat("#.#");
            return Double.parseDouble(twoDForm.format(d));
        } catch (Exception e) {
            System.out.println("Not a Number");
            return 0;
        }
    }

    public boolean isShowShipNames() {
        return showShipNames;
    }

    public void setShowShipNames(boolean showShipNames) {
        this.showShipNames = showShipNames;
    }

    public boolean isShowStationNames() {
        return showStationNames;
    }

    public void setShowStationNames(boolean showStationNames) {
        this.showStationNames = showStationNames;
    }

    @Override
    public void handleKeyReleasedEvent(KeyEvent ke) {
        /*
         * navmap keys
         */ if (ke.getKeyCode() == KeyEvent.VK_END) {
            incrementMode();
        } else if (ke.getKeyCode() == KeyEvent.VK_HOME) {
            decrementMode();
        } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            setShowShipNames(!isShowShipNames());
        } else if (ke.getKeyCode() == KeyEvent.VK_PAGE_UP) {
            setShowStationNames(!isShowStationNames());
        }
    }
}
