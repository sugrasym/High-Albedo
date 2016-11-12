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
 * A space ship!
 */
package celestial.Ship;

import cargo.Equipment;
import cargo.Hardpoint;
import cargo.Item;
import cargo.Weapon;
import celestial.Celestial;
import celestial.Jumphole;
import engine.Entity;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import lib.AstralMessage;
import lib.Binling;
import lib.Conversation;
import lib.Faction;
import lib.FastMath;
import lib.Parser;
import lib.Parser.Param;
import lib.Parser.Term;
import lib.Soundling;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author Nathan Wiehoff
 */
public class Ship extends Celestial {

    /*
     * Behaviors are over-arching goals and motivations such as hunting down
     * hostiles or trading. The behave() method will keep track of any
     * variables it needs and call autopilot functions as needed to realize
     * these goals.
     */
    public enum Behavior {

        NONE,
        TEST,
        PATROL,
        SECTOR_TRADE,
        UNIVERSE_TRADE, //requires a jump drive
        SUPPLY_HOMEBASE, //requires a jump drive
        REPRESENT_HOMEBASE, //requires a jump drive
    }

    /*
     * Autopilot functions are slices of behavior that are useful as part of
     * a big picture.
     */
    public enum Autopilot {

        NONE, //nothing
        WAIT, //waiting
        WAITED, //done waiting
        DOCK_STAGE1, //get permission, go to the alignment vector
        DOCK_STAGE2, //fly into docking area
        DOCK_STAGE3, //fly into docking port
        UNDOCK_STAGE1, //fly into docking area
        UNDOCK_STAGE2, //align to alignment vector
        UNDOCK_STAGE3, //accelerate and release
        FLY_TO_CELESTIAL, //fly to a celestial
        ATTACK_TARGET, //attack current target
        ALL_STOP, //slow down until velocity is 0
        FOLLOW, //follow a target at a range
    }
    //constants
    public static final double LOOT_DROP_PROBABILITY = 0.21;
    public static final double PATROL_REFUEL_PERCENT = 0.5;
    public static final double TRADER_RESERVE_PERCENT = 0.5;
    public static final double TRADER_REFUEL_PERCENT = 0.25;
    public static final double TRADER_JD_SAFETY_FUEL = 0.40;
    public static final double PLAYER_AGGRO_SHIELD = 0.5;
    public static final double MAX_JUMP_SHIELD_DAMAGE = 0.45;
    public static final double JUMP_SAFETY_FUEL = 0.25;
    public static final int HOSTILE_STANDING = -2;
    public static final String PLAYER_FACTION = "Player";
    public static final double MAX_WAIT_TIME = 25;
    public static final double MIN_WAIT_TIME = 5;
    public static final double BAIL_CHECK_SHIELD_RESET = 1;
    public static final double BAIL_CHECK_SHIELD_STOP = 0.1;
    public static final double BAIL_CHECK_HULL_DAMAGE = 0.5;
    //raw loadout
    protected String equip = "";
    private String template = "";
    private boolean initialized = false;
    //info
    private boolean alternateString = false;
    //texture and type
    protected transient Image raw_tex;
    protected transient BufferedImage tex;
    protected String type;
    //'pilot name'
    private String pilot = "Unknown";
    //faction
    protected Faction myFaction;
    protected String faction;
    //group
    protected String group = "NONE";
    //plot ship switch
    private boolean plotShip = false;
    //timing and waiting
    private double waitTimer = 0;
    private double waitTimerLength = 0;
    //death
    private String explosion = "Explosion";
    //remove control (ex player control) switches
    private boolean controlThrustForward = false;
    private boolean controlThrustReverse = false;
    private boolean rotateMinus = false;
    private boolean rotatePlus = false;
    //navigational aid
    private double autopilotRange = 0; //how close to fly to something
    private Celestial flyToTarget;
    //wallet
    protected long cash = 10000000;
    protected Station homeBase;
    //behavior and autopilot
    protected Behavior behavior = Behavior.NONE;
    protected Autopilot autopilot = Autopilot.NONE;
    protected boolean docked;
    protected PortContainer port;
    //bailing
    protected double courage = 1; //determines how likely a ship is NOT to bail.
    protected boolean checkedBail = false;
    protected boolean bailed = false;
    //trading
    private Station buyFromStation;
    private int buyFromPrice;
    private Station sellToStation;
    private int sellToPrice;
    private Item workingWare;
    //acceleration
    protected double accel = 0;
    protected double turning = 0;
    //fuel
    protected double fuel;
    protected double maxFuel;
    protected boolean infiniteFuel = false;
    //shield and hull
    protected double shield;
    protected double maxShield;
    protected double shieldRechargeRate;
    protected double hull;
    protected double maxHull;
    //bound
    protected ArrayList<Rectangle> bound = new ArrayList<>();
    //sensor
    protected double sensor;
    protected Ship target;
    private boolean scanForContraband = false;
    //detecting aggro
    private Ship lastBlow = this;
    //cargo
    protected double cargo;
    protected ArrayList<Hardpoint> hardpoints = new ArrayList();
    protected ArrayList<Item> cargoBay = new ArrayList();
    //RNG
    Random rnd = new Random();
    //communications
    private ArrayList<AstralMessage> messages = new ArrayList<>();
    private Conversation conversation;
    private boolean plotOffer = false;
    //media switches
    protected boolean thrusting = false;
    //sound que
    private transient ArrayList<Soundling> soundQue;
    //sound effects
    private transient Soundling engineLoop;
    private transient Soundling notifyMessage;
    //optimization of collission testing
    private double last_theta;

    public Ship(String name, String type) {
        setName(name);
        setType(type);
    }

    private String makeName() {
        /*
         * Generates a random name for this ship's pilot.
         */
        ArrayList<Term> fg = Universe.getCache().getNameCache().getTermsOfType("First");
        ArrayList<Term> lg = Universe.getCache().getNameCache().getTermsOfType("Last");
        String first = "";
        String last = "";
        {
            for (int a = 0; a < fg.size(); a++) {
                if (fg.get(a).getValue("name").equals("Generic")) {
                    Param pick = fg.get(a).getParams().get(rnd.nextInt(fg.get(a).getParams().size() - 1) + 1);
                    first = pick.getValue();
                    break;
                }
            }

            for (int a = 0; a < lg.size(); a++) {
                if (lg.get(a).getValue("name").equals("Generic")) {
                    Param pick = lg.get(a).getParams().get(rnd.nextInt(lg.get(a).getParams().size() - 1) + 1);
                    last = pick.getValue();
                    break;
                }
            }
        }

        return first + " " + last;
    }

    @Override
    public void init(boolean loadedGame) {
        if (!loadedGame) {
            //setup stats
            initStats();
        }
        state = State.ALIVE;
    }

    @Override
    public void initGraphics() {
        try {
            if (getUniverse() != null) {
                /*
                 * Generate graphics
                 */
                //get the image
                raw_tex = Universe.getCache().getShipSprite(getType());
                //create the usable version
                ImageIcon icon = new ImageIcon(raw_tex);
                setHeight(icon.getIconHeight());
                setWidth(icon.getIconWidth());
                tex = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int a = 0; a < hardpoints.size(); a++) {
                    Equipment mount = hardpoints.get(a).getMounted();
                    if (mount != null) {
                        if (mount instanceof Weapon) {
                            Weapon tmp = (Weapon) mount;
                            tmp.initGraphics();
                        }
                    }
                }
                /*
                 * Generate audio
                 */
                //engine loop
                engineLoop = new Soundling("engineLoop", "audio/effects/engine loop.wav", true);
                notifyMessage = new Soundling("notifyMessage", "audio/effects/notify message.wav", false);
                //update bound
                bound.clear();
                updateBound();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disposeGraphics() {
        raw_tex = null;
        tex = null;
        for (int a = 0; a < hardpoints.size(); a++) {
            Equipment mount = hardpoints.get(a).getMounted();
            if (mount != null) {
                if (mount instanceof Weapon) {
                    Weapon tmp = (Weapon) mount;
                    tmp.disposeGraphics();
                }
            }
        }
        //dispose of audio
        killSounds();
        engineLoop = null;
        notifyMessage = null;
    }

    protected void initStats() {
        //don't initialize multiple times
        if (initialized) {
            return;
        }
        /*
         * Loads the stats for this ship from the ships file.
         */
        initialized = true;
        //create parser
        Parser parse = Universe.getCache().getShipCache();
        //get the term with this ship's type
        ArrayList<Term> terms = parse.getTermsOfType("Ship");
        Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("type");
            if (termName.equals(getType())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            //now decode stats
            accel = Double.parseDouble(relevant.getValue("accel"));
            turning = Double.parseDouble(relevant.getValue("turning"));
            shield = maxShield = Double.parseDouble(relevant.getValue("shield"));
            shieldRechargeRate = Double.parseDouble(relevant.getValue("shieldRecharge"));
            maxHull = hull = Double.parseDouble(relevant.getValue("hull"));
            maxFuel = fuel = Double.parseDouble(relevant.getValue("fuel"));
            //check inf fuel
            if (fuel == -1) {
                infiniteFuel = true;
                maxFuel = fuel = Double.MAX_VALUE / 2;
            }
            setMass(Double.parseDouble(relevant.getValue("mass")));
            sensor = Double.parseDouble(relevant.getValue("sensor"));
            cargo = Double.parseDouble(relevant.getValue("cargo"));
            //initial width and height for OOS indeterminate
            String ws = relevant.getValue("width");
            String hs = relevant.getValue("height");
            if (ws != null && hs != null) {
                width = Integer.parseInt(ws);
                height = Integer.parseInt(hs);
            }
            //hardpoints
            installHardpoints(relevant);
            //equipment
            installLoadout();
            //faction
            installFaction();
            pilot = makeName();
            //bring the ship to life
            state = State.ALIVE;
        } else {
            System.out.println("The item " + getType() + " does not exist in SHIPS.txt");
        }
    }

    @Override
    public void alive() {
        aliveAlways();
        if (docked) {
            aliveInDock();
        } else {
            aliveInSpace();
            //update sounds
            updateSoundEffects();
            updateSoundSwitches();
        }
    }

    protected void aliveAlways() {
        //check homebase
        if (homeBase != null) {
            if (homeBase.getState() == State.ALIVE) {
                //do nothing
            } else {
                //clear homebase
                clearHomeBase();
            }
        }
        //check infinite fuel
        if (infiniteFuel) {
            fuel = maxFuel;
        }
        //update conversation
        if (getConversation() != null) {
            getConversation().periodicUpdate(tpf);
            if (getConversation().isDone()) {
                conversation = null;
            }
        }
        //update hard points
        for (int a = 0; a < hardpoints.size(); a++) {
            hardpoints.get(a).periodicUpdate(tpf);
        }
        //recharge shield
        if (getShield() < getMaxShield() && getFuel() > 0) {
            setShield(getShield() + getShieldRechargeRate() * tpf);
            //setFuel(getFuel() - getShieldRechargeRate() * 0.1 * tpf);
        } else if (getShield() > getMaxShield()) {
            setShield(getMaxShield());
        }
        //translate
        setX(getX() + getVx() * tpf);
        setY(getY() + getVy() * tpf);
        //fix theta
        theta = (theta + 2.0 * Math.PI) % (2.0 * Math.PI);
        //am i dead?
        if (hull <= 0) {
            state = State.DYING;
            System.out.println(getName() + " was destroyed in " + currentSystem.getName() + " by " + lastBlow.getName());
            //did the player destroy this ship?
            if (lastBlow.getFaction().equals(PLAYER_FACTION)) {
                //adjust the player's standings accordingly
                if (!faction.equals("Neutral")) {
                    getUniverse().getPlayerShip().getMyFaction().derivedModification(myFaction, -1.0);
                }
            }
        } else {
            if (hull > maxHull) {
                hull = maxHull;
            }
            syncStandings();
            behave();
        }
    }

    private void syncStandings() {
        /*
         * Keeps all player owned property in sync with the player's current
         * actions.
         */
        if (faction.hashCode() == PLAYER_FACTION.hashCode()) {
            if (getUniverse() != null) {
                myFaction = getUniverse().getPlayerShip().getMyFaction();
                messages = getUniverse().getPlayerShip().getMessages();
                alternateString = true;
            }
        }
    }

    protected void aliveInDock() {
        //stop ship
        vx = 0;
        vy = 0;
        //refuel
        fuel = maxFuel;
        shield = maxShield;
        //kill non waiting autopilots
        if (autopilot != Autopilot.WAIT && autopilot != Autopilot.WAITED) {
            autopilot = Autopilot.NONE;
        }
        //stop sounds
        killSounds();
        //run autopilot block (should only be running wait)
        autopilot();
    }

    protected void aliveInSpace() {
        if (!docked) {
            //update engines
            if (isThrustForward()) {
                fireForwardThrusters();
            }
            if (isThrustRear()) {
                fireRearThrusters();
            }
            if (isRotateMinus()) {
                rotateMinus();
            }
            if (isRotatePlus()) {
                rotatePlus();
            }
            //update sensors for target
            if (target != null) {
                try {
                    //defererence if it isn't in the same solar system
                    if (target.getCurrentSystem() != getCurrentSystem()) {
                        target = null;
                    } else {
                        if (distanceTo(target) > getSensor()) {
                            target = null;
                        }
                        if (target.getState() == State.DEAD) {
                            target = null;
                        }
                    }
                } catch (Exception e) {
                    target = null;
                }
            }
            //avoidance code
            if (autopilot != Autopilot.NONE) {
                Ship obstruction = avoidCollission();
                if (obstruction == null || isExemptFromAvoidance()) {
                    autopilot();
                } else {
                    autopilotAvoidBlock(obstruction);
                }
            }
            //bail code
            if (!faction.equals(PLAYER_FACTION)) {
                //player ships do not bail
                checkBail();
            }
        }
    }

    /*
     * Autopilot
     */
    protected void autopilot() {
        /*if (currentSystem == getUniverse().getPlayerShip().currentSystem) {
         System.out.println("A: " + autopilot + " :: " + "B: " + behavior);
         }*/
        try {
            if (getAutopilot() == Autopilot.NONE) {
                //do nothing
            } else if (null != autopilot) {
                switch (autopilot) {
                    case FLY_TO_CELESTIAL:
                        //call components
                        autopilotFlyToBlock();
                        break;
                    case WAIT:
                        autopilotWaitBlock();
                        break;
                    case FOLLOW:
                        autopilotFollowBlock();
                        break;
                    case ALL_STOP:
                        autopilotAllStopBlock();
                        break;
                    default:
                        autopilotFightingBlock();
                        autopilotDockingBlock();
                        autopilotUndockingBlock();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println(getName() + " encountered an autopilot issue. (" + autopilot + ", " + behavior + ")");
            e.printStackTrace();
            autopilot = Autopilot.NONE;
        }
    }

    protected boolean isExemptFromAvoidance() {
        if (null != getAutopilot()) {
            switch (getAutopilot()) {
                case DOCK_STAGE2:
                    return true;
                case DOCK_STAGE3:
                    return true;
                case UNDOCK_STAGE1:
                    return true;
                case UNDOCK_STAGE2:
                    return true;
                case ATTACK_TARGET:
                    return true;
                default:
                    break;
            }
        }
        return false;
    }

    protected void autopilotAvoidBlock(Ship obstruction) {
        if (obstruction != null) {
            //calculate angle between our centers
            double tcx = obstruction.getCenterX();
            double tcy = obstruction.getCenterY();
            double cx = getCenterX();
            double cy = getCenterY();
            //solution distances
            double solPlus;
            double solMinus;
            //predict future location
            double fT = getTheta() + Math.PI / 2;
            double tdx = accel * Math.cos(fT);
            double tdy = accel * Math.sin(fT);
            solPlus = magnitude(tcx - (cx + tdx), tcy + -(cy + tdy));
            fT = getTheta() - Math.PI / 2;
            tdx = accel * Math.cos(fT);
            tdy = accel * Math.sin(fT);
            solMinus = magnitude(tcx - (cx + tdx), tcy + -(cy + tdy));
            //use the best solution
            if (solPlus < solMinus) {
                straffPositive();
            } else {
                straffNegative();
            }
        }
    }

    protected Ship avoidCollission() {
        /*
         * This method is responsible for avoiding collissions by interrupting
         * the normal autopilot code if a future collission is detected and making
         * a course correction.
         *
         *
         */
        //create a list for storing candidates
        ArrayList<Ship> candidates = new ArrayList<>();
        //create a rectangle for testing ahead of the ship
        Rectangle thrustRect = getDodgeLine().getBounds();
        //get a list of all entities in my solar system
        ArrayList<Entity> entities = getCurrentSystem().getEntities();
        //we only care about celestials extending the Ship class that are not projectiles
        for (int a = 0; a < entities.size(); a++) {
            if (entities.get(a) instanceof Ship) {
                if (!(entities.get(a) instanceof Projectile)) {
                    Ship test = (Ship) entities.get(a);
                    if (test != this) {
                        if (test.collideWith(thrustRect)) {
                            candidates.add(test);
                        }
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        } else {
            Ship ret = candidates.get(0);
            double rec = Double.MAX_VALUE;
            for (int a = 0; a < candidates.size(); a++) {
                double dist = candidates.get(a).distanceTo(ret);
                if (dist < rec) {
                    ret = candidates.get(a);
                    rec = dist;
                }
            }
            return ret;
        }
    }

    public Line2D getDodgeLine() {
        double iT = theta;
        double dx;
        double dy;
        //generate differences
        dx = -4 * Math.abs(getWidth()) * Math.cos(iT);
        dy = -4 * Math.abs(getHeight()) * Math.sin(iT);
        //generate dodge line
        Line2D tmp = new Line2D.Double();
        //
        //create the end points
        double ax = getX() + (getWidth() / 2);
        double ay = getY() + (getHeight() / 2);
        double bx = (ax) + dx;
        double by = (ay) + dy;
        //get some fuzziness for negotiating hard situations
        double fuzzX = getWidth() * (rnd.nextDouble() - 0.5);
        double fuzzY = getHeight() * (rnd.nextDouble() - 0.5);
        //handle edge cases
        {
            //literally, this alorithm fails on 0* 90* 180* 270*
            //this algorithm uses a 0.5 tolerance, 0.25 in each direction.
            if (theta > 6.03 || theta < 0.25) {
                //remove fuzziness in special cases
                fuzzX = 0;
                fuzzY = 0;
                //this is the pi and 2pi case
                ax = getX() + (getWidth() / 2);
                ay = getY();
                bx = (ax) + dx;
                by = (ay) + getHeight();
            }
            if (theta > 1.32 && theta < 1.82) {
                //remove fuzziness in special cases
                fuzzX = 0;
                fuzzY = 0;
                //this is the pi/2 case
                ax = getX();
                ay = getY() + (getHeight() / 2);
                bx = ax + getWidth();
                by = ay + dy;
            }
            if (theta > 2.89 && theta < 3.39) {
                //remove fuzziness in special cases
                fuzzX = 0;
                fuzzY = 0;
                //this is the pi case
                ax = getX() + (getWidth() / 2);
                ay = getY();
                bx = (ax) + dx;
                by = (ay) + getHeight();
            }
            if (theta > 4.46 && theta < 4.96) {
                //remove fuzziness in special cases
                fuzzX = 0;
                fuzzY = 0;
                //this is the 3pi/2 case
                ax = getX();
                ay = getY() + (getHeight() / 2);
                bx = ax + getWidth();
                by = ay + dy;
            }
        }
        //set line
        tmp.setLine(ax, ay, bx + fuzzX, by + fuzzY);
        return tmp;
    }

    /*
     * Command "Blocks"
     */
    public void cmdFlyToCelestial(Celestial destination, double range) {
        /*
         * Fly to within a certain range of a celestial
         */
        flyToTarget = destination;
        autopilotRange = range;
        autopilot = Autopilot.FLY_TO_CELESTIAL;
    }

    public void cmdFollowShip(Ship destination, double range) {
        /*
         * Fly to within a certain range of a celestial
         */
        flyToTarget = destination;
        autopilotRange = range;
        autopilot = Autopilot.FOLLOW;
    }

    public void cmdDock(Ship ship) {
        /*
         * Wrapper for cmdDock
         */
        if (ship instanceof Station) {
            cmdDock((Station) ship);
        }
    }

    private void dockAtFriendlyStationInSystem() {
        abortTrade();
        //wait
        ArrayList<Station> fstat = getFriendlyStationsInSystem();
        Station near = fstat.get(rnd.nextInt(fstat.size()));
        if (near != null) {
            cmdDock(near);
        } else {
            cmdAllStop();
        }
    }

    public void cmdDock(Station station) {
        /*
         * Dock at a station
         */
        setFlyToTarget(station);
        setAutopilot(Autopilot.DOCK_STAGE1);
    }

    /*
     * Autopilot "Blocks"
     */
    protected void autopilotFollowBlock() {
        if (getAutopilot() == Autopilot.FOLLOW) {
            if (flyToTarget != null) {
                if (flyToTarget.getCurrentSystem() == currentSystem) {
                    double dist = distanceTo(flyToTarget);
                    if (dist < (autopilotRange) + (getWidth() * 2)) {
                        decelerate();
                    } else if (dist > (autopilotRange) + (getWidth() * 6)) {
                        moveToPositionWithHold(flyToTarget.getX(), flyToTarget.getY(), getFollowHold());
                    } else {
                        //wait
                    }
                } else {
                    //determine if this is a system we could jump to
                    SolarSystem targetSystem = flyToTarget.getCurrentSystem();
                    if (canJump(targetSystem)) {
                        //jump to follow target
                        cmdJump(targetSystem);
                    } else {
                        //abort follow
                        cmdAllStop();
                    }
                }
            } else {
                autopilot = Autopilot.NONE;
            }
        }
    }

    protected void autopilotFlyToBlock() {
        if (getAutopilot() == Autopilot.FLY_TO_CELESTIAL) {
            if (flyToTarget != null) {
                if (flyToTarget.getCurrentSystem() == currentSystem) {
                    double dist = distanceTo(flyToTarget);
                    if (dist <= autopilotRange) {
                        cmdAllStop();
                    } else {
                        //determine correct hold to use
                        double hold;
                        if (dist <= getFlightHold()) {
                            hold = dist;
                        } else {
                            hold = getFlightHold();
                        }
                        //move to position using our hold
                        double tCx = flyToTarget.getCenterX();
                        double tCy = flyToTarget.getCenterY();
                        moveToPositionWithHold(tCx, tCy, hold);
                        //detect if autopilot kicked off
                        if (autopilot == Autopilot.NONE) {
                            /*
                             * moveToPosition() detects when the ship has stopped
                             * moving and corrects itself by turning off the autopilot.
                             * 
                             * Since we aren't here yet, we need to re-issue the command
                             * to fine tune our approach to the target.
                             */
                            cmdFlyToCelestial(flyToTarget, autopilotRange);
                        } else {
                            //do nothing, we are still on autopilot
                        }
                    }
                } else if (flyToTarget.getCurrentSystem() == null) {
                    try {
                        throw new Exception(flyToTarget.getName() + " has null solar system");
                    } catch (Exception ex) {
                        Logger.getLogger(Ship.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    //abort follow
                    cmdAllStop();
                }
            } else {
                autopilot = Autopilot.NONE;
            }
        }
    }

    protected void autopilotUndockingBlock() {
        if (null != getAutopilot()) {
            switch (getAutopilot()) {
                case UNDOCK_STAGE1: {
                    /*
                * get out of the docking port.
                     */
                    double lx = x - port.getAlignX();
                    double ly = y - port.getAlignY();
                    //setup nav parameters
                    double dist;
                    double desired;
                    double speed = magnitude(vx, vy);
                    double hold = accel;
                    //pick the right axis and calculate angle
                    if (lx > ly) {
                        dist = magnitude((lx), 0);
                        desired = FastMath.atan2(0, lx);
                    } else {
                        dist = magnitude(0, (ly));
                        desired = FastMath.atan2(ly, 0);
                    }       //turn towards desired angle
                    desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                    if (Math.abs(theta - desired) > turning * tpf) {
                        if (theta - desired > 0) {
                            rotateMinus();
                        } else if (theta - desired < 0) {
                            rotatePlus();
                        }
                    } else {
                        theta = desired;
                        if (dist < hold) {
                            decelerate();
                            if (speed == 0) {
                                setAutopilot(Autopilot.UNDOCK_STAGE2);
                            }
                        } else if (speed > hold) {
                            //we're getting further from the goal, slow down
                            decelerate();
                        } else if (speed <= hold) {
                            fireRearThrusters(); //accelerate
                        }
                    }
                    break;
                }
                case UNDOCK_STAGE2: {
                    /*
                * align down the line so that we can thrust out of the dock
                     */
                    double ax = x - port.getAlignX();
                    double ay = y - port.getAlignY();
                    //
                    double desired = FastMath.atan2(ay, ax);
                    desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                    if (Math.abs(theta - desired) > turning * tpf) {
                        if (theta - desired > 0) {
                            rotateMinus();
                        } else if (theta - desired < 0) {
                            rotatePlus();
                        }
                    } else {
                        theta = desired;
                        autopilot = Autopilot.UNDOCK_STAGE3;
                    }
                    break;
                }
                case UNDOCK_STAGE3: {
                    double ax = x - port.getAlignX();
                    double ay = y - port.getAlignY();
                    double dist = magnitude((ax), (ay));
                    double speed = magnitude(vx, vy);
                    fireRearThrusters();
                    if (dist > 1000 || (speed > dist)) {
                        autopilot = Autopilot.NONE;
                        port = null;
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    protected void autopilotWaitBlock() {
        if (autopilot == Autopilot.WAIT) {
            waitTimer += tpf;
            if (waitTimer >= waitTimerLength) {
                autopilot = Autopilot.WAITED;
            }
        }
    }

    protected void autopilotAllStopBlock() {
        /*
         * Decelerate until |v| == 0
         */
        if (getAutopilot() == Autopilot.ALL_STOP) {
            double v = magnitude(vx, vy);
            if (v > 0) {
                decelerate();
            } else {
                autopilot = Autopilot.NONE;
            }
        }
    }

    protected void autopilotFightingBlock() {
        /*
         * Fights whatever is currently targeted.
         */
        if (getAutopilot() == Autopilot.ATTACK_TARGET) {
            if (target != null) {
                if (target.getCurrentSystem() == currentSystem) {
                    if (target.getState() == State.ALIVE) {
                        fightTarget();
                    } else {
                        target = null;
                        autopilot = Autopilot.NONE;
                    }
                } else {
                    target = null;
                    autopilot = Autopilot.NONE;
                }
            } else {
                target = null;
                autopilot = Autopilot.NONE;
            }
        }
    }

    protected void autopilotDockingBlock() {
        if (null != getAutopilot()) {
            switch (getAutopilot()) {
                case DOCK_STAGE1:
                    /*
                * The goal of stage 1 is to get permission to dock, get a docking port, and to get to the
                * location of the docking align for that port, and then stop the ship.
                     */
                    //make sure we have a flyToTarget
                    if (flyToTarget != null) {
                        //make sure it is a station
                        if (flyToTarget instanceof Station && flyToTarget.getState() == State.ALIVE) {
                            //make sure we can actually dock there
                            Station tmp = (Station) flyToTarget;
                            if (tmp.canDock(this) && tmp.getCurrentSystem() == currentSystem) {
                                if (port == null) {
                                    //get the docking port to use
                                    port = tmp.requestDockPort(this);
                                } else {
                                    //get the docking align
                                    double ax = x - port.getAlignX();
                                    double ay = y - port.getAlignY();
                                    double dist = magnitude((ax), (ay));
                                    double speed = magnitude(vx, vy);
                                    double hold = 0;
                                    //calculate hold
                                    if (dist > 500) {
                                        hold = accel * 3;
                                    } else {
                                        hold = width / 2;
                                    }
                                    //
                                    double desired = FastMath.atan2(ay, ax);
                                    desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                                    if (Math.abs(theta - desired) > turning * tpf) {
                                        if (theta - desired > 0) {
                                            rotateMinus();
                                        } else if (theta - desired < 0) {
                                            rotatePlus();
                                        }
                                    } else if (dist < hold) {
                                        if (speed == 0 && dist < width) {
                                            setAutopilot(Autopilot.DOCK_STAGE2);
                                        } else {
                                            decelerate();
                                        }
                                    } else {
                                        boolean canAccel = true;
                                        //this is damage control - it deals with bad initial velocities and out of control spirals
                                        //check x axis
                                        double dPx = 0;
                                        double d1x = magnitude(ax, 0);
                                        double d2x = magnitude((x + vx) - (port.getAlignX() + flyToTarget.getVx()), 0);
                                        dPx = d2x - d1x;
                                        if (dPx > 0) {
                                            //we're getting further from the goal, slow down
                                            decelX();
                                            canAccel = false;
                                        }
                                        //check y axis
                                        double dPy = 0;
                                        double d1y = magnitude(0, ay);
                                        double d2y = magnitude(0, (y + vy) - (port.getAlignY() + flyToTarget.getVy()));
                                        dPy = d2y - d1y;
                                        if (dPy > 0) {
                                            //we're getting further from the goal, slow down
                                            decelY();
                                            canAccel = false;
                                        }
                                        //accel if needed
                                        if (canAccel && speed < hold) {
                                            fireRearThrusters();
                                        }
                                    }
                                }
                            } else {
                                cmdAbortDock();
                            }
                        } else {
                            cmdAbortDock();
                        }
                    } else {
                        cmdAbortDock();
                    }
                    break;
                case DOCK_STAGE2: {
                    /*
                * The goal of this docking stage is to align towards the docking port, but so that
                * only one axis is used in the alignment. Thrust is then applied and the ship moves
                * along a single axis until it is near the port on that axis. It then transfers
                * control to stage 3.
                     */
                    //determine which axis is further away
                    double lx = x - port.getPortX();
                    double ly = y - port.getPortY();
                    //setup nav parameters
                    double dist;
                    double desired;
                    double speed = magnitude(vx, vy);
                    double hold = accel * 2;
                    //pick the right axis and calculate angle
                    if (lx > ly) {
                        dist = magnitude((lx), 0);
                        desired = FastMath.atan2(0, lx);
                    } else {
                        dist = magnitude(0, (ly));
                        desired = FastMath.atan2(ly, 0);
                    }       //turn towards desired angle
                    desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                    if (Math.abs(theta - desired) > turning * tpf) {
                        if (theta - desired > 0) {
                            rotateMinus();
                        } else if (theta - desired < 0) {
                            rotatePlus();
                        }
                    } else {
                        theta = desired;
                        if (dist < hold) {
                            decelerate();
                            if (speed == 0) {
                                setAutopilot(Autopilot.DOCK_STAGE3);
                            }
                        } else if (speed > hold) {
                            //we're getting further from the goal, slow down
                            decelerate();
                        } else if (speed <= hold) {
                            fireRearThrusters(); //accelerate
                        }
                    }
                    break;
                }
                case DOCK_STAGE3: {
                    double lx = x - port.getPortX();
                    double ly = y - port.getPortY();
                    //setup nav parameters
                    double desired;
                    //pick the right axis and calculate angle
                    desired = FastMath.atan2(ly, lx);
                    //turn towards desired angle
                    desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                    if (Math.abs(theta - desired) > turning * tpf) {
                        if (theta - desired > 0) {
                            rotateMinus();
                        } else if (theta - desired < 0) {
                            rotatePlus();
                        }
                    } else {
                        theta = desired;
                        if (docked) {
                            setAutopilot(Autopilot.NONE);
                        } else {
                            fireRearThrusters(); //accelerate
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    /*
     * Behaviors
     */
    protected void behave() {
        /*
         * Behaviors are basically the role this NPC plays in the universe. This
         * is what makes it into a trader, a fighter, etc.
         */
        //shield percent
        double shieldPercent = 100 * (shield / maxShield);
        if (null != getBehavior()) //behavior blocks
        {
            switch (getBehavior()) {
                //do nothing
                case NONE:
                    break;
                case TEST:
                    behaviorTest();
                    break;
                case PATROL:
                    behaviorPatrol();
                    break;
                case SECTOR_TRADE:
                    //give this thing a chance of fighting back against hostiles
                    if (shieldPercent > 75) {
                        behaviorSectorTrade();
                    } else {
                        behaviorPatrol();
                    }
                    break;
                case UNIVERSE_TRADE:
                    //give this thing a chance of fighting back against hostiles
                    if (shieldPercent > 75) {
                        behaviorUniverseTrade();
                    } else if (shieldPercent > 40) {
                        behaviorPatrol();
                    } else {
                        tryJumpRetreat();
                    }
                    break;
                case SUPPLY_HOMEBASE:
                    if (shieldPercent > 75) {
                        behaviorSupplyHomeBase();
                    } else if (shieldPercent > 40) {
                        behaviorPatrol();
                    } else {
                        tryJumpRetreat();
                    }
                    break;
                case REPRESENT_HOMEBASE:
                    if (shieldPercent > 75) {
                        behaviorRepresentHomeBase();
                    } else if (shieldPercent > 40) {
                        behaviorPatrol();
                    } else {
                        tryJumpRetreat();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void tryJumpRetreat() {
        /*
         * Attempts to retreat using the jump drive. If there is nowhere to
         * retreat to, it will continue to fight.
         */
        //get a list of systems in jump range
        ArrayList<SolarSystem> zone = new ArrayList<>();
        for (int a = 0; a < getUniverse().getSystems().size(); a++) {
            if (canJump(getUniverse().getSystems().get(a))) {
                zone.add(getUniverse().getSystems().get(a));
            }
        }
        if (zone.size() > 0 && target != null) {
            //abort trade
            abortTrade();
            //jump
            cmdJump(zone.get(rnd.nextInt(zone.size())));
            //notify
            System.out.println(getName() + " escaped to " + currentSystem.getName());
        } else {
            //keep fighting
            behaviorPatrol();
        }
    }

    protected void behaviorUniverseTrade() {
        /*
         * Buy low sell high within systems within jump range of each other.
         */
        if (!docked) {
            if (autopilot == Autopilot.NONE && (fuel / maxFuel) > TRADER_REFUEL_PERCENT) {
                /*
                 * 1. Get a list of friendly stations to collate wares from
                 * 2. Build a list of all wares that can be traded in jumpable sectors
                 * 3. Find the one with the highest profit.
                 * 4. Fill up on the ware.
                 * 5. Drop off the ware.
                 * repeat
                 */
                if (getNumInCargoBay(workingWare) > 0) {
                    /*
                     * There are wares to be sold, this is stage 2.
                     */
                    if (canJump(sellToStation.getCurrentSystem())) {
                        if (sellToStation.getCurrentSystem() != currentSystem) {
                            cmdJump(sellToStation.getCurrentSystem());
                        }
                        cmdDock(sellToStation);
                    } else {
                        abortTrade();
                        leaveSystem();
                    }
                } else {
                    /*
                     * This is stage 1, find the best deal.
                     */
                    //get a list of friendly stations
                    ArrayList<Station> friendly = new ArrayList<>();
                    ArrayList<SolarSystem> zone = new ArrayList<>();
                    for (int a = 0; a < getUniverse().getSystems().size(); a++) {
                        if (canJump(getUniverse().getSystems().get(a))) {
                            ArrayList<Station> tmp = getFriendlyStationsInSystem(getUniverse().getSystems().get(a));
                            zone.add(getUniverse().getSystems().get(a));
                            friendly.addAll(tmp);
                        }
                    }
                    if (friendly.size() > 1) {
                        //build a list of wares that are being produced
                        ArrayList<String> produced = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationSelling();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName();
                                if (!produced.contains(ware)) {
                                    produced.add(ware);
                                }
                            }
                        }
                        //build a list of wares that are being consumed
                        ArrayList<String> consumed = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationBuying();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName();
                                if (!consumed.contains(ware)) {
                                    consumed.add(ware);
                                }
                            }
                        }
                        //cross reference the lists to find what's the same in both
                        ArrayList<String> sample = new ArrayList<>();
                        for (int a = 0; a < consumed.size(); a++) {
                            for (int b = 0; b < produced.size(); b++) {
                                if (consumed.get(a).equals(produced.get(b))) {
                                    sample.add(consumed.get(a));
                                    break;
                                }
                            }
                        }
                        //make sure there's a sample
                        if (sample.size() > 0) {
                            Station buyLoc = null;
                            Station sellLoc = null;
                            Item bestWare = null;
                            double gain = 0;
                            for (int a = 0; a < sample.size(); a++) {
                                Item ware = new Item(sample.get(a));
                                //get the best stations
                                Station pickUp = getBestPickup(zone, ware);
                                Station dropOff = getBestDropOff(zone, ware);
                                //get prices
                                if (pickUp != null && dropOff != null) {
                                    int pickUpPrice = pickUp.getPrice(ware);
                                    int dropOffPrice = dropOff.getPrice(ware);
                                    //find profit
                                    int profit = dropOffPrice - pickUpPrice;
                                    if (pickUpPrice != -1 && dropOffPrice != -1) {
                                        if (profit > 0) {
                                            if (profit > gain) {
                                                buyLoc = pickUp;
                                                sellLoc = dropOff;
                                                bestWare = ware;
                                                //store prices
                                                gain = profit;
                                                buyFromPrice = pickUpPrice;
                                                sellToPrice = dropOffPrice;
                                            }
                                        } else {
                                            //no point in trading this
                                        }
                                    }
                                } else {
                                    //something went wrong
                                }
                            }
                            if (bestWare != null) {
                                //store start and end
                                buyFromStation = buyLoc;
                                sellToStation = sellLoc;
                                workingWare = bestWare;
                                //start trading
                                if (canJump(buyFromStation.getCurrentSystem())) {
                                    if (buyFromStation.getCurrentSystem() != currentSystem) {
                                        cmdJump(buyFromStation.getCurrentSystem());
                                    }
                                    cmdDock(buyFromStation);
                                } else {
                                    abortTrade();
                                    leaveSystem();
                                }
                            } else {
                                /*
                                 * Universe traders roam the universe
                                 */
                                leaveSystem();
                            }
                        } else {
                            //maybe profit awaits us elsewhere
                            leaveSystem();
                        }
                    } else {
                        //profit definately awaits us elsewhere
                        leaveSystem();
                    }
                }
            } else if (autopilot == Autopilot.NONE && (fuel / maxFuel) <= TRADER_REFUEL_PERCENT) {
                //dock at the nearest friendly station
                Station near = getNearestFriendlyStationInSystem();
                if (near != null) {
                    cmdDock(near);
                    System.out.println(getName() + " [UT] is low on fuel and docking at "
                            + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                } else {
                    leaveSystem();
                }
            } else {
                //wait;
            }
        } else //setup wait
        if (autopilot == Autopilot.NONE && port != null) {
            //restore fuel
            fuel = maxFuel;
            //do buying and selling
            Station curr = port.getParent();
            if (curr == buyFromStation) {
                //make sure the price is still ok
                if ((curr.getPrice(workingWare) <= buyFromPrice)
                        && (sellToStation.getPrice(workingWare) >= sellToPrice)
                        && canJump(sellToStation.getCurrentSystem())) {
                    //how much of the ware can we carry
                    int maxQ = (int) (cargo - getBayUsed()) / Math.max(1, (int) workingWare.getVolume());
                    //how much can we carry if we want to follow reserve rules
                    int q = (int) ((1 - TRADER_RESERVE_PERCENT) * maxQ);
                    //buy as much as we can carry
                    curr.buy(this, workingWare, q);
                    System.out.println(getName() + " bought " + getNumInCargoBay(workingWare)
                            + " " + workingWare.getName() + " from " + curr.getName());
                } else {
                    //abort trading operation
                    abortTrade();
                    System.out.println(getName() + " aborted trading operation.");
                }
                //wait
                double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                double delt = rnd.nextDouble() * diff;
                cmdWait(MIN_WAIT_TIME + delt);
            } else if (curr == sellToStation) {
                if (curr.getPrice(workingWare) >= sellToPrice) {
                    //try to dump all our wares at this price
                    int q = getNumInCargoBay(workingWare);
                    curr.sell(this, workingWare, q);
                    System.out.println(getName() + " sold " + (q - getNumInCargoBay(workingWare))
                            + " " + workingWare.getName() + " to " + curr.getName());
                } else {
                    //System.out.println(getName() + " did not sell (Bad sell price)");
                }
                //wait
                if (getNumInCargoBay(workingWare) == 0) {
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                } else {
                    //not everything sold yet
                }
            } else {
                //wait
                double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                double delt = rnd.nextDouble() * diff;
                cmdWait(MIN_WAIT_TIME + delt);
            }
        } //finally undock when waiting is over
        else if (autopilot == Autopilot.WAITED) {
            if (getNumInCargoBay(workingWare) > 0) {
                cmdUndock();
                if (currentSystem != sellToStation.getCurrentSystem()) {
                    /*
                         * Undocking might use too much fuel to reach our destination
                         * causing a failed trade run. Just jump right out of the gate.
                     */
                    autopilot = Autopilot.NONE;
                    port = null;
                }
            } else {
                cmdUndock();
            }
        } else if (port == null) {
            abortTrade();
            cmdUndock();
        } else {
            //do nothing
        }
    }

    protected void behaviorSectorTrade() {
        /*
         * Buy low sell high within one solar system.
         */
        if (!docked) {
            if (autopilot == Autopilot.NONE && (fuel / maxFuel) > TRADER_REFUEL_PERCENT) {
                /*
                 * 1. Get a list of friendly stations to collate wares from
                 * 2. Build a list of all wares that can be traded in the
                 * sector (a ware must have both a buyer and a seller)
                 * 3. Find the one with the highest profit.
                 * 4. Fill up on the ware.
                 * 5. Drop off the ware.
                 * repeat
                 */
                if (getNumInCargoBay(workingWare) > 0) {
                    /*
                     * There are wares to be sold, this is stage 2.
                     */
                    cmdDock(sellToStation);
                } else {
                    /*
                     * This is stage 1, find the best deal.
                     */
                    //get a list of friendly stations
                    ArrayList<Station> friendly = getFriendlyStationsInSystem();
                    if (friendly.size() > 1) {
                        //build a list of wares that are being produced
                        ArrayList<String> produced = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationSelling();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName();
                                if (!produced.contains(ware)) {
                                    produced.add(ware);
                                }
                            }
                        }
                        //build a list of wares that are being consumed
                        ArrayList<String> consumed = new ArrayList<>();
                        for (int a = 0; a < friendly.size(); a++) {
                            ArrayList<Item> made = friendly.get(a).getStationBuying();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName();
                                if (!consumed.contains(ware)) {
                                    consumed.add(ware);
                                }
                            }
                        }
                        //cross reference the lists to find what's the same in both
                        ArrayList<String> sample = new ArrayList<>();
                        for (int a = 0; a < consumed.size(); a++) {
                            for (int b = 0; b < produced.size(); b++) {
                                if (consumed.get(a).equals(produced.get(b))) {
                                    sample.add(consumed.get(a));
                                    break;
                                }
                            }
                        }
                        //make sure there's a sample
                        if (sample.size() > 0) {
                            Station buyLoc = null;
                            Station sellLoc = null;
                            Item bestWare = null;
                            double gain = 0;
                            for (int a = 0; a < sample.size(); a++) {
                                Item ware = new Item(sample.get(a));
                                //get the best stations
                                ArrayList<SolarSystem> curr = new ArrayList<>();
                                curr.add(currentSystem);
                                Station pickUp = getBestPickup(curr, ware);
                                Station dropOff = getBestDropOff(curr, ware);
                                //get prices
                                if (pickUp != null && dropOff != null) {
                                    int pickUpPrice = pickUp.getPrice(ware);
                                    int dropOffPrice = dropOff.getPrice(ware);
                                    //find profit
                                    int profit = dropOffPrice - pickUpPrice;
                                    if (pickUpPrice != -1 && dropOffPrice != -1) {
                                        if (profit > 0) {
                                            if (profit > gain) {
                                                buyLoc = pickUp;
                                                sellLoc = dropOff;
                                                bestWare = ware;
                                                //store prices
                                                gain = profit;
                                                buyFromPrice = pickUpPrice;
                                                sellToPrice = dropOffPrice;
                                            }
                                        } else {
                                            //no point in trading this
                                        }
                                    }
                                } else {
                                    //something went wrong
                                }
                            }
                            if (bestWare != null) {
                                //store start and end
                                buyFromStation = buyLoc;
                                sellToStation = sellLoc;
                                workingWare = bestWare;
                                //start trading
                                cmdDock(buyFromStation);
                            } else if (faction.equals(PLAYER_FACTION)) {
                                dockAtFriendlyStationInSystem();
                            } else {
                                /*
                                     * I honestly don't give a damn if some random NPC trader dies.
                                     * It probably keeps the universe more interesting.
                                 */
                                leaveSystem();
                            }
                        } else {
                            //maybe profit awaits us elsewhere
                            leaveSystem();
                        }
                    } else {
                        //profit definately awaits us elsewhere
                        leaveSystem();
                    }
                }
            } else if (autopilot == Autopilot.NONE && (fuel / maxFuel) <= TRADER_REFUEL_PERCENT) {
                //dock at the nearest friendly station
                Station near = getNearestFriendlyStationInSystem();
                if (near != null) {
                    cmdDock(near);
                    System.out.println(getName() + " [ST] is low on fuel and docking at "
                            + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                } else {
                    leaveSystem();
                }
            } else {
                //wait;
            }
        } else if (autopilot == Autopilot.NONE && port != null) {
            //restore fuel
            fuel = maxFuel;
            //do buying and selling
            Station curr = port.getParent();
            if (curr == buyFromStation) {
                //make sure the price is still ok
                if ((curr.getPrice(workingWare) <= buyFromPrice) && (sellToStation.getPrice(workingWare) >= sellToPrice)) {
                    //how much of the ware can we carry
                    int maxQ = (int) (cargo - getBayUsed()) / Math.max(1, (int) workingWare.getVolume());
                    //how much can we carry if we want to follow reserve rules
                    int q = (int) ((1 - TRADER_RESERVE_PERCENT) * maxQ);
                    //buy as much as we can carry
                    curr.buy(this, workingWare, q);
                    System.out.println(getName() + " bought " + getNumInCargoBay(workingWare)
                            + " " + workingWare.getName() + " from " + curr.getName());
                } else {
                    //abort trading operation
                    abortTrade();
                    System.out.println(getName() + " aborted trading operation (Bad buy price)");
                }
                //wait
                double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                double delt = rnd.nextDouble() * diff;
                cmdWait(MIN_WAIT_TIME + delt);
            } else if (curr == sellToStation) {
                if (curr.getPrice(workingWare) >= sellToPrice) {
                    //try to dump all our wares at this price
                    int q = getNumInCargoBay(workingWare);
                    curr.sell(this, workingWare, q);
                    System.out.println(getName() + " sold " + (q - getNumInCargoBay(workingWare))
                            + " " + workingWare.getName() + " to " + curr.getName());
                } else {
                    //System.out.println(getName() + " did not sell (Bad sell price)");
                }
                //wait
                if (getNumInCargoBay(workingWare) == 0) {
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                } else {
                    //not everything sold yet
                }
            } else {
                //wait
                double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                double delt = rnd.nextDouble() * diff;
                cmdWait(MIN_WAIT_TIME + delt);
            }
        } else if (autopilot == Autopilot.WAITED) {
            //finally undock
            cmdUndock();
        } else if (port == null) {
            abortTrade();
            cmdUndock();
        } else {

        }
    }

    protected void behaviorRepresentHomeBase() {
        /*
         * Sells the products of the homebase for the best price.
         */
        if (homeBase != null) {
            if (!docked) {
                if (autopilot == Autopilot.NONE && (fuel / maxFuel) > TRADER_REFUEL_PERCENT) {
                    if (getNumInCargoBay(workingWare) > 0) {
                        /*
                         * There are wares to be sold, this is stage 2.
                         */
                        if (canJump(sellToStation.getCurrentSystem())) {
                            if (sellToStation.getCurrentSystem() != currentSystem) {
                                cmdJump(sellToStation.getCurrentSystem());
                            }
                            cmdDock(sellToStation);
                        } else {
                            abortTrade();
                            leaveSystem();
                        }
                    } else {
                        /*
                         * This is stage 1, find the best deal.
                         */
                        //get a list of friendly stations
                        ArrayList<Station> friendly = new ArrayList<>();
                        ArrayList<SolarSystem> zone = new ArrayList<>();
                        for (int a = 0; a < getUniverse().getSystems().size(); a++) {
                            if (canJump(getUniverse().getSystems().get(a))) {
                                ArrayList<Station> tmp = getFriendlyStationsInSystem(getUniverse().getSystems().get(a));
                                zone.add(getUniverse().getSystems().get(a));
                                friendly.addAll(tmp);
                            }
                        }
                        if (friendly.size() > 1) {
                            //we know what is being produced, it is homebase products
                            ArrayList<String> produced = new ArrayList<>();
                            {
                                ArrayList<Item> made = homeBase.getStationSelling();
                                for (int b = 0; b < made.size(); b++) {
                                    String ware = made.get(b).getName();
                                    if (!produced.contains(ware)) {
                                        produced.add(ware);
                                    }
                                }
                            }
                            //build a list of wares that are being consumed
                            ArrayList<String> consumed = new ArrayList<>();
                            for (int a = 0; a < friendly.size(); a++) {
                                ArrayList<Item> made = friendly.get(a).getStationBuying();
                                for (int b = 0; b < made.size(); b++) {
                                    String ware = made.get(b).getName();
                                    if (!consumed.contains(ware)) {
                                        consumed.add(ware);
                                    }
                                }
                            }
                            //cross reference the lists to find what's the same in both
                            ArrayList<String> sample = new ArrayList<>();
                            for (int a = 0; a < consumed.size(); a++) {
                                for (int b = 0; b < produced.size(); b++) {
                                    if (consumed.get(a).equals(produced.get(b))) {
                                        sample.add(consumed.get(a));
                                        break;
                                    }
                                }
                            }
                            //make sure there's a sample
                            if (sample.size() > 0) {
                                Station buyLoc = null;
                                Station sellLoc = null;
                                Item bestWare = null;
                                double gain = 0;
                                for (int a = 0; a < sample.size(); a++) {
                                    Item ware = new Item(sample.get(a));
                                    //always pick up from homebase
                                    Station pickUp = homeBase;
                                    //get best sell station
                                    Station dropOff = getBestDropOff(zone, ware);
                                    //get prices
                                    if (pickUp != null && dropOff != null) {
                                        int pickUpPrice = pickUp.getPrice(ware);
                                        int dropOffPrice = dropOff.getPrice(ware);
                                        //find profit
                                        int profit = dropOffPrice - pickUpPrice;
                                        if (pickUpPrice != -1 && dropOffPrice != -1) {
                                            if (profit > 0) {
                                                if (profit > gain) {
                                                    buyLoc = pickUp;
                                                    sellLoc = dropOff;
                                                    bestWare = ware;
                                                    //store prices
                                                    gain = profit;
                                                    buyFromPrice = pickUpPrice;
                                                    sellToPrice = dropOffPrice;
                                                }
                                            } else {
                                                //no point in trading this
                                            }
                                        }
                                    } else {
                                        //something went wrong
                                    }
                                }
                                if (bestWare != null) {
                                    //store start and end
                                    buyFromStation = buyLoc;
                                    sellToStation = sellLoc;
                                    workingWare = bestWare;
                                    //start trading
                                    if (canJump(buyFromStation.getCurrentSystem())) {
                                        if (buyFromStation.getCurrentSystem() != currentSystem) {
                                            cmdJump(buyFromStation.getCurrentSystem());
                                        }
                                        cmdDock(buyFromStation);
                                    } else {
                                        abortTrade();
                                    }
                                } else {
                                    dockAtFriendlyStationInSystem();
                                }
                            } else {
                                dockAtFriendlyStationInSystem();
                            }
                        } else {
                            dockAtFriendlyStationInSystem();
                        }
                    }
                } else if (autopilot == Autopilot.NONE && (fuel / maxFuel) <= TRADER_REFUEL_PERCENT) {
                    //dock at the nearest friendly station
                    Station near = getNearestFriendlyStationInSystem();
                    if (near != null) {
                        cmdDock(near);
                        System.out.println(getName() + " [HR] is low on fuel and docking at "
                                + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                    } else {
                        leaveSystem();
                    }
                } else {
                    //wait;
                }
            } else //setup wait
            if (autopilot == Autopilot.NONE && port != null) {
                //restore fuel
                fuel = maxFuel;
                //do buying and selling
                Station curr = port.getParent();
                if (curr == buyFromStation) {
                    //make sure the price is still ok
                    if ((curr.getPrice(workingWare) <= buyFromPrice)
                            && (sellToStation.getPrice(workingWare) >= sellToPrice)
                            && canJump(sellToStation.getCurrentSystem())) {
                        //how much of the ware can we carry
                        int maxQ = (int) (cargo - getBayUsed()) / Math.max(1, (int) workingWare.getVolume());
                        //how much can we carry if we want to follow reserve rules
                        int q = (int) ((1 - TRADER_RESERVE_PERCENT) * maxQ);
                        //buy as much as we can carry
                        curr.buy(this, workingWare, q);
                        System.out.println(getName() + " bought " + getNumInCargoBay(workingWare)
                                + " " + workingWare.getName() + " from " + curr.getName());
                    } else {
                        //abort trading operation
                        abortTrade();
                        System.out.println(getName() + " aborted trading operation.");
                    }
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                } else if (curr == sellToStation) {
                    if (curr.getPrice(workingWare) >= sellToPrice) {
                        //try to dump all our wares at this price
                        int q = getNumInCargoBay(workingWare);
                        curr.sell(this, workingWare, q);
                        System.out.println(getName() + " sold " + (q - getNumInCargoBay(workingWare))
                                + " " + workingWare.getName() + " to " + curr.getName());
                    } else {
                        //System.out.println(getName() + " did not sell (Bad sell price)");
                    }
                    //wait
                    if (getNumInCargoBay(workingWare) == 0) {
                        double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                        double delt = rnd.nextDouble() * diff;
                        cmdWait(MIN_WAIT_TIME + delt);
                    } else {
                        //not everything sold yet
                    }
                } else {
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                }
            } //finally undock when waiting is over
            else if (autopilot == Autopilot.WAITED) {
                if (getNumInCargoBay(workingWare) > 0) {
                    cmdUndock();
                    if (currentSystem != sellToStation.getCurrentSystem()) {
                        /*
                             * Undocking might use too much fuel to reach our destination
                             * causing a failed trade run. Just jump right out of the gate.
                         */
                        autopilot = Autopilot.NONE;
                        port = null;
                    }
                } else {
                    cmdUndock();
                }
            } else if (port == null) {
                abortTrade();
                cmdUndock();
            } else {
                //do nothing
            }
        } else {
            setBehavior(Behavior.NONE);
        }
    }

    protected void behaviorSupplyHomeBase() {
        /*
         * Supplies the selected home base with wares it is low on. Basically, it goes out and buys
         * the ware the station is lowest on.
         */
        if (homeBase != null) {
            if (!docked) {
                if (autopilot == Autopilot.NONE && (fuel / maxFuel) > TRADER_REFUEL_PERCENT) {
                    if (getNumInCargoBay(workingWare) > 0) {
                        /*
                         * There are wares to be sold, this is stage 2.
                         */
                        if (canJump(sellToStation.getCurrentSystem())) {
                            if (sellToStation.getCurrentSystem() != currentSystem) {
                                cmdJump(sellToStation.getCurrentSystem());
                            }
                            cmdDock(sellToStation);
                        } else {
                            abortTrade();
                        }
                    } else {
                        /*
                         * This is stage 1, find the best deal.
                         */
                        //get a list of friendly stations
                        ArrayList<Station> friendly = new ArrayList<>();
                        ArrayList<SolarSystem> zone = new ArrayList<>();
                        for (int a = 0; a < getUniverse().getSystems().size(); a++) {
                            if (canJump(getUniverse().getSystems().get(a))) {
                                ArrayList<Station> tmp = getFriendlyStationsInSystem(getUniverse().getSystems().get(a));
                                zone.add(getUniverse().getSystems().get(a));
                                friendly.addAll(tmp);
                            }
                        }
                        if (friendly.size() > 1) {
                            //build a list of wares that are being produced
                            ArrayList<String> produced = new ArrayList<>();
                            for (int a = 0; a < friendly.size(); a++) {
                                ArrayList<Item> made = friendly.get(a).getStationSelling();
                                for (int b = 0; b < made.size(); b++) {
                                    String ware = made.get(b).getName();
                                    if (!produced.contains(ware)) {
                                        produced.add(ware);
                                    }
                                }
                            }
                            //we know what is being consumed, since it's resources homebase needs
                            ArrayList<String> consumed = new ArrayList<>();
                            ArrayList<Item> made = homeBase.getStationBuying();
                            for (int b = 0; b < made.size(); b++) {
                                String ware = made.get(b).getName();
                                if (!consumed.contains(ware)) {
                                    consumed.add(ware);
                                }
                            }
                            //cross reference the lists to find what's the same in both
                            ArrayList<String> sample = new ArrayList<>();
                            for (int a = 0; a < consumed.size(); a++) {
                                for (int b = 0; b < produced.size(); b++) {
                                    if (consumed.get(a).equals(produced.get(b))) {
                                        sample.add(consumed.get(a));
                                        break;
                                    }
                                }
                            }
                            //make sure there's a sample
                            if (sample.size() > 0) {
                                Station buyLoc = null;
                                Station sellLoc = null;
                                Item bestWare = null;
                                double gain = 0;
                                for (int a = 0; a < sample.size(); a++) {
                                    Item ware = new Item(sample.get(a));
                                    //get the best stations
                                    Station pickUp = getBestPickup(zone, ware);
                                    //we always drop off at the home base
                                    Station dropOff = homeBase;
                                    //get prices
                                    if (pickUp != null && dropOff != null) {
                                        int pickUpPrice = pickUp.getPrice(ware);
                                        int dropOffPrice = dropOff.getPrice(ware);
                                        //find profit
                                        int profit = dropOffPrice - pickUpPrice;
                                        if (pickUpPrice != -1 && dropOffPrice != -1) {
                                            if (profit > 0) {
                                                if (profit > gain) {
                                                    buyLoc = pickUp;
                                                    sellLoc = dropOff;
                                                    bestWare = ware;
                                                    //store prices
                                                    gain = profit;
                                                    buyFromPrice = pickUpPrice;
                                                    sellToPrice = dropOffPrice;
                                                }
                                            } else {
                                                //no point in trading this
                                            }
                                        }
                                    } else {
                                        //something went wrong
                                    }
                                }
                                if (bestWare != null) {
                                    //store start and end
                                    buyFromStation = buyLoc;
                                    sellToStation = sellLoc;
                                    workingWare = bestWare;
                                    //start trading
                                    if (canJump(buyFromStation.getCurrentSystem())) {
                                        if (buyFromStation.getCurrentSystem() != currentSystem) {
                                            cmdJump(buyFromStation.getCurrentSystem());
                                        }
                                        cmdDock(buyFromStation);
                                    } else {
                                        abortTrade();
                                    }
                                } else {
                                    dockAtFriendlyStationInSystem();
                                }
                            } else {
                                dockAtFriendlyStationInSystem();
                            }
                        } else {
                            dockAtFriendlyStationInSystem();
                        }
                    }
                } else if (autopilot == Autopilot.NONE && (fuel / maxFuel) <= TRADER_REFUEL_PERCENT) {
                    //dock at the nearest friendly station
                    Station near = getNearestFriendlyStationInSystem();
                    if (near != null) {
                        cmdDock(near);
                        System.out.println(getName() + " [HS] is low on fuel and docking at "
                                + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                    } else {
                        leaveSystem();
                    }
                } else {
                    //wait;
                }
            } else //setup wait
            if (autopilot == Autopilot.NONE && port != null) {
                //restore fuel
                fuel = maxFuel;
                //do buying and selling
                Station curr = port.getParent();
                if (curr == buyFromStation) {
                    //make sure the price is still ok
                    if ((curr.getPrice(workingWare) <= buyFromPrice)
                            && (sellToStation.getPrice(workingWare) >= sellToPrice)
                            && canJump(sellToStation.getCurrentSystem())) {
                        //how much of the ware can we carry
                        int maxQ = (int) (cargo - getBayUsed()) / Math.max(1, (int) workingWare.getVolume());
                        //how much does the homebase need?
                        int needQ = 0;
                        for (int v = 0; v < homeBase.getStationBuying().size(); v++) {
                            if (homeBase.getStationBuying().get(v).getName().equals(workingWare.getName())) {
                                int have = homeBase.getStationBuying().get(v).getQuantity();
                                int store = homeBase.getStationBuying().get(v).getStore();
                                needQ = store - have;
                            }
                        }
                        //don't get more than the station can use
                        if (maxQ > needQ) {
                            maxQ = needQ;
                        }
                        //how much can we carry if we want to follow reserve rules
                        int q = (int) ((1 - TRADER_RESERVE_PERCENT) * maxQ);
                        //buy as much as we can carry
                        curr.buy(this, workingWare, q);
                        System.out.println(getName() + " bought " + getNumInCargoBay(workingWare)
                                + " " + workingWare.getName() + " from " + curr.getName());
                    } else {
                        //abort trading operation
                        abortTrade();
                        System.out.println(getName() + " aborted trading operation.");
                    }
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                } else if (curr == sellToStation) {
                    if (curr.getPrice(workingWare) >= sellToPrice) {
                        //try to dump all our wares at this price
                        int q = getNumInCargoBay(workingWare);
                        curr.sell(this, workingWare, q);
                        System.out.println(getName() + " sold " + (q - getNumInCargoBay(workingWare))
                                + " " + workingWare.getName() + " to " + curr.getName());
                    } else {
                        //System.out.println(getName() + " did not sell (Bad sell price)");
                    }
                    //wait
                    if (getNumInCargoBay(workingWare) == 0) {
                        double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                        double delt = rnd.nextDouble() * diff;
                        cmdWait(MIN_WAIT_TIME + delt);
                    } else {
                        //not everything sold yet
                    }
                } else {
                    //wait
                    double diff = MAX_WAIT_TIME - MIN_WAIT_TIME;
                    double delt = rnd.nextDouble() * diff;
                    cmdWait(MIN_WAIT_TIME + delt);
                }
            } //finally undock when waiting is over
            else if (autopilot == Autopilot.WAITED) {
                if (getNumInCargoBay(workingWare) > 0) {
                    cmdUndock();
                    if (currentSystem != sellToStation.getCurrentSystem()) {
                        /*
                             * Undocking might use too much fuel to reach our destination
                             * causing a failed trade run. Just jump right out of the gate.
                         */
                        autopilot = Autopilot.NONE;
                        port = null;
                    }
                } else {
                    cmdUndock();
                }
            } else if (port == null) {
                abortTrade();
                cmdUndock();
            } else {
                //do nothing
            }
        } else {
            //exit, no home base
            setBehavior(Behavior.NONE);
        }
    }

    protected void behaviorTest() {
        /*
         * This is a test behavior and should only be used for development and
         * testing purposes. This behavior should never be enabled in an actual
         * release.
         */
        if (!docked) {
            //target nearest ship
            targetNearestShip();
            if (target == null) {
                if (magnitude(vx, vy) > 0) {
                    decelerate();
                }
            } else if (target != null) {
                fightTarget();
            }
        } else {
            //TODO - ADD CODE FOR WHEN DOCKED
        }
    }

    protected void behaviorPatrol() {
        if (!docked) {
            if ((fuel / maxFuel) > PATROL_REFUEL_PERCENT) {
                //target nearest enemy
                targetNearestHostileShip();
                if (target == null) {
                    targetNearestHostileStation();
                }
            }
            //handle what we got
            if (target == null) {
                /*
                 * Resume the patrol. Pick a station to fly within sensor
                 * range of and fly to it. If fuel is less than 50% go dock
                 * so it is replenished.
                 */
                if (autopilot == Autopilot.NONE) {
                    //fuel check
                    if ((fuel / maxFuel) <= PATROL_REFUEL_PERCENT) {
                        //dock at the nearest friendly station
                        Station near = getNearestFriendlyStationInSystem();
                        if (near != null) {
                            cmdDock(near);
                            System.out.println(getName() + " [P] is low on fuel and docking at "
                                    + near.getName() + " (" + (int) (100 * (fuel / maxFuel)) + "%)");
                        } else {
                            leaveSystem();
                        }
                    } else {
                        /*
                         * Get a random celestial or station in system. Stations
                         * are preferred but if there aren't many available then
                         * fly to celestials as well.
                         */
                        double pick = rnd.nextFloat();
                        Celestial near;
                        if (currentSystem.getStationList().size() < 4) {
                            if (pick <= 0.5) {
                                near = getRandomStationInSystem();
                            } else {
                                near = getRandomCelestialInSystem();
                            }
                        } else {
                            near = getRandomStationInSystem();
                        }
                        if (near != null) {
                            //fly within sensor range
                            double range = sensor;
                            cmdFlyToCelestial(near, range);
                        } else {
                            leaveSystem();
                        }
                    }
                } else {
                    //wait
                }
            } else //fight current target
            if ((target.getStandingsToMe(this) < HOSTILE_STANDING) || scanForContraband(target) || target == lastBlow) {
                cmdFightTarget(target);
            }
        } else {
            //restore fuel
            fuel = maxFuel;
            //undock
            cmdUndock();
        }
    }

    /*
     * Utility nav functions
     */
    private void leaveSystem() {
        /*
         * Finds a random jump hole and flies through it.
         */
        Jumphole njmp = getRandomJumpholeInSystem();
        cmdFlyToCelestial(njmp, 0);
    }

    private void abortTrade() {
        //end trade
        autopilot = Autopilot.NONE;
        buyFromStation = null;
        sellToStation = null;
        workingWare = null;
        buyFromPrice = 0;
        sellToPrice = 0;
    }

    protected double getFlightHold() {
        return 3 * accel;
    }

    protected double getFollowHold() {
        return Double.POSITIVE_INFINITY;
    }

    protected void moveToPosition(double tx, double ty) {
        /*
         * Maintains compatibility with most flight methods.
         */
        moveToPositionWithHold(tx, ty, getFlightHold());
    }

    protected void moveToPositionWithHold(double tx, double ty, double hold) {
        //get the destination
        double ax = getCenterX() - tx;
        double ay = getCenterY() - ty;
        double dist = magnitude((ax), (ay));
        double speed = magnitude(vx, vy);
        //
        double desired = FastMath.atan2(ay, ax);
        desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
        if (Math.abs(theta - desired) > turning * tpf) {
            if (theta - desired > 0) {
                rotateMinus();
            } else if (theta - desired < 0) {
                rotatePlus();
            }
        } else if ((dist < hold) && hold != Double.POSITIVE_INFINITY) {
            decelerate();
            if (speed == 0) {
                //disable autopilot destination reached
                autopilot = Autopilot.NONE;
            }
        } else {
            boolean canAccel = true;
            //this is damage control - it deals with bad initial velocities and out of control spirals
            double d2x;
            double d2y;
            d2x = magnitude((getCenterX() + vx) - (tx), 0);
            d2y = magnitude(0, (getCenterY() + vy) - (ty));
            //check x axis
            double dPx;
            double d1x = magnitude(ax, 0);
            dPx = d2x - d1x;
            if (dPx > 0) {
                //we're getting further from the goal, slow down
                decelX();
                canAccel = false;
            }
            //check y axis
            double dPy;
            double d1y = magnitude(0, ay);
            dPy = d2y - d1y;
            if (dPy > 0) {
                //we're getting further from the goal, slow down
                decelY();
                canAccel = false;
            }
            //accel if needed
            if (canAccel && speed < hold) {
                fireRearThrusters();
            }
        }
    }

    public Station getBestDropOff(ArrayList<SolarSystem> systems, Item ware) {
        Station ret;
        {
            Station bStation = null;
            int bPrice = 0;
            for (int b = 0; b < systems.size(); b++) {
                ArrayList<Station> friendly = getFriendlyStationsInSystem(systems.get(b));
                if (friendly.size() > 0) {
                    for (int a = 0; a < friendly.size(); a++) {
                        Station test = friendly.get(a);
                        if (test.buysWare(ware)) {
                            if (bStation == null) {
                                bStation = test;
                                bPrice = test.getPrice(ware);
                            } else {
                                int nP = test.getPrice(ware);
                                if (nP > bPrice) {
                                    bStation = test;
                                }
                            }
                        }
                    }
                }
            }
            ret = bStation;
        }
        return ret;
    }

    public Station getBestPickup(ArrayList<SolarSystem> systems, Item ware) {
        Station ret;
        {
            Station bStation = null;
            int bPrice = 0;
            for (int b = 0; b < systems.size(); b++) {
                ArrayList<Station> friendly = getFriendlyStationsInSystem(systems.get(b));
                if (friendly.size() > 0) {
                    for (int a = 0; a < friendly.size(); a++) {
                        Station test = friendly.get(a);
                        if (test.sellsWare(ware)) {
                            if (bStation == null) {
                                bStation = test;
                                bPrice = test.getPrice(ware);
                            } else {
                                int nP = test.getPrice(ware);
                                if (nP < bPrice) {
                                    bStation = test;
                                }
                            }
                        }
                    }
                }
            }
            ret = bStation;
        }
        return ret;
    }

    public Station getRandomStationInSystem() {
        Station ret;
        {
            ArrayList<Entity> stations = currentSystem.getStationList();
            if (stations.size() > 0) {
                ret = (Station) stations.get(rnd.nextInt(stations.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    public Celestial getRandomCelestialInSystem() {
        Celestial ret;
        {
            ArrayList<Entity> celestials = currentSystem.getCelestialList();
            if (celestials.size() > 0) {
                ret = (Celestial) celestials.get(rnd.nextInt(celestials.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    public Jumphole getRandomJumpholeInSystem() {
        Jumphole ret;
        {
            ArrayList<Entity> jumpHoles = currentSystem.getJumpholeList();
            if (jumpHoles.size() > 0) {
                ret = (Jumphole) jumpHoles.get(rnd.nextInt(jumpHoles.size()));
            } else {
                return null;
            }
        }
        return ret;
    }

    public ArrayList<Station> getFriendlyStationsInSystem() {
        return getFriendlyStationsInSystem(currentSystem);
    }

    public ArrayList<Station> getFriendlyStationsInSystem(SolarSystem system) {
        ArrayList<Station> list = new ArrayList<>();
        {
            ArrayList<Entity> stations = system.getStationList();
            if (stations.size() > 0) {
                for (int a = 0; a < stations.size(); a++) {
                    Station test = (Station) stations.get(a);
                    if (test.canDock(this)) {
                        list.add(test);
                    }
                }
            }
        }
        return list;
    }

    public ArrayList<Ship> getShipsInSensorRange() {
        ArrayList<Ship> ret = new ArrayList<>();
        {
            //get ship list
            ArrayList<Entity> ships = currentSystem.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp != this) {
                    if (tmp.distanceTo(this) < sensor) {
                        ret.add(tmp);
                    }
                }
            }
            //get station list
            ArrayList<Entity> stations = currentSystem.getStationList();
            for (int a = 0; a < stations.size(); a++) {
                Station tmp = (Station) stations.get(a);
                if (tmp.distanceTo(this) < sensor) {
                    ret.add(tmp);
                }
            }
        }
        return ret;
    }

    public Station getNearestFriendlyStationInSystem() {
        Station ret;
        {
            ArrayList<Entity> stations = currentSystem.getStationList();
            if (stations.size() > 0) {
                Station closest = (Station) stations.get(0);
                for (int a = 0; a < stations.size(); a++) {
                    Station test = (Station) stations.get(a);
                    if (test.canDock(this)) {
                        double old = closest.distanceTo(this);
                        double next = test.distanceTo(this);
                        if (next < old) {
                            closest = test;
                        }
                    }
                }
                ret = closest;
            } else {
                return null;
            }
        }
        //final check
        if (ret.canDock(this)) {
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void dying() {
        //end sound effects
        killSounds();
        //drop cargo
        dropLoot();
        //drop explosions
        explode();
        //die
        state = State.DEAD;
    }

    @Override
    public void dead() {
        killSounds();
    }

    /*
     * Navigation signals
     */
    public void requestDocking() {
        /*
         * Gets a docking port
         */
        if (target instanceof Station) {
            Station tmp = (Station) target;
            PortContainer por = tmp.requestDockPort(this);
            if (por != null) {
                port = por;
            }
        }
    }

    public void cmdJump(SolarSystem destination) {
        //make sure we have a jump drive group device
        if (canJump(destination)) {
            //drop jump effect
            dropJumpEffect();
            //determine fuel cost
            double fuelCost = getJumpFuelCost(destination);
            //deduct fuel
            fuel -= fuelCost;
            //move to new system
            currentSystem.pullEntityFromSystem(this);
            destination.putEntityInSystem(this);
            //apply negative effects
            double dmg = rnd.nextFloat() * MAX_JUMP_SHIELD_DAMAGE * maxShield;
            dealDamage(dmg);
            //randomize location
            x = rnd.nextInt(12000 * 2) - 12000;
            y = rnd.nextInt(12000 * 2) - 12000;
            //drop the jump effect
            dropJumpEffect();
        }
    }

    public void cmdWait(double duration) {
        autopilot = Autopilot.WAIT;
        waitTimerLength = duration;
        waitTimer = 0;
    }

    public void cmdAbortDock() {
        if (!docked) {
            setAutopilot(Autopilot.NONE);
            port = null;
        } else {
            //don't null the port
        }
        if (behavior == Behavior.UNIVERSE_TRADE || behavior == Behavior.SECTOR_TRADE) {
            //stop trading
            abortTrade();
        }
        flyToTarget = null;
        setAutopilot(Autopilot.NONE);
    }

    public void cmdAllStop() {
        setAutopilot(Autopilot.ALL_STOP);
    }

    public void cmdUndock() {
        /*
         * Attempt to undock with the current target
         */
        if (port != null) {
            port.setClient(null);
        }
        target = null;
        docked = false;
        autopilot = Autopilot.UNDOCK_STAGE1;
    }

    public void cmdFightTarget(Ship ship) {
        this.target = ship;
        autopilot = Autopilot.ATTACK_TARGET;
    }

    private boolean scanForContraband(Ship ship) {
        /*
         * Only used for detecting contraband being carried by the
         * player.
         */
        if (ship.getFaction().equals(PLAYER_FACTION)) {
            if (scanForContraband) {
                ArrayList<Item> sc = ship.getCargoBay();
                for (int a = 0; a < sc.size(); a++) {
                    if (myFaction.isContraband(sc.get(a).getName())) {
                        //notify the player
                        if (conversation == null) {
                            if (myFaction.getContrabandNotifications().size() > 0) {
                                String pick = myFaction.getContrabandNotifications().
                                        get(rnd.nextInt(myFaction.getContrabandNotifications().size()));
                                conversation = new Conversation(this, "Contraband " + sc.get(a).getName(), pick);
                            }
                        }
                        //return true
                        return true;
                    }
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public void targetNearestShip() {
        //get a list of all nearby ships
        ArrayList<Entity> nearby = getCurrentSystem().getEntities();
        ArrayList<Ship> ships = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            if (nearby.get(a) instanceof Ship) {
                if (!(nearby.get(a) instanceof Projectile)) {
                    if (!(nearby.get(a) instanceof Explosion)) {
                        Ship tmp = (Ship) nearby.get(a);
                        if (tmp != this) {
                            //make sure it is alive
                            if (tmp.getState() == State.ALIVE) {
                                //make sure it is in range
                                if (distanceTo(tmp) < getSensor()) {
                                    ships.add(tmp);
                                }
                            }
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < ships.size(); a++) {
            if (closest == null) {
                closest = ships.get(a);
            } else {
                double distClosest = distanceTo(closest);
                double distTest = distanceTo(ships.get(a));
                if (distTest < distClosest) {
                    closest = ships.get(a);
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestNeutralShip() {
        target = null;
        //get a list of all nearby hostiles
        ArrayList<Entity> nearby = getCurrentSystem().getShipList();
        ArrayList<Ship> neutrals = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Ship tmp = (Ship) nearby.get(a);
            //make sure it is in range
            if (distanceTo(tmp) < getSensor()) {
                if (nearby.get(a) instanceof Ship) {
                    if (!(nearby.get(a) instanceof Projectile)) {
                        if (!(nearby.get(a) instanceof Explosion)) {
                            if (tmp != this) {
                                //make sure it is alive and isn't docked
                                if (tmp.getState() == State.ALIVE && !tmp.isDocked()) {
                                    //check standings
                                    double standing = tmp.getStandingsToMe(this);
                                    if (standing > HOSTILE_STANDING && standing <= 2) {
                                        neutrals.add(tmp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < neutrals.size(); a++) {
            if (closest == null) {
                closest = neutrals.get(a);
            } else {
                double distClosest = distanceTo(closest);
                double distTest = distanceTo(neutrals.get(a));
                if (distTest < distClosest) {
                    closest = neutrals.get(a);
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestFriendlyShip() {
        target = null;
        //get a list of all nearby hostiles
        ArrayList<Entity> nearby = getCurrentSystem().getShipList();
        ArrayList<Ship> friendlies = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Ship tmp = (Ship) nearby.get(a);
            //make sure it is in range
            if (distanceTo(tmp) < getSensor()) {
                if (nearby.get(a) instanceof Ship) {
                    if (!(nearby.get(a) instanceof Projectile)) {
                        if (!(nearby.get(a) instanceof Explosion)) {
                            if (tmp != this) {
                                //make sure it is alive and isn't docked
                                if (tmp.getState() == State.ALIVE && !tmp.isDocked()) {
                                    //check standings
                                    if (tmp.getStandingsToMe(this) > 2) {
                                        friendlies.add(tmp);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //target the nearest one
        Ship closest = null;
        for (int a = 0; a < friendlies.size(); a++) {
            if (closest == null) {
                closest = friendlies.get(a);
            } else {
                double distClosest = distanceTo(closest);
                double distTest = distanceTo(friendlies.get(a));
                if (distTest < distClosest) {
                    closest = friendlies.get(a);
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestHostileShip() {
        target = null;
        Ship closest = null;
        //get a list of all nearby hostiles
        ArrayList<Entity> nearby = getCurrentSystem().getShipList();
        for (int a = 0; a < nearby.size(); a++) {
            Ship tmp = (Ship) nearby.get(a);
            //make sure it is in range
            if (distanceTo(tmp) < getSensor()) {
                if (nearby.get(a) instanceof Ship) {
                    if (!(nearby.get(a) instanceof Projectile)) {
                        if (!(nearby.get(a) instanceof Explosion)) {
                            if (tmp != this) {
                                //make sure it is alive and isn't docked
                                if (tmp.getState() == State.ALIVE && !tmp.isDocked()) {
                                    //check standings
                                    if (tmp.getStandingsToMe(this) < HOSTILE_STANDING || scanForContraband(tmp)) {
                                        //determine if this one is closer
                                        if (closest == null) {
                                            closest = tmp;
                                        } else {
                                            double distClosest = distanceTo(closest);
                                            double distTest = distanceTo(tmp);
                                            if (distTest < distClosest) {
                                                closest = tmp;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //see if it's being beaten on by the player
        if (shield / maxShield < PLAYER_AGGRO_SHIELD) {
            if (!faction.equals(PLAYER_FACTION)) {
                if (lastBlow == getUniverse().getPlayerShip()) {
                    if (closest != null) {
                        double distClosest = distanceTo(closest);
                        double distTest = distanceTo(getUniverse().getPlayerShip());
                        if (distTest < distClosest) {
                            closest = getUniverse().getPlayerShip();
                        }
                    } else {
                        closest = getUniverse().getPlayerShip();
                    }
                }
            }
        }
        //store
        target = closest;
    }

    public void targetNearestHostileStation() {
        target = null;
        Station closest = null;
        //get a list of all nearby hostiles
        ArrayList<Entity> nearby = getCurrentSystem().getStationList();
        ArrayList<Station> hostiles = new ArrayList<>();
        for (int a = 0; a < nearby.size(); a++) {
            Station tmp = (Station) nearby.get(a);
            //make sure it is in range
            if (distanceTo(tmp) < getSensor()) {
                //make sure it is alive
                if (tmp.getState() == State.ALIVE) {
                    //check standings
                    if (tmp.getStandingsToMe(this) <= HOSTILE_STANDING) {
                        hostiles.add(tmp);
                    }
                }
            }
        }
        //target the nearest one
        if (hostiles.size() > 0) {
            closest = hostiles.get(0);
            for (int a = 0; a < hostiles.size(); a++) {
                if (closest == null) {
                    closest = hostiles.get(a);
                } else {
                    double distClosest = distanceTo(closest);
                    double distTest = distanceTo(hostiles.get(a));
                    if (distTest < distClosest) {
                        closest = hostiles.get(a);
                    }
                }
            }
        } else {
            //nothing to target
        }
        //store
        target = closest;
    }

    protected void fightTarget() {
        /*
         * This is not to create classic dogfighting, but what I believe to be
         * a more realistic way to fight in zero gravity.
         */
        if (target != null) {
            //attack
            if (target.state == State.ALIVE) {
                fireActiveTurrets(target);
                double distance = distanceTo(target);
                double range = getNearWeaponRange();
                //fire thrusters based on range
                Ship avoid = avoidCollission();
                if (avoid == null || avoid == target) {
                    if (distance < (range / 3)) {
                        /*
                         * The enemy is getting too close to the ship, so fire the reverse
                         * thrusters.
                         */
                        fireForwardThrusters();
                    } else if (distance > (range / 2) && distance < (2 * range / 3)) {
                        /*
                         * The enemy is getting too far away from the ship, fire the forward
                         * thrusters.
                         */
                        fireRearThrusters();
                    } else if (distance > (range)) {
                        /*
                         * The enemy is out of weapons range and needs to be approached
                         */
                        double dP;
                        double d1 = magnitude(x - target.getX(), y - target.getY());
                        double d2 = magnitude((x + vx) - (target.getX() + target.getVx()), (y + vy) - (target.getY() + target.getVy()));
                        dP = d2 - d1;
                        if (dP + (accel * 2) > 0) {
                            fireRearThrusters();
                        }

                    }
                } else {
                    autopilotAvoidBlock(avoid);
                }
                double enemyX = getFireLeadX();
                double enemyY = getFireLeadY();
                /*double enemyX = (getX()) - (target.getX() + target.getWidth() / 2) + (vx - target.getVx());
                 double enemyY = (getY()) - (target.getY() + target.getHeight() / 2) + (vy - target.getVy());*/
                double desired;
                if (currentSystem != getUniverse().getPlayerShip().getCurrentSystem()) {
                    desired = FastMath.atan2(enemyY, enemyX);
                } else {
                    desired = Math.atan2(enemyY, enemyX);
                }
                desired = (desired + 2.0 * Math.PI) % (2.0 * Math.PI);
                //rotate to face the enemy
                if (Math.abs(theta - desired) > turning * tpf) {
                    if (distance > width && distance > height) {
                        if (theta - desired > -0.05) {
                            rotateMinus();
                        } else if (theta - desired < 0.05) {
                            rotatePlus();
                        }
                    }
                } else if (distance <= range) {
                    fireActiveGuns(target);
                }
            } else {
                target = null;
            }
        }
    }

    public void straffPositive() {
        if (getFuel() > 0) {
            setVx(getVx() - getAccel() * tpf * Math.cos(getTheta() + Math.PI / 2));
            setVy(getVy() - getAccel() * tpf * Math.sin(getTheta() + Math.PI / 2));
            //deduct fuel costs
            setFuel(getFuel() - getAccel() * tpf);
            //apply dampening coefficient
            applyDampening();
        }
    }

    public void straffNegative() {
        if (getFuel() > 0) {
            setVx(getVx() - getAccel() * tpf * Math.cos(getTheta() - Math.PI / 2));
            setVy(getVy() - getAccel() * tpf * Math.sin(getTheta() - Math.PI / 2));
            //deduct fuel costs
            setFuel(getFuel() - getAccel() * tpf);
            //apply dampening coefficient
            applyDampening();
        }
    }

    public void fireRearThrusters() {
        if (getFuel() > 0) {
            setVx(getVx() - getAccel() * tpf * Math.cos(getTheta()));
            setVy(getVy() - getAccel() * tpf * Math.sin(getTheta()));
            //deduct fuel costs
            setFuel(getFuel() - getAccel() * tpf);
            //apply dampening coefficient
            applyDampening();
            //keep playing the thrust noise
            thrusting = true;
        }
    }

    public void fireForwardThrusters() {
        if (getFuel() > 0) {
            setVx(getVx() + getAccel() * tpf * Math.cos(getTheta()));
            setVy(getVy() + getAccel() * tpf * Math.sin(getTheta()));
            //deduct fuel costs
            setFuel(getFuel() - getAccel() * tpf);
            //apply dampening coefficient
            applyDampening();
            //keep playing the thrust noise
            thrusting = true;
        }
    }

    public void decelerate() {
        //stops the ship entirely, good panic button in zero g motion.
        decelX();
        decelY();
    }

    protected void decelX() {
        if (getFuel() > 0) {
            if (Math.abs(getVx()) < 4 * getAccel() * tpf) {
                setVx(0);
            }
            //try to slow the ship down
            if (getVx() > 0) {
                setVx(getVx() - getAccel() * tpf);
                setFuel(getFuel() - getAccel() * tpf);
            } else if (getVx() < 0) {
                setVx(getVx() + getAccel() * tpf);
                setFuel(getFuel() - getAccel() * tpf);
            }
        }
    }

    protected void decelY() {
        if (getFuel() > 0) {
            if (Math.abs(getVy()) < 4 * getAccel() * tpf) {
                setVy(0);
            }
            if (getVy() > 0) {
                setVy(getVy() - getAccel() * tpf);
                setFuel(getFuel() - getAccel() * tpf);
            } else if (getVy() < 0) {
                setVy(getVy() + getAccel() * tpf);
                setFuel(getFuel() - getAccel() * tpf);
            }
        }
    }

    public void rotateMinus() {
        if (getFuel() > 0) {
            setTheta(getTheta() - getTurning() * tpf);
            setFuel(getFuel() - getTurning() * tpf);
        }
    }

    public void rotatePlus() {
        if (getFuel() > 0) {
            setTheta(getTheta() + getTurning() * tpf);
            setFuel(getFuel() - getTurning() * tpf);
        }
    }

    public void rotateProportion(double p) {
        if (getFuel() > 0) {
            double sign = Math.signum(p);
            if (Math.abs(p) > 1) {
                p = 1;
                p *= sign;
            }
            setTheta(getTheta() + getTurning() * p * tpf);
            setFuel(getFuel() - getTurning() * p * tpf);
        }
    }

    public void rotateAngle(double dt) {
        if (getFuel() > 0) {
            double sign = Math.signum(dt);
            if (Math.abs(dt) > turning) {
                dt = turning;
                dt *= sign;
            }
            setTheta(getTheta() + dt * tpf);
            setFuel(getFuel() - dt * tpf);
        }
    }

    public void rotate(double dt) {
        setTheta(getTheta() + dt);
    }

    @Override
    public void informOfCollisionWith(Entity target) {
        if (target instanceof Celestial) {
            if (!(target instanceof Projectile)) {
                if (target instanceof CargoPod) {
                    CargoPod pod = (CargoPod) target;
                    addToCargoBay(pod.getWare());
                    pod.setHull(-1000);
                    pod.setState(State.DYING);
                    pod.setWare(null);
                } else {
                    Celestial tmp = (Celestial) target;
                    //get the differential velocity
                    double mx = vx - tmp.getVx();
                    double my = vy - tmp.getVy();
                    double q = Math.sqrt(mx * mx + my * my);
                    //take it as damage
                    dealDamage(10 * q);
                }
            } else {
                //is it owned by me? if not apply damage
                Projectile tmp = (Projectile) target;
                if (tmp.getOwner() != this) {
                    if (!tmp.isGuided()) {
                        dealDamage(tmp.getDamage());
                        //store last ship to attack this ship
                        lastBlow = tmp.getOwner();
                    } else if (tmp.getTarget() == this) {
                        dealDamage(tmp.getDamage());
                        //store last ship to attack this ship
                        lastBlow = tmp.getOwner();
                    }
                }
            }
        }
    }

    public void dealDamage(double damage) {
        if (damage > 0) {
            shield -= damage;
            if (shield < 0) {
                hull += shield;
                shield = 0;
            }
        } else {
            hull -= damage;
        }
    }

    /*
     * Rendering
     */
    @Override
    public void render(Graphics g, double dx, double dy) {
        if (tex != null) {
            //cache theta
            last_theta = theta;
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
                rot.rotate(getTheta() - (Math.PI / 2), getWidth() / 2, getHeight() / 2);
                //apply transform
                f.transform(rot);
                f.drawImage(raw_tex, 0, 0, null);
            }
            //draw health bars
            if (this != getUniverse().getPlayerShip()) {
                drawHealthBars(g, dx, dy);
            }
            if (Universe.DEBUG_RENDER) {
                ((Graphics2D) (g)).setStroke(new BasicStroke(1));
                //draw avoidance info
                Line2D tmp = getDodgeLine();
                g.setColor(Color.WHITE);
                g.drawLine((int) (tmp.getX1() - dx), (int) (tmp.getY1() - dy), (int) (tmp.getX2() - dx), (int) (tmp.getY2() - dy));
                Rectangle tmp2 = tmp.getBounds();
                g.drawRect((int) (tmp2.getX() - dx), (int) (tmp2.getY() - dy), (int) tmp2.getWidth(), (int) tmp2.getHeight());
            }
            //draw the buffer onto the main frame
            g.drawImage(tex, (int) (getX() - dx), (int) (getY() - dy), null);
            if (Universe.DEBUG_RENDER) {
                ((Graphics2D) (g)).setStroke(new BasicStroke(1));
                //draw the bounds
                for (int a = 0; a < getBounds().size(); a++) {
                    double bx = getBounds().get(a).x;
                    double by = getBounds().get(a).y;
                    int bw = getBounds().get(a).width;
                    int bh = getBounds().get(a).height;
                    g.setColor(Color.PINK);
                    g.drawRect((int) (bx - dx), (int) (by - dy), bw, bh);
                }
            }
        } else {
            initGraphics();
        }
    }

    protected void drawHealthBars(Graphics g, double dx, double dy) {
        //draw health bars
        double hullPercent = hull / maxHull;
        double shieldPercent = shield / maxShield;
        g.setColor(Color.RED);
        g.fillRect((int) (getX() - dx), (int) (getY() - dy), (int) (getWidth() * hullPercent), 2);
        g.setColor(Color.GREEN);
        g.fillRect((int) (getX() - dx), (int) (getY() - dy), (int) (getWidth() * shieldPercent), 2);
    }

    public void applyDampening() {
        double dampX = 0.25 * vx * tpf;
        double dampY = 0.25 * vy * tpf;
        vx -= dampX;
        vy -= dampY;
    }

    /*
     * Access and Mutation
     */
    @Override
    public ArrayList<Rectangle> getBounds() {
        return getBound();
    }

    public final String getType() {
        return type;
    }

    public final void setType(String type) {
        this.type = type;
    }

    public double getAccel() {
        return accel;
    }

    public void setAccel(double accel) {
        this.accel = accel;
    }

    public double getTurning() {
        return turning;
    }

    public void setTurning(double turning) {
        this.turning = turning;
    }

    public double getHull() {
        return hull;
    }

    public void setHull(double hull) {
        this.hull = hull;
    }

    public double getShield() {
        return shield;
    }

    public void setShield(double shield) {
        this.shield = shield;
    }

    public double getMaxShield() {
        return maxShield;
    }

    public void setMaxShield(double maxShield) {
        this.maxShield = maxShield;
    }

    public double getShieldRechargeRate() {
        return shieldRechargeRate;
    }

    public void setShieldRechargeRate(double shieldRechargeRate) {
        this.shieldRechargeRate = shieldRechargeRate;
    }

    public double getMaxHull() {
        return maxHull;
    }

    public void setMaxHull(double maxHull) {
        this.maxHull = maxHull;
    }

    public ArrayList<Rectangle> getBound() {
        if (vx != 0 || vy != 0 || last_theta != theta || bound.isEmpty()) {
            updateBound();
        }
        return bound;
    }

    public void setBound(ArrayList<Rectangle> bound) {
        this.bound = bound;
    }

    public double getFuel() {
        return fuel;
    }

    public void setFuel(double fuel) {
        this.fuel = fuel;
    }

    public double getMaxFuel() {
        return maxFuel;
    }

    @Override
    public double getMass() {
        int cmass = 0;
        for (int a = 0; a < cargoBay.size(); a++) {
            cmass += cargoBay.get(a).getMass();
        }
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).getMounted() != null) {
                cmass += hardpoints.get(a).getMounted().getMass();
            }
        }
        return mass + cmass;
    }

    public void setMaxFuel(double maxFuel) {
        this.maxFuel = maxFuel;
    }

    protected void updateBound() {
        bound.clear();

        if (tex != null) {
            //calculate dynamic bounds
            int w = tex.getWidth();
            int h = tex.getHeight();
            int s = Math.max(6, w / 20);
            for (int _y = 0; _y < h; _y += s) {
                for (int _x = 0; _x < w; _x += s) {
                    //skip alpha pixels
                    if ((tex.getRGB(_x, _y) & 0xFF000000) != 0xFF000000) {
                        continue;
                    } else {
                        //convert to a rectangle
                        int ax = (int) (x + _x);
                        int ay = (int) (y + _y);
                        bound.add(new Rectangle(ax - s, ay - s, s * 2, s * 2));
                    }
                }
            }
        } else //do rectangle detection
        if (width != 0 && height != 0) {
            bound.add(new Rectangle((int) getX(), (int) getY(), getWidth(), getHeight()));
        } else {
            bound.add(new Rectangle((int) getX(), (int) getY(), 50, 50));
        }
    }

    public boolean addToCargoBay(Item item) {
        if (item != null) {
            /*
             * Puts an item into the cargo bay if there is space available.
             */
            double used = 0;
            for (int a = 0; a < cargoBay.size(); a++) {
                used += cargoBay.get(a).getVolume();
            }
            double fVol;
            if (cargoBay.contains(item)) {
                fVol = item.getVolume() / item.getQuantity();
            } else {
                fVol = item.getVolume();
            }
            if ((cargo - used) > fVol) {
                if (!cargoBay.contains(item)) {
                    cargoBay.add(item);
                } else {
                    item.setQuantity(item.getQuantity() + 1);
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public void removeFromCargoBay(Item item) {
        if (item.getQuantity() > 1) {
            item.setQuantity(item.getQuantity() - 1);
        } else {
            cargoBay.remove(item);
        }
    }

    public void ejectModule(Hardpoint hardpoint) {
        CargoPod pod = new CargoPod(hardpoint.getMounted());
        hardpoint.unmount(hardpoint.getMounted());
        pod.setFaction("Neutral");
        pod.init(false);
        //make sure it isn't touching this ship and has the right velocity
        pod.setVx(vx * 1.1);
        pod.setVy(vy * 1.1);
        pod.setX(x - width);
        pod.setY(y - height);
        //store position
        double dT = rnd.nextInt() % (Math.PI * 2.0);
        double dx = width * 2 * Math.cos(dT);
        double dy = height * 2 * Math.sin(dT);
        pod.setX(getCenterX() - pod.getWidth() / 2 + dx);
        pod.setY(getCenterY() - pod.getHeight() / 2 + dy);
        //calculate speed
        double speed = rnd.nextInt(25) + 1;
        double pdx = speed * Math.cos(dT);
        double pdy = speed * Math.sin(dT);
        //add to host vector
        pod.setVx(getVx() + pdx);
        pod.setVy(getVy() + pdy);
        pod.setCurrentSystem(currentSystem);
        //deploy
        getCurrentSystem().putEntityInSystem(pod);
    }

    public void ejectCargo(Item item) {
        if (cargoBay.contains(item)) {
            //if this equipment, stop the sound
            if (item instanceof Equipment) {
                Equipment tmp = (Equipment) item;
                tmp.deactivate();
                tmp.killSounds();
            }
            //remove
            cargoBay.remove(item);
            CargoPod pod = new CargoPod(item);
            pod.setFaction("Neutral");
            pod.init(false);
            //make sure it isn't touching this ship and has the right velocity
            pod.setVx(vx * 1.1);
            pod.setVy(vy * 1.1);
            pod.setX(x - width);
            pod.setY(y - height);
            //store position
            double dT = rnd.nextInt() % (Math.PI * 2.0);
            double dx = width * 2 * Math.cos(dT);
            double dy = height * 2 * Math.sin(dT);
            pod.setX(getCenterX() - pod.getWidth() / 2 + dx);
            pod.setY(getCenterY() - pod.getHeight() / 2 + dy);
            //calculate speed
            double speed = rnd.nextInt(25) + 1;
            double pdx = speed * Math.cos(dT);
            double pdy = speed * Math.sin(dT);
            //add to host vector
            pod.setVx(getVx() + pdx);
            pod.setVy(getVy() + pdy);
            pod.setCurrentSystem(currentSystem);
            //deploy
            getCurrentSystem().putEntityInSystem(pod);
        }
    }

    protected void dropLoot() {
        for (int a = 0; a < cargoBay.size(); a++) {
            double pick = rnd.nextFloat();
            if (pick <= LOOT_DROP_PROBABILITY) {
                ejectCargo(cargoBay.get(a));
            }
        }
        for (int a = 0; a < hardpoints.size(); a++) {
            double pick = rnd.nextFloat();
            if (pick <= LOOT_DROP_PROBABILITY) {
                ejectModule(hardpoints.get(a));
            }
        }
    }

    protected void dumpCargo() {
        for (int a = 0; a < cargoBay.size(); a++) {
            ejectCargo(cargoBay.get(a));
        }
    }

    protected void dropJumpEffect() {
        /*
         Generates jump effect
         */
        if (tex != null) {
            Point2D.Double size = new Point2D.Double(width * 15, height * 15);
            Explosion jumpEffect = new Explosion(size, "Jump", 0.5);
            jumpEffect.setFaction("Neutral");
            jumpEffect.init(false);
            //store position
            jumpEffect.setX(getCenterX() - size.x / 2);
            jumpEffect.setY(getCenterY() - size.y / 2);
            //use host velocity as jump effect velocity
            jumpEffect.setVx(getVx());
            jumpEffect.setVy(getVy());
            jumpEffect.setCurrentSystem(currentSystem);
            //randomize rotation
            jumpEffect.setTheta(rnd.nextDouble() * (2 * Math.PI));
            //deploy
            getCurrentSystem().putEntityInSystem(jumpEffect);
        }
    }

    protected void explode() {
        /*
         * Generates explosion effect
         */
        if (tex != null) {
            Point2D.Double size = new Point2D.Double(width, height);
            for (int a = 0; a < 15; a++) {
                Explosion exp = new Explosion(size, explosion, 3);
                exp.setFaction("Neutral");
                exp.init(false);
                //calculate helpers
                double dT = rnd.nextInt() % (Math.PI * 2.0);
                double ew = 2 * rnd.nextInt(getWidth() + 1) - getWidth();
                double dx = ew * Math.cos(dT);
                double dy = ew * Math.sin(dT);
                //store position
                exp.setX(getCenterX() - exp.getWidth() / 2 + dx);
                exp.setY(getCenterY() - exp.getHeight() / 2 + dy);
                //calculate speed
                double speed = rnd.nextInt(40) + 50;
                double pdx = speed * Math.cos(dT);
                double pdy = speed * Math.sin(dT);
                //add to host vector
                exp.setVx(getVx() + pdx);
                exp.setVy(getVy() + pdy);
                exp.setCurrentSystem(currentSystem);
                //randomize rotation
                exp.setTheta(rnd.nextDouble() * (2 * Math.PI));
                //deploy
                getCurrentSystem().putEntityInSystem(exp);
            }
        }
    }

    public int getNumInCargoBay(Item item) {
        int count = 0;
        if (item != null) {
            String iname = item.getName();
            String itype = item.getType();
            String group = item.getGroup();
            for (int a = 0; a < cargoBay.size(); a++) {
                Item tmp = cargoBay.get(a);
                if (iname.equals(tmp.getName())) {
                    if (itype.equals(tmp.getType())) {
                        if (group.equals(tmp.getGroup())) {
                            count += tmp.getQuantity();
                        }
                    }
                }
            }
        }
        return count;
    }

    public double getBayUsed() {
        double cmass = 0;
        for (int a = 0; a < cargoBay.size(); a++) {
            cmass += cargoBay.get(a).getVolume();
        }
        return cmass;
    }

    public void addInitialCargo(String cargo) {
        if (cargo != null) {
            String[] stuff = cargo.split("/");
            for (String s : stuff) {
                String[] tb = s.split("~");
                Item tmp = new Item(tb[0]);
                int count = 1;
                if (tb.length == 2) {
                    count = Integer.parseInt(tb[1]);
                }
                for (int v = 0; v < count; v++) {
                    addToCargoBay(tmp);
                }
            }
        }
    }

    public boolean hasInCargo(Item item) {
        return cargoBay.contains(item);
    }

    public boolean hasInCargo(String item) {
        for (int a = 0; a < cargoBay.size(); a++) {
            if (cargoBay.get(a).getName().equals(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGroupInCargo(String group) {
        for (int a = 0; a < cargoBay.size(); a++) {
            if (cargoBay.get(a).getGroup() != null) {
                if (cargoBay.get(a).getGroup().equals(group)) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<Item> getCargoBay() {
        return cargoBay;
    }

    protected void installHardpoints(Term relevant) throws NumberFormatException {
        /*
         * Equips the ship with hardpoints
         */
        String complex = relevant.getValue("hardpoint");
        if (complex != null) {
            String[] arr = complex.split("/");
            for (int a = 0; a < arr.length; a++) {
                String[] re = arr[a].split(",");
                String hType = re[0];
                int hSize = Integer.parseInt(re[1]);
                double hr = Double.parseDouble(re[2]);
                double ht = Double.parseDouble(re[3]);
                hardpoints.add(new Hardpoint(this, hType, hSize, hr, ht));
            }
        }
    }

    public void installLoadout() {
        /*
         * Equips the ship with equipment from the starting loadout
         */
        if (equip != null) {
            //equip player from install keyword
            String[] arr = equip.split("/");
            for (String s : arr) {
                Item test = new Item(s);
                /*
                 * Cannons and launchers are both in the weapon class
                 */
                if (getType() != null) {
                    try {
                        if (test.getType().equals("cannon") || test.getType().equals("missile")
                                || test.getType().equals("battery") || test.getType().equals("turret")) {
                            Weapon wep = new Weapon(s);
                            fit(wep);
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }
                }
            }
        }
    }

    public void installFaction() {
        myFaction = new Faction(faction);
    }

    /*
     * Fitting
     */
    public void fit(Equipment equipment) {
        for (int a = 0; a < hardpoints.size(); a++) {
            if (equipment.getQuantity() == 1) {
                if (hardpoints.get(a).isEmpty()) {
                    if (hardpoints.get(a).getSize() >= equipment.getVolume()) {
                        if (hardpoints.get(a).getType().equals(equipment.getType())) {
                            hardpoints.get(a).mount(equipment);
                            //is this a weapon?
                            Weapon wep = (Weapon) equipment;
                            wep.initGraphics();
                            //remove from cargo
                            cargoBay.remove(equipment);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void unfit(Equipment equipment) {
        try {
            for (int a = 0; a < hardpoints.size(); a++) {
                if (hardpoints.get(a).getMounted() == equipment) {
                    if (getBayUsed() + equipment.getVolume() <= cargo) {
                        hardpoints.get(a).unmount(equipment);
                        cargoBay.add(equipment);
                    } else {
                        //not enough room
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(Ship.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void fireActiveTurrets(Entity target) {
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).getType().equals("turret") || hardpoints.get(a).getType().equals("battery")) {
                hardpoints.get(a).activate(target);
            }
        }
    }

    public void fireActiveGuns(Entity target) {
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).getType().equals("cannon") || hardpoints.get(a).getType().equals("missile")) {
                hardpoints.get(a).activate(target);
            }
        }
    }

    public double getNearWeaponRange() {
        /*
         * Returns the range of the closest range onlined weapon.
         */
        double range = Double.MAX_VALUE;
        for (int a = 0; a < hardpoints.size(); a++) {
            if (hardpoints.get(a).isEnabled()) {
                if (hardpoints.get(a).notNothing()) {
                    if (hardpoints.get(a).getMounted().getRange() < range) {
                        range = hardpoints.get(a).getMounted().getRange();
                    }
                }
            }
        }
        return range;
    }

    /*
     * Access and mutation
     */
    public double getSensor() {
        return sensor;
    }

    public void setSensor(double sensor) {
        this.sensor = sensor;
    }

    public double getCargo() {
        return cargo;
    }

    public void setCargo(double cargo) {
        this.cargo = cargo;
    }

    public ArrayList<Hardpoint> getHardpoints() {
        return hardpoints;
    }

    public void setHardpoints(ArrayList<Hardpoint> hardpoints) {
        this.hardpoints = hardpoints;
    }

    public Ship getTarget() {
        return target;
    }

    public void setTarget(Ship target) {
        this.target = target;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    public String getEquip() {
        return equip;
    }

    public void setEquip(String equip) {
        this.equip = equip;
    }

    public Faction getMyFaction() {
        return myFaction;
    }

    public void setMyFaction(Faction myFaction) {
        this.myFaction = myFaction;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public int getStandingsToMe(String faction) {
        if (myFaction != null) {
            return (int) myFaction.getStanding(faction);
        } else {
            installFaction();
            return 0;
        }
    }

    public int getStandingsToMe(Ship ship) {
        try {
            if (myFaction != null) {
                if (ship.getFaction().hashCode() == PLAYER_FACTION.hashCode()) {
                    return (int) ship.getMyFaction().getStanding(getFaction());
                } else {
                    return (int) myFaction.getStanding(ship.getFaction());
                }
            } else {
                installFaction();
                return 0;
            }
        } catch (Exception e) {
            //e.printStackTrace();
            return 0;
        }
    }

    public String getPilot() {
        if (this != getUniverse().getPlayerShip()) {
            return pilot;
        } else {
            return "You";
        }
    }

    public void setPilot(String pilot) {
        this.pilot = pilot;
    }

    @Override
    public String toString() {
        String ret;
        {
            if (!alternateString) {
                /*
                 * This is the string used for reporting NPC ships.
                 */
                ret = "(" + type + ") - " + name + ", " + faction;
            } else {
                /*
                 * This is the string used for reporting player ships.
                 */
                if (currentSystem != getUniverse().getPlayerShip().getCurrentSystem()) {
                    ret = "[SHIP] (" + type + ") - " + name + ", " + currentSystem.getName();
                    if (docked) {
                        ret += " [" + port.getParent().getName() + "]";
                    }
                } else {
                    ret = "[SHIP] (" + type + ") - " + name;
                    if (docked) {
                        ret += " [" + port.getParent().getName() + "]";
                    }
                }
                if (this == getUniverse().getPlayerShip()) {
                    ret += " *Current Ship*";
                }
            }
        }
        return ret;
    }

    public double magnitude(double dx, double dy) {
        return Math.sqrt((dx * dx) + (dy * dy));
    }

    public boolean isDocked() {
        return docked;
    }

    public void setDocked(boolean docked) {
        this.docked = docked;
    }

    public PortContainer getPort() {
        return port;
    }

    public void setPort(PortContainer port) {
        this.port = port;
    }

    public Autopilot getAutopilot() {
        return autopilot;
    }

    public void setAutopilot(Autopilot autopilot) {
        this.autopilot = autopilot;
    }

    public long getCash() {
        if (homeBase == null) {
            return cash;
        } else {
            return homeBase.getCash();
        }
    }

    public void setCash(long cash) {
        if (homeBase == null) {
            this.cash = cash;
        } else {
            homeBase.setCash(cash);
        }
    }

    public boolean isThrustForward() {
        return controlThrustForward;
    }

    public void setThrustForward(boolean thrustForward) {
        this.controlThrustForward = thrustForward;
    }

    public boolean isThrustRear() {
        return controlThrustReverse;
    }

    public void setThrustRear(boolean thrustRear) {
        this.controlThrustReverse = thrustRear;
    }

    public boolean isRotateMinus() {
        return rotateMinus;
    }

    public void setRotateMinus(boolean rotateMinus) {
        this.rotateMinus = rotateMinus;
    }

    public boolean isRotatePlus() {
        return rotatePlus;
    }

    public void setRotatePlus(boolean rotatePlus) {
        this.rotatePlus = rotatePlus;
    }

    protected double getFireLeadX() {
        //get the center of the enemy
        double enemyX = getCenterX() - (target.getCenterX()) + (vx - target.getVx());
        return enemyX;
    }

    protected double getFireLeadY() {
        double enemyY = getCenterY() - (target.getCenterY()) + (vy - target.getVy());
        return enemyY;
    }

    public String getExplosion() {
        return explosion;
    }

    public void setExplosion(String explosion) {
        this.explosion = explosion;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isAlternateString() {
        return alternateString;
    }

    public void setAlternateString(boolean alternateString) {
        this.alternateString = alternateString;
    }

    public Celestial getFlyToTarget() {
        return flyToTarget;
    }

    public void setFlyToTarget(Celestial cel) {
        flyToTarget = cel;
    }

    public Station getBuyFromStation() {
        return buyFromStation;
    }

    public int getBuyFromPrice() {
        return buyFromPrice;
    }

    public Station getSellToStation() {
        return sellToStation;
    }

    public int getSellToPrice() {
        return sellToPrice;
    }

    public Item getWorkingWare() {
        return workingWare;
    }

    public Ship getLastBlow() {
        return lastBlow;
    }

    public void setLastBlow(Ship lastBlow) {
        this.lastBlow = lastBlow;
    }

    /*
     * The sound section
     */
    protected void updateSoundEffects() {
        /*
         * Thruster effect
         */
        if (thrusting) {
            playSound(engineLoop);
        } else {
            stopSound(engineLoop);
        }
    }

    public void playSound(Soundling sound) {
        if (soundQue == null) {
            soundQue = new ArrayList<>();
        }
        if (sound != null) {
            //are we in the player's system?
            if (currentSystem == getUniverse().getPlayerShip().getCurrentSystem()) {
                //make sure it doesn't already contain this noise
                boolean safe = true;
                for (int a = 0; a < soundQue.size(); a++) {
                    if (soundQue.get(a).getName().equals(name)) {
                        safe = false;
                        break;
                    }
                }
                if (safe && !sound.isPlaying()) {
                    soundQue.add(sound);
                }
            } else {
                //nope, no need to push anything to the que
            }
        }
    }

    public void stopSound(Soundling sound) {
        if (sound != null) {
            sound.stop();
        }
        if (soundQue != null) {
            soundQue.remove(sound);
        }
    }

    private void killSounds() {
        //halt noises
        stopSound(engineLoop);
        stopSound(notifyMessage);
        for (int a = 0; a < hardpoints.size(); a++) {
            hardpoints.get(a).getMounted().killSounds();
        }
        //clear que
        if (soundQue != null) {
            for (int a = 0; a < soundQue.size(); a++) {
                soundQue.get(a).stop();
            }
            soundQue.clear();
        }
    }

    protected void updateSoundSwitches() {
        //stop switches
        if (!controlThrustForward && !controlThrustReverse) {
            thrusting = false;
        }
    }

    public ArrayList<Soundling> getSoundQue() {
        return soundQue;
    }

    public double getJumpFuelCost(SolarSystem destination) {
        //calculate distance between current system and destination
        double cx = currentSystem.getX();
        double cy = currentSystem.getY();
        double tx = destination.getX();
        double ty = destination.getY();
        double dist = magnitude((cx - tx), (cy - ty));
        //fuel cost is linear
        double fuelCost = dist * 50;
        return fuelCost;
    }

    public boolean canJump(SolarSystem destination) {
        double safety;
        if (behavior == Behavior.UNIVERSE_TRADE) {
            safety = TRADER_JD_SAFETY_FUEL;
        } else {
            safety = JUMP_SAFETY_FUEL;
        }
        //make sure we have a jump drive group device
        if (hasGroupInCargo("jumpdrive")) {
            //fuel cost is linear
            if (fuel - getJumpFuelCost(destination) >= safety * maxFuel) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSalvageSoftware() {
        return hasGroupInCargo("salvagesoftware");
    }

    public boolean isScanForContraband() {
        return scanForContraband;
    }

    public void setScanForContraband(boolean scanForContraband) {
        this.scanForContraband = scanForContraband;
    }

    public void recieveReply(Binling choice) {
        if (conversation != null) {
            conversation.reply(choice);
        }
    }

    public void composeMessage(Ship recieve, String subject, String body, ArrayList<Binling> options) {
        AstralMessage tmp = new AstralMessage(this, subject, body, options);
        recieve.receiveMessage(tmp);
    }

    public boolean receiveMessage(AstralMessage message) {
        /*
         * NPCs do not use the messaging system to communicate with each other
         * so any sent message is disregarded if it is not a player ship. Any
         * message sent to a player ship is automatically forwarded to the
         * player's current ship.
         */
        message.setWasSent(true);
        if (faction.equals(PLAYER_FACTION)) {
            stopSound(notifyMessage);
            playSound(notifyMessage);
            if (this == getUniverse().getPlayerShip()) {
                //add to que
                messages.add(message);
            } else {
                //forward
                getUniverse().getPlayerShip().receiveMessage(message);
            }
            return true;
        } else {
            return false;
        }
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public ArrayList<AstralMessage> getMessages() {
        return messages;
    }

    public void hail() {
        /*
         * Used by the player to hail an NPC. The NPC has a direct line to
         * the player's ship for replying. Hailing initiates a new conversation
         * with the NPC.
         */
        //get player standings
        int standings = getUniverse().getPlayerShip().getStandingsToMe(this);
        if (conversation == null) {
            if (standings > 2) {
                if (!plotOffer) {
                    //on great terms
                    /*
                     * Will offer rumors and missions
                     */
                    //offer mission
                    ArrayList<String> choices = myFaction.getFriendlyNotifications();
                    if (choices.size() > 0) {
                        String pick = choices.get(rnd.nextInt(choices.size()));
                        conversation = new Conversation(this, "Hail", pick);
                    } else {
                        //nothing to say
                    }
                } else {
                    //will offer plots
                    ArrayList<String> choices = myFaction.getCampaignList();
                    if (choices.size() > 0) {
                        /*
                         * Campaigns are offered in order per faction. The list
                         * of campaigns the faction offers will be checked
                         * against the completed ones and the first one that is
                         * neither completed nor in progress will be offered.
                         */
                        String pick = null;
                        for (int a = 0; a < choices.size(); a++) {
                            boolean safe = true;
                            //check to make sure this isn't completed
                            for (int b = 0; b < getUniverse().getCompletedCampaigns().size(); b++) {
                                if (getUniverse().getCompletedCampaigns().get(b).getName().equals(choices.get(a))) {
                                    safe = false;
                                }
                            }
                            //check to make sure this isn't in progress
                            for (int b = 0; b < getUniverse().getPlayerCampaigns().size(); b++) {
                                if (getUniverse().getPlayerCampaigns().get(b).getName().equals(choices.get(a))) {
                                    safe = false;
                                }
                            }
                            //determine if safe
                            if (safe) {
                                pick = choices.get(a);
                                break;
                            } else {
                                //go to next one
                            }
                        }
                        //attempt to start plot if found
                        if (pick != null) {
                            conversation = new Conversation(this, "Hail", pick);
                        } else {
                            //do not offer plot
                        }
                    }
                }
            } else if (standings > -2) {
                //on neutral terms
                /*
                 * Will offer you missions
                 */
                ArrayList<String> choices = myFaction.getNeutralNotifications();
                if (choices.size() > 0) {
                    String pick = choices.get(rnd.nextInt(choices.size()));
                    conversation = new Conversation(this, "Hail", pick);
                } else {
                    //nothing to say
                }
            } else {
                //on bad terms
                /*
                 * Will be nasty to you
                 */
                ArrayList<String> choices = myFaction.getHateNotifications();
                if (choices.size() > 0) {
                    String pick = choices.get(rnd.nextInt(choices.size()));
                    conversation = new Conversation(this, "Hail", pick);
                } else {
                    //nothing to say
                }
            }
        } else {
            //still talking
        }
    }

    public void clearHomeBase() {
        setHomeBase(null);
    }

    /**
     * @return the homeBase
     */
    public Station getHomeBase() {
        return homeBase;
    }

    /**
     * @param homeBase the homeBase to set
     */
    public void setHomeBase(Station homeBase) {
        this.homeBase = homeBase;
    }

    /*
     * Bailing Code
     */
    public double getCourage() {
        return courage;
    }

    public void setCourage(double courage) {
        this.courage = courage;
    }

    protected void checkBail() {
        /*
         * Determines whether or not a ship will bail. Bail is checked when the
         * hull is less than the mark. It will not be rechecked until the shields are
         * back to the reset %.
         */
        if (courage < 1) {
            if (checkedBail()) {
                //set to false if shields are back to 100%
                if ((shield / maxShield) >= BAIL_CHECK_SHIELD_RESET) {
                    setCheckedBail(false);
                }
            } else if ((shield / maxShield) <= BAIL_CHECK_SHIELD_STOP) {
                //is the hull below 50%?
                if ((hull / maxHull) <= BAIL_CHECK_HULL_DAMAGE) {
                    //roll the dice
                    double dice = rnd.nextDouble();
                    if (dice > courage) {
                        //bail
                        bail();
                    } else {
                        //sad christmas
                        //System.out.println(getName() + " did not bail " + dice + " < " + courage);
                    }
                    //and stop
                    setCheckedBail(true);
                }
            }
        } else {
            //this can never bail
        }
    }

    public void bail() {
        /*
         * Causes the pilot to bail.
         */
        //abort autopilot commands and behaviors
        setBehavior(Behavior.NONE);
        setAutopilot(Autopilot.NONE);
        cmdAbortDock();
        //standing loss, not as bad as actually destroying the ship
        System.out.println(getName() + " bailed in " + currentSystem.getName());
        if (lastBlow.getFaction().equals(PLAYER_FACTION)) {
            //adjust the player's standings accordingly
            if (!faction.equals("Neutral")) {
                getUniverse().getPlayerShip().getMyFaction().derivedModification(myFaction, -0.5);
            }
        }
        //mark this ship as bailed
        setBailed(true);
        //eject cargo
        dumpCargo();
        //set the faction to neutral
        setFaction("Neutral");
        installFaction();
    }

    public boolean checkedBail() {
        return checkedBail;
    }

    public void setCheckedBail(boolean checkedBail) {
        this.checkedBail = checkedBail;
    }

    public boolean isBailed() {
        return bailed;
    }

    public void setBailed(boolean bailed) {
        this.bailed = bailed;
    }

    public void claim(Ship claimant) {
        if (bailed) {
            //make sure it won't be removed by setting bailed to false
            bailed = false;
            //store new faction
            faction = claimant.getFaction();
            installFaction();
            //push and pull
            getCurrentSystem().pullEntityFromSystem(this);
            getCurrentSystem().putEntityInSystem(this);
            //give it the behavior of the ship claiming it
            setBehavior(claimant.getBehavior());
        } else {
            //cannot be claimed
        }
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean hasPlotOffer() {
        return plotOffer;
    }

    public void setPlotOffer(boolean plotOffer) {
        this.plotOffer = plotOffer;
    }

    public boolean isPlotShip() {
        return plotShip;
    }

    public void setPlotShip(boolean plotShip) {
        this.plotShip = plotShip;
    }

}
