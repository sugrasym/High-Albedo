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
 * A space station
 */
package celestial.Ship;

import cargo.Item;
import celestial.Ship.Process;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import lib.Parser;

/**
 *
 * @author nwiehoff
 */
public class Station extends Ship {
    //complex bound

    protected ArrayList<Rectangle> boundDef = new ArrayList<>();
    protected ArrayList<PortContainer> docks = new ArrayList<>();
    //products and resources for production
    protected ArrayList<Item> stationSelling = new ArrayList<>();
    protected ArrayList<Item> stationBuying = new ArrayList<>();
    //manufacturing
    protected ArrayList<Process> processes = new ArrayList<>();
    //cheating is needed sometimes
    private boolean exemptFromEconomics = false;

    public Station(String name, String type) {
        super(name, type);
    }

    @Override
    public void alive() {
        super.alive();
        //check if out of business
        if (cash < 0) {
            if (!isExemptFromEconomics()) {
                //out of business :(
                for (int a = 0; a < docks.size(); a++) {
                    docks.get(a).kickOut();
                }
                //so sad
                state = State.DYING;
                System.out.println(getName() + " was removed because it is out of business.");
            } else {
                //the station is above the system
                cash = rnd.nextInt(1000000);
                System.out.println(getName() + " was topped off because it is exempt from economics.");
            }
        }
        //check dockers
        for (int a = 0; a < docks.size(); a++) {
            docks.get(a).periodicUpdate(tpf);
        }
        //check processes
        for (int a = 0; a < processes.size(); a++) {
            processes.get(a).periodicUpdate(tpf);
        }
        //they never run out of fuel
        fuel = maxFuel;
        //don't move
        theta = 0;
        if (vx != 0 || vy != 0) {
            decelerate();
        }
    }

    @Override
    protected void autopilot() {
        //do nothing
    }

    @Override
    protected void behaviorTest() {
    }

    @Override
    protected void initStats() {
        /*
         * Loads the stats for this ship from the ships file.
         */
        //create parser
        Parser parse = new Parser("STATIONS.txt");
        //get the term with this ship's type
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Station");
        Parser.Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("type");
            if (termName.matches(getType())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        //now decode stats
        accel = Double.parseDouble(relevant.getValue("accel"));
        turning = Double.parseDouble(relevant.getValue("turning"));
        maxShield = Double.parseDouble(relevant.getValue("shield"));
        shieldRechargeRate = Double.parseDouble(relevant.getValue("shieldRecharge"));
        maxHull = hull = Double.parseDouble(relevant.getValue("hull"));
        maxFuel = fuel = Double.parseDouble(relevant.getValue("fuel"));
        //exemption block
        String exempt = relevant.getValue("exempt");
        exemptionSetup(exempt);
        //more stats
        setMass(Double.parseDouble(relevant.getValue("mass")));
        computeComplexRectangularBounds(relevant);
        computeDockBounds(relevant);
        computeProcesses(relevant);
        randomizeInitialGoods();
        //bring the ship to life
        state = State.ALIVE;
    }

    public int getPrice(Item item) {
        int max = 0;
        int min = 0;
        int q = 0;
        int s = 1;
        //get the right commodity
        boolean found = false;
        for (int a = 0; a < stationBuying.size(); a++) {
            if (stationBuying.get(a).getName().matches(item.getName())) {
                max = stationBuying.get(a).getMaxPrice();
                min = stationBuying.get(a).getMinPrice();
                q = stationBuying.get(a).getQuantity();
                s = stationBuying.get(a).getStore();
                found = true;
                break;
            }
        }
        if (!found) {
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().matches(item.getName())) {
                    max = stationSelling.get(a).getMaxPrice();
                    min = stationSelling.get(a).getMinPrice();
                    q = stationSelling.get(a).getQuantity();
                    s = stationSelling.get(a).getStore();
                    found = true;
                    break;
                }
            }
        }
        //calculate price
        int d = max - min;
        float per = (float) q / (float) s;
        int x = (int) (d * (1 - per));
        int price = min + x;
        if (price < min) {
            price = min;
        } else if (price > max) {
            price = max;
        }
        return price;
    }

    public void buy(Ship ship, Item item, int quantity) {
        //get current offer
        int price = getPrice(item);
        //repeat buy procedure
        for (int lx = 0; lx < quantity; lx++) {
            Item rel = null;
            //validate the item is available
            for (int a = 0; a < stationSelling.size(); a++) {
                if (stationSelling.get(a).getName().matches(item.getName())) {
                    //make sure there is something available
                    if (stationSelling.get(a).getQuantity() > 0) {
                        rel = stationSelling.get(a);
                    }
                    break;
                }
            }
            if (rel != null) {
                //validate the player can cover the charge
                if (ship.getCash() - price >= 0) {
                    //attempt transfer of item
                    if (ship.addToCargoBay(new Item(item.getName()))) {
                        //decrement stocks
                        rel.setQuantity(rel.getQuantity() - 1);
                        //transfer funds
                        ship.setCash(ship.getCash() - price);
                        setCash(getCash() + price);
                    }
                }
            }
        }
    }

    public void sell(Ship ship, Item item, int quantity) {
        if (item.getQuantity() == 1) {
            //get current offer
            int price = getPrice(item);
            //repeat sell procedure
            for (int lx = 0; lx < quantity; lx++) {
                Item rel = null;
                //validate the item is in the cargo bay
                for (int a = 0; a < ship.getCargoBay().size(); a++) {
                    if (ship.getCargoBay().get(a).getName().matches(item.getName())) {
                        rel = ship.getCargoBay().get(a);
                        break;
                    }
                }
                if (rel != null) {
                    //send to station
                    for (int a = 0; a < getStationBuying().size(); a++) {
                        if (rel.getName().matches(getStationBuying().get(a).getName())) {
                            getStationBuying().get(a).setQuantity(getStationBuying().get(a).getQuantity() + rel.getQuantity());
                            //remove from cargo
                            ship.removeFromCargoBay(rel);
                            //pay the ship
                            ship.setCash(ship.getCash() + price);
                            //remove funds from station wallet
                            setCash(getCash() - price);
                            break;
                        }
                    }
                }
            }
        }
    }

    protected void randomizeInitialGoods() {
        if (stationSelling.size() > 0) {
            for (int a = 0; a < stationSelling.size(); a++) {
                stationSelling.get(a).setQuantity(rnd.nextInt(stationSelling.get(a).getStore()));
            }
        }
        if (stationBuying.size() > 0) {
            for (int a = 0; a < stationBuying.size(); a++) {
                stationBuying.get(a).setQuantity(rnd.nextInt(stationBuying.get(a).getStore()));
            }
        }
    }

    protected void computeComplexRectangularBounds(Parser.Term relevant) throws NumberFormatException {
        //do complex rectangular bounds (useful for stations)
        {
            /*
             * WARNING: COMPLEX RECTANGULAR BOUNDS DO NOT GET ROTATED WHEN
             * THE SHIP ROTATES! IF YOUR CELESTIAL IS GOING TO BE DOING
             * A LOT OF ROTATING CONSIDER ANOTHER OPTION.
             */
            String complex = relevant.getValue("rectBound");
            if (complex != null) {
                String[] arr = complex.split("/");
                for (int a = 0; a < arr.length; a++) {
                    String[] re = arr[a].split(",");
                    int bx0 = Integer.parseInt(re[0]);
                    int by0 = Integer.parseInt(re[1]);
                    int bx1 = Integer.parseInt(re[2]);
                    int by1 = Integer.parseInt(re[3]);
                    //calculate rectangular region
                    int w = bx1 - bx0;
                    int h = by1 - by0;
                    Rectangle rect = new Rectangle(bx0, by0, w, h);
                    boundDef.add(rect);
                }
            }
        }
    }

    protected void computeDockBounds(Parser.Term relevant) throws NumberFormatException {
        //do complex rectangular bounds (useful for stations)
        {
            /*
             * WARNING: COMPLEX RECTANGULAR BOUNDS DO NOT GET ROTATED WHEN
             * THE SHIP ROTATES! IF YOUR CELESTIAL IS GOING TO BE DOING
             * A LOT OF ROTATING CONSIDER ANOTHER OPTION.
             */
            String complex = relevant.getValue("dock");
            if (complex != null) {
                String[] arr = complex.split("/");
                for (int a = 0; a < arr.length; a++) {
                    String[] re = arr[a].split(",");
                    int bx0 = Integer.parseInt(re[0]);
                    int by0 = Integer.parseInt(re[1]);
                    int bx1 = Integer.parseInt(re[2]);
                    int by1 = Integer.parseInt(re[3]);
                    int ax = Integer.parseInt(re[4]);
                    int ay = Integer.parseInt(re[5]);
                    //calculate rectangular region
                    int w = bx1 - bx0;
                    int h = by1 - by0;
                    docks.add(new PortContainer(this, bx0, by0, w, h, ax, ay));
                }
            }
        }
    }

    protected void computeProcesses(Parser.Term relevant) throws NumberFormatException {
        //generates the processes that were linked to this station
        {
            String raw = relevant.getValue("process");
            if (raw != null) {
                String[] arr = raw.split("/");
                for (int a = 0; a < arr.length; a++) {
                    Process p = new Process(this, arr[a], stationSelling, stationBuying);
                    if (p != null) {
                        processes.add(p);
                    }
                }
            }
        }
    }

    @Override
    protected void updateBound() {
        bound.clear();
        if (boundDef.size() < 1) {
            bound.add(new Rectangle((int) getX(), (int) getY(), getWidth(), getHeight()));
        } else {
            //create complex rectangular bounds
            for (int a = 0; a < boundDef.size(); a++) {
                Rectangle tmp = boundDef.get(a);
                int bx = (int) getX() + tmp.x;
                int by = (int) getY() + tmp.y;
                bound.add(new Rectangle(bx, by, tmp.width, tmp.height));
            }
        }
    }

    public boolean canDock(Ship ship) {
        for (int a = 0; a < docks.size(); a++) {
            if (docks.get(a).canFit(ship) && docks.get(a).isAvailable(ship)) {
                if (ship.getStandingsToMe(this) > -2) {
                    return true;
                }
            } else {
                System.out.println(docks.get(a).canFit(ship) + " " + a);
                System.out.println(docks.get(a).getClient() + " " + a);
            }
        }
        return false;
    }

    public PortContainer requestDockPort(Ship ship) {
        /*
         * Returns an available docking port if docking is permitted and there
         * are ports available.
         */
        for (int a = 0; a < docks.size(); a++) {
            if (docks.get(a).canFit(ship) && docks.get(a).isAvailable(ship)) {
                if (ship.getStandingsToMe(this) > -2) {
                    docks.get(a).setClient(ship);
                    return docks.get(a);
                }
            }
        }
        return null;
    }

    @Override
    public void initGraphics() {
        try {
            //get the image
            raw_tex = io.loadImage("station/" + type + ".png");
            //create the usable version
            ImageIcon icon = new ImageIcon(raw_tex);
            setHeight(icon.getIconHeight());
            setWidth(icon.getIconWidth());
            tex = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disposeGraphics() {
        raw_tex = null;
        tex = null;
    }

    @Override
    public void render(Graphics g, double dx, double dy) {
        theta = 0;
        if (tex != null) {
            //setup the buffer's graphics
            Graphics2D f = tex.createGraphics();
            //clear the buffer
            f.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            f.fillRect(0, 0, getWidth(), getHeight());
            f.setComposite(AlphaComposite.Src);
            //enable anti aliasing
            f.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            f.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //draw the updated version
            {
                //create an affine transform
                AffineTransform rot = new AffineTransform();
                rot.rotate(0, getWidth() / 2, getHeight() / 2);
                //apply transform
                f.transform(rot);
                f.drawImage(raw_tex, 0, 0, null);
                //draw docking reticles
                for (int a = 0; a < docks.size(); a++) {
                    docks.get(a).render(f);
                }
            }
            drawHealthBars(g, dx, dy);
            //draw the buffer onto the main frame
            g.drawImage(tex, (int) (getX() - dx), (int) (getY() - dy), null);
        } else {
            initGraphics();
        }
    }

    @Override
    protected void drawHealthBars(Graphics g, double dx, double dy) {
        /*//draw the bounds
         for (int a = 0; a < getBounds().size(); a++) {
         double bx = getBounds().get(a).x;
         double by = getBounds().get(a).y;
         int bw = getBounds().get(a).width;
         int bh = getBounds().get(a).height;
         g.setColor(Color.PINK);
         g.drawRect((int) (bx - dx), (int) (by - dy), bw, bh);
         }*/
        //draw health bars
        double hullPercent = hull / maxHull;
        double shieldPercent = shield / maxShield;
        g.setColor(Color.RED);
        g.fillRect((int) (getX() - dx), (int) (getY() - dy), (int) (getWidth() * hullPercent), 2);
        g.setColor(Color.GREEN);
        g.fillRect((int) (getX() - dx), (int) (getY() - dy), (int) (getWidth() * shieldPercent), 2);
    }

    @Override
    public String toString() {
        String ret = "";
        {
            ret = name + ", " + faction;
        }
        return ret;
    }

    public ArrayList<Item> getStationSelling() {
        return stationSelling;
    }

    public void setStationSelling(ArrayList<Item> stationSelling) {
        this.stationSelling = stationSelling;
    }

    public ArrayList<Item> getStationBuying() {
        return stationBuying;
    }

    public void setStationBuying(ArrayList<Item> stationBuying) {
        this.stationBuying = stationBuying;
    }

    public ArrayList<Process> getProcesses() {
        return processes;
    }

    public void setProcesses(ArrayList<Process> processes) {
        this.processes = processes;
    }

    public boolean isExemptFromEconomics() {
        return exemptFromEconomics;
    }

    public void setExemptFromEconomics(boolean exemptFromEconomics) {
        this.exemptFromEconomics = exemptFromEconomics;
    }

    private void exemptionSetup(String exempt) {
        //exemption block
        if (exempt != null) {
            exemptFromEconomics = Boolean.parseBoolean(exempt);
        } else {
            exemptFromEconomics = false;
        }
        if (exemptFromEconomics) {
            System.out.println(getName() + " is exempted from economics.");
        }
    }
}
