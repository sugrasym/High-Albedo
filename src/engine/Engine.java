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
 * Game engine.
 */
package engine;

import celestial.Celestial;
import celestial.Jumphole;
import celestial.Planet;
import celestial.Ship.CargoPod;
import celestial.Ship.Projectile;
import celestial.Ship.Ship;
import gdi.CargoWindow;
import gdi.EquipmentWindow;
import gdi.FuelWindow;
import gdi.HealthWindow;
import gdi.OverviewWindow;
import gdi.TradeWindow;
import gdi.VelocityWindow;
import gdi.component.AstralWindow;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import lib.AstralIO;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author Nathan Wiehoff
 */
public class Engine {
    //aspect ratio constants

    public static final double WIDE = 1.7778;
    public static final double STD = 1.3333;
    //graphics and threading
    BufferStrategy bf;
    Element render;
    //HUD
    protected HUD hud = new HUD();
    //dimensions
    private int uiX;
    private int uiY;
    //game entities
    ArrayList<Entity> entities;
    private Universe universe;
    //engine state
    double dilation = 1; // time dilation
    double dx = 0; //render x differential
    double dy = 0; //render y differential
    double pvx = 0; //player x velocity
    double pvy = 0; //player y velocity
    private Ship playerShip;
    //mouse
    private int mouseX;
    private int mouseY;
    //button state toggles
    private boolean allStopPressed = false;
    private boolean firing = false;

    public HUD getHud() {
        return hud;
    }

    public void setHud(HUD hud) {
        this.hud = hud;
    }

    enum State {

        RUNNING,
        PAUSED
    }
    State state = State.PAUSED;
    //io
    AstralIO io = new AstralIO();

    public Engine(BufferStrategy bf, int uiX, int uiY) {
        //store graphics
        this.uiX = uiX;
        this.uiY = uiY;
        this.bf = bf;
        //initialize entities
        entities = new ArrayList<>();
        //create components
        render = new Element();
        render.generateBackdrop();
        //halt components
        stop();
    }

    public void suicide() {
        /*
         * Cleans out everything! Yaay!
         */
        entities = new ArrayList<>();
        universe = null;
        playerShip = null;
        state = State.PAUSED;
    }

    public void resurrect() {
        /*
         * Brings the engine back to life after suicide called. This is used
         * under the assumption a game is being loaded.
         */
        for (int a = 0; a < entities.size(); a++) {
            entities.get(a).init(true);
        }
        state = State.RUNNING;
    }

    public void addEntity(Entity entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public void removeEntity(Entity entity) {
        if (entities.contains(entity)) {
            entities.remove(entity);
        }
    }

    public Universe getUniverse() {
        return universe;
    }

    public void setUniverse(Universe universe) {
        this.universe = universe;
        //add this universe's systems to the entity list
        for (int a = 0; a < universe.getSystems().size(); a++) {
            entities.add(universe.getSystems().get(a));
        }
        //store the player reference
        playerShip = universe.getPlayerShip();
    }

    /*
     * Begins execution
     */
    public final void start() {
        state = State.RUNNING;
    }

    /*
     * Pauses execution
     */
    public final void stop() {
        state = State.PAUSED;
    }

    /*
     * HUD class, makes sense to put it here.
     */
    public class HUD {

        ArrayList<AstralWindow> windows = new ArrayList<>();
        VelocityWindow velocityWindow = new VelocityWindow();
        HealthWindow healthWindow = new HealthWindow();
        FuelWindow fuelWindow = new FuelWindow();
        OverviewWindow overviewWindow = new OverviewWindow();
        EquipmentWindow equipmentWindow = new EquipmentWindow();
        CargoWindow cargoWindow = new CargoWindow();
        TradeWindow tradeWindow = new TradeWindow();

        public HUD() {
            //pack
            windows.add(velocityWindow);
            windows.add(healthWindow);
            windows.add(fuelWindow);
            windows.add(overviewWindow);
            windows.add(equipmentWindow);
            cargoWindow.setVisible(false);
            windows.add(cargoWindow);
            windows.add(tradeWindow);
        }

        public void render(Graphics f) {
            //position velocity window
            velocityWindow.setX((uiX / 2) - velocityWindow.getWidth() / 2);
            velocityWindow.setY(uiY - 40);
            //position health window
            healthWindow.setX((uiX / 2) - healthWindow.getWidth() / 2);
            healthWindow.setY(uiY - 55);
            //position fuel window
            fuelWindow.setX((uiX / 2) - fuelWindow.getWidth() / 2);
            fuelWindow.setY(uiY - 70);
            //position overview window
            overviewWindow.setX((uiX - (overviewWindow.getWidth() + 20)));
            overviewWindow.setY(uiY - (overviewWindow.getHeight() + 20));
            //position equipment window
            equipmentWindow.setX(20);
            equipmentWindow.setY(uiY - (equipmentWindow.getHeight() + 20));
            //position cargo window
            cargoWindow.setX((uiX / 2) - cargoWindow.getWidth() / 2);
            cargoWindow.setY((uiY / 2) - cargoWindow.getHeight() / 2);
            //position trade window
            tradeWindow.setX((uiX / 2) - tradeWindow.getWidth() / 2);
            tradeWindow.setY((uiY / 2) - tradeWindow.getHeight() / 2);
            //render
            for (int a = windows.size() - 1; a >= 0; a--) {
                windows.get(a).render(f);
            }
        }

        public void periodicUpdate() {
            //push hud changes
            velocityWindow.updateVelocity(pvx, pvy);
            healthWindow.updateHealth((playerShip.getShield() / playerShip.getMaxShield()),
                    (playerShip.getHull() / playerShip.getMaxHull()));
            fuelWindow.updateFuel(playerShip.getFuel() / playerShip.getMaxFuel());
            overviewWindow.updateOverview(playerShip);
            equipmentWindow.update(playerShip);
            cargoWindow.update(playerShip);
            tradeWindow.update(playerShip);
            //update
            for (int a = 0; a < windows.size(); a++) {
                windows.get(a).periodicUpdate();
            }
        }

        /*
         * The following are window event handlers. Do not add game logic to them.
         */
        public void checkFocusChanges() {
            /*
             * Window focus is determined based on mouse position.
             */
            Rectangle mRect = new Rectangle(mouseX, mouseY, 1, 1);
            boolean foundOne = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).intersects(mRect) && windows.get(a).isVisible() && !foundOne) {
                    windows.get(a).setFocused(true);
                    windows.get(a).setOrder(0);
                    foundOne = true;
                } else {
                    windows.get(a).setFocused(false);
                    windows.get(a).setOrder(windows.get(a).getOrder() - 1);
                }
            }
            if (foundOne) {
                /*
                 * Since sorting can be expensive, I only resort windows when the focus is known
                 * to have changed.
                 */
                AstralWindow arr[] = new AstralWindow[windows.size()];
                for (int a = 0; a < windows.size(); a++) {
                    arr[a] = windows.get(a);
                }
                for (int a = 0; a < arr.length; a++) {
                    for (int b = 0; b < arr.length; b++) {
                        if (arr[a].getOrder() > arr[b].getOrder()) {
                            AstralWindow tmp = arr[b];
                            arr[b] = arr[a];
                            arr[a] = tmp;
                        }
                    }
                }
                windows.clear();
                windows.addAll(Arrays.asList(arr));
            }
        }

        public void handleMouseMovedEvent(MouseEvent me) {
            //store mouse position
            mouseX = me.getX();
            mouseY = me.getY();
            //check to see if this needs to be intercepted
            boolean windowIntercepted = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                    windows.get(a).handleMouseMovedEvent(me);
                    windowIntercepted = true;
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
            }
        }

        public void handleMousePressedEvent(MouseEvent me) {
            boolean windowIntercepted = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                    windows.get(a).handleMousePressedEvent(me);
                    windowIntercepted = true;
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
            }
        }

        public void handleMouseReleasedEvent(MouseEvent me) {
            boolean windowIntercepted = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                    windows.get(a).handleMouseReleasedEvent(me);
                    windowIntercepted = true;
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
            }
        }

        public void handleMouseClickedEvent(MouseEvent me) {
            checkFocusChanges();
            boolean windowIntercepted = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                    windows.get(a).handleMouseClickedEvent(me);
                    windowIntercepted = true;
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
                Rectangle mRect = new Rectangle((int) dx + me.getX(), (int) dy + me.getY(), 1, 1);
                //check to see if it intersected any ships or objects
                ArrayList<Entity> tmpE = playerShip.getCurrentSystem().getCelestials();
                for (int a = 0; a < tmpE.size(); a++) {
                    if (tmpE.get(a) instanceof Ship) {
                        Ship tmp = (Ship) tmpE.get(a);
                        if (tmp.collideWith(mRect) && tmp != playerShip) {
                            playerShip.setTarget(tmp);
                            break;
                        }
                    }
                }
            }
        }

        public void handleKeyTypedEvent(KeyEvent ke) {
            boolean windowIntercepted = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                    windows.get(a).handleKeyTypedEvent(ke);
                    windowIntercepted = true;
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
            }
        }

        public void handleKeyPressedEvent(KeyEvent ke) {
            boolean windowIntercepted = false;
            for (int a = 0; a < windows.size(); a++) {
                if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                    windows.get(a).handleKeyPressedEvent(ke);
                    windowIntercepted = true;
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
                /*
                 * In-space
                 */
                if (!playerShip.isDocked()) {
                    if (ke.getKeyCode() == KeyEvent.VK_UP) {
                        playerShip.setThrustRear(true);
                    } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                        playerShip.setThrustForward(true);
                    } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                        playerShip.setRotatePlus(true);
                    } else if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                        playerShip.setRotateMinus(true);
                    } else if (ke.getKeyCode() == KeyEvent.VK_HOME) {
                        allStopPressed = true;
                    } else if (ke.getKeyCode() == KeyEvent.VK_SPACE) {
                        firing = true;
                    }
                } else {
                    /*
                     * Docked
                     */
                }
            }
        }

        public void handleKeyReleasedEvent(KeyEvent ke) {
            boolean windowIntercepted = false;
            if (ke.getKeyCode() == KeyEvent.VK_F5) {
                //defocus all windows
                for (int a = 0; a < windows.size(); a++) {
                    windows.get(a).setFocused(false);
                }
            } else if (ke.getKeyCode() == KeyEvent.VK_F6) {
                //defocus all windows and hide them
                for (int a = 0; a < windows.size(); a++) {
                    windows.get(a).setFocused(false);
                    windows.get(a).setVisible(false);
                }
                //show these since they are always visible
                velocityWindow.setVisible(true);
                healthWindow.setVisible(true);
                fuelWindow.setVisible(true);
            } else {
                for (int a = 0; a < windows.size(); a++) {
                    if (windows.get(a).isFocused() && windows.get(a).isVisible()) {
                        windows.get(a).handleKeyReleasedEvent(ke);
                        windowIntercepted = true;
                    }
                }
            }
            /*
             * Now game logic
             */
            if (!windowIntercepted) {
                /*
                 * In-space
                 */
                if (!playerShip.isDocked()) {
                    if (ke.getKeyCode() == KeyEvent.VK_UP) {
                        playerShip.setThrustRear(false);
                    } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                        playerShip.setThrustForward(false);
                    } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                        playerShip.setRotatePlus(false);
                    } else if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                        playerShip.setRotateMinus(false);
                    } else if (ke.getKeyCode() == KeyEvent.VK_HOME) {
                        allStopPressed = false;
                    }/*
                     * weapon keys
                     */ else if (ke.getKeyCode() == KeyEvent.VK_SPACE) {
                        firing = false;
                    } /*
                     * comms keys
                     */ else if (ke.getKeyCode() == KeyEvent.VK_D) {
                        playerShip.setAutopilot(Ship.Autopilot.DOCK_STAGE1);
                    }
                } else {
                    /*
                     * docked
                     */
                    if (ke.getKeyCode() == KeyEvent.VK_D) {
                        playerShip.undock();
                    }
                }
                /*
                 * The following commands are independent of being docked or not. Use with
                 * extreme caution and watch for overlap.
                 */
                if (ke.getKeyCode() == KeyEvent.VK_E) {
                    equipmentWindow.setVisible(!equipmentWindow.isVisible());
                }
                if (ke.getKeyCode() == KeyEvent.VK_S) {
                    overviewWindow.setVisible(!overviewWindow.isVisible());
                }
                if (ke.getKeyCode() == KeyEvent.VK_C) {
                    cargoWindow.setVisible(!cargoWindow.isVisible());
                }
                if (ke.getKeyCode() == KeyEvent.VK_T) {
                    if (playerShip.isDocked()) {
                        tradeWindow.setVisible(!tradeWindow.isVisible());
                    }
                }
                /*
                 * Time dilation keys
                 */
                if (ke.getKeyCode() == KeyEvent.VK_1) {
                    dilation = 0.25;
                }
                if (ke.getKeyCode() == KeyEvent.VK_2) {
                    dilation = 0.5;
                }
                if (ke.getKeyCode() == KeyEvent.VK_3) {
                    dilation = 1;
                }
                if (ke.getKeyCode() == KeyEvent.VK_4) {
                    dilation = 2;
                }
                if (ke.getKeyCode() == KeyEvent.VK_5) {
                    dilation = 4;
                }
            }
        }
    }
    /*
     * Responsible for drawing and updating the universe.
     */

    private class Element implements EngineElement {
        //timing

        private long lastFrame;
        //rendering helpers
        BufferedImage frame = new BufferedImage(uiX, uiY, BufferedImage.TYPE_INT_RGB); //double buffered frame
        Graphics2D f = (Graphics2D) frame.getGraphics(); //graphics context for the frame
        //per system
        Image backplate;
        //the thread thing
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        periodicUpdate();
                        //Thread.sleep(5);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        public Element() {
            th.start();
        }

        @Override
        public void periodicUpdate() {
            try {
                logic();
                render();
            } catch (IllegalStateException e) {
                //lol
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void generateBackdrop() {
            //generate the stock starry drop
            createStars();
        }

        private double getAspectRatio() {
            return ((double) uiX / (double) uiY);
        }

        private void createStars() {
            //determine aspect ratio
            double aspect = getAspectRatio();
            //determine whether we are closer to 16x9/16x10 or 4x3
            String plateGroup = "";
            if (Math.abs(aspect - STD) < Math.abs(aspect - WIDE)) {
                //aproximately std ratio
                plateGroup = "std";
            } else {
                //approximately wide ratio
                plateGroup = "wide";
            }
            //get base plate
            Image stars = io.loadImage("plate/" + plateGroup + "/base_plate.png");
            //scale backplate to screen size
            stars = stars.getScaledInstance(uiX, uiY, Image.SCALE_SMOOTH);
            backplate = stars;
        }

        /*
         * Rendering
         */
        private void render() throws Exception {
            //setup strategy
            //f.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            f.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //setup graphics
            Graphics g = bf.getDrawGraphics();
            //backplate
            f.drawImage(backplate, 0, 0, null);
            //update render view
            Rectangle view = new Rectangle((int) dx, (int) dy, uiX, uiY);
            //render entities in current solar system
            try {
                if (playerShip != null) {
                    SolarSystem current = playerShip.getCurrentSystem();
                    if (current != null) {
                        for (int a = 0; a < current.getCelestials().size(); a++) {
                            if (current.getCelestials().get(a).getState() != Entity.State.DEAD) {
                                if (current.getCelestials().get(a).collideWith(view)) {
                                    if (current.getCelestials().get(a) == playerShip.getTarget()) {
                                        renderTargetMarker();
                                    } else {
                                        if (current.getCelestials().get(a) instanceof Ship) {
                                            renderIFFMarker((Ship) current.getCelestials().get(a));
                                        }
                                    }
                                    current.getCelestials().get(a).render(f, dx, dy);
                                }
                            }
                        }
                        //render player ship
                        playerShip.render(f, dx, dy);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            //render HUD
            getHud().render(f);
            //use ui graphics context to draw
            if (!bf.contentsLost()) {
                g.drawImage(frame, 0, 0, null);
                bf.show();
            }
            Toolkit.getDefaultToolkit().sync();
        }

        /*
         * Updating
         */
        public void logic() {
            //get time since last frame
            long dt = System.nanoTime() - lastFrame;
            //store current time
            lastFrame = System.nanoTime();
            //calculate time per frame
            double tpf = Math.abs(dt / 1000000000.0) * dilation;
            if (state == State.RUNNING) {
                //handle player events
                handlePlayerEvents();
                //collission test
                collissionTest(tpf);
                //update game entities
                for (int a = 0; a < entities.size(); a++) {
                    entities.get(a).periodicUpdate(tpf);
                    if (entities.get(a).getState() == Entity.State.DEAD) {
                        //remove the entity
                        entities.remove(a);
                    }
                }
                //update hud
                getHud().periodicUpdate();
                //update differentials for rendering
                dx = (int) playerShip.getX() - (uiX / 2) + (playerShip.getWidth() / 2);
                dy = (int) playerShip.getY() - (uiY / 2) + (playerShip.getHeight() / 2);
                pvx = playerShip.getVx();
                pvy = playerShip.getVy();
            }
        }

        private void collissionTest(double tpf) {
            /*
             * 1. Collissions are not tested on planets
             * 2. Collissions are only tested between entities in the same solar system.
             */
            for (int a = 0; a < universe.getSystems().size(); a++) {
                ArrayList<Entity> objects = universe.getSystems().get(a).getCelestials();
                for (int b = 0; b < objects.size(); b++) {
                    if ((objects.get(b) instanceof Planet == false) || (objects.get(b) instanceof Jumphole)) {
                        for (int c = 0; c < objects.size(); c++) {
                            if ((objects.get(c) instanceof Planet == false) || (objects.get(c) instanceof Jumphole)) {
                                if (objects.get(b) != objects.get(c)) {
                                    if (objects.get(b).collideWith(objects.get(c))) {
                                        elasticCollision(objects.get(b), objects.get(c), tpf);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private void elasticCollision(Entity a, Entity b, double tpf) {
            //inform them of the collision for any special events
            a.informOfCollisionWith(b);
            b.informOfCollisionWith(a);
            //physics time
            if (a instanceof Celestial && b instanceof Celestial) {
                /*
                 * I don't really want this physics being applied between projectiles
                 * and their targets.
                 */
                if (!(a instanceof CargoPod || b instanceof CargoPod)) {
                    if (!(a instanceof Projectile || b instanceof Projectile)) {
                        if (!(a instanceof Jumphole || b instanceof Jumphole)) {
                            Celestial dummyA = (Celestial) a;
                            Celestial dummyB = (Celestial) b;
                            //get velocity and mass
                            double aVx = dummyA.getVx();
                            double aVy = dummyA.getVy();
                            double aM = dummyA.getMass();
                            double bVx = dummyB.getVx();
                            double bVy = dummyB.getVy();
                            double bM = dummyB.getMass();
                            //push them apart to avoid double counting and overlap
                            dummyA.setX(dummyA.getX() - aVx * tpf * 2.0);
                            dummyA.setY(dummyA.getY() - aVy * tpf * 2.0);
                            dummyB.setX(dummyB.getX() - bVx * tpf * 2.0);
                            dummyB.setY(dummyB.getY() - bVy * tpf * 2.0);
                            //determine center of mass's velocity
                            double cVx = (aVx * aM + bVx * bM) / (aM + bM);
                            double cVy = (aVy * aM + bVy * bM) / (aM + bM);
                            //reverse directions and de-reference frame
                            double aVx2 = -aVx + cVx;
                            double aVy2 = -aVy + cVy;
                            double bVx2 = -bVx + cVx;
                            double bVy2 = -bVy + cVy;
                            //store
                            dummyA.setVx(aVx2);
                            dummyA.setVy(aVy2);
                            dummyB.setVx(bVx2);
                            dummyB.setVy(bVy2);
                        }
                    }
                }
            }
        }

        /*
         * Ongoing event handling
         */
        private void handlePlayerEvents() {
            if (!playerShip.isDocked()) {
                if (allStopPressed) {
                    playerShip.decelerate();
                }
                if (firing) {
                    playerShip.fireActiveModules(null);
                }
            }
        }

        /*
         * special marker rendering
         */
        protected void renderTargetMarker() {
            //draw a marker around the player's target
            if (playerShip.getTarget() != null) {
                int tx = (int) (playerShip.getTarget().getX() - dx);
                int ty = (int) (playerShip.getTarget().getY() - dy);
                int tw = playerShip.getTarget().getWidth();
                int th = playerShip.getTarget().getHeight();
                f.setColor(Color.YELLOW);
                f.setStroke(new BasicStroke(3));
                f.drawOval(tx, ty, tw, th);
            }
        }

        protected void renderIFFMarker(Ship ship) {
            if (!(ship instanceof Projectile) && ship != playerShip) {
                //draw a marker to indicate standings
                int tx = (int) (ship.getX() - dx);
                int ty = (int) (ship.getY() - dy);
                int tw = ship.getWidth();
                int th = ship.getHeight();
                int standing = ship.getStandingsToMe(playerShip);
                if (standing > 3) {
                    f.setColor(Color.BLUE);
                } else if (standing < -3) {
                    f.setColor(Color.RED);
                } else {
                    f.setColor(Color.GRAY);
                }
                f.setStroke(new BasicStroke(2));
                f.drawOval(tx, ty, tw, th);
            }
        }
    }

    /*
     * Player state modifiers
     */
    public void setActivePlayerShip(Ship ship) {
        playerShip = ship;
    }

    public Ship getActivePlayerShip() {
        return playerShip;
    }
}
