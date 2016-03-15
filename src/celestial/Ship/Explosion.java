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
 * A unit used for an explosion effect.
 */
package celestial.Ship;

import engine.Entity;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import lib.Parser;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Explosion extends Ship {

    double maxLife = 3;
    double lifeLimit;
    double elapsed = 0;
    double size;

    public Explosion(Point2D.Double size, String type, double maxLife) {
        super("Debris", type);
        this.maxLife = maxLife;
        //store size
        if (size.x > size.y) {
            this.size = size.x;
        } else {
            this.size = size.y;
        }
        //calculate life limit
        lifeLimit = 0.5 + new Random().nextDouble() * maxLife;
    }

    @Override
    public void init(boolean loadedGame) {
        super.init(loadedGame);
    }

    @Override
    protected void initStats() {
        /*
         * Loads the stats for this ship from the ships file.
         */
        //create parser
        Parser parse = Universe.getCache().getExplosionCache();
        //get the term with this ship's type
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Explosion");
        Parser.Term relevant = null;
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
            shield = maxShield = Double.MAX_VALUE;
            shieldRechargeRate = Double.MAX_VALUE;
            maxHull = hull = Double.MAX_VALUE;
            maxFuel = fuel = Double.MAX_VALUE;
            //faction
            installFaction();
            //bring the ship to life
            state = State.ALIVE;
        } else {
            System.out.println("The item " + getName() + " does not exist in SHIPS.txt");
        }
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
    protected void updateBound() {
        bound.clear();

        if (width != 0 && height != 0) {
            bound.add(new Rectangle((int) getX(), (int) getY(), getWidth(), getHeight()));
        } else {
            bound.add(new Rectangle((int) getX(), (int) getY(), 50, 50));
        }
    }
    
    @Override
    public boolean quickCollideWith(Rectangle target) {
        return true;
    }

    @Override
    protected void drawHealthBars(Graphics g, double dx, double dy) {
        //derp
    }

    @Override
    public void explode() {
        //avoid some recursion
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
    protected void behave() {
        //these entities have no behavior
    }

    @Override
    public void render(Graphics g, double dx, double dy) {
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
                //calculate size
                double sx = size * (1 - (elapsed / lifeLimit));
                //create an affine transform
                AffineTransform rot = new AffineTransform();
                rot.rotate(getTheta() - (Math.PI / 2), sx / 2, sx / 2);
                //apply transform
                f.transform(rot);
                //calculate size
                f.drawImage(raw_tex, 0, 0, (int) sx, (int) sx, null);
            }
            /*//draw avoidance info
             Line2D tmp = getDodgeLine();
             g.setColor(Color.WHITE);
             g.drawLine((int) (tmp.getX1() - dx), (int) (tmp.getY1() - dy), (int) (tmp.getX2() - dx), (int) (tmp.getY2() - dy));
             Rectangle tmp2 = tmp.getBounds();
             g.drawRect((int) (tmp2.getX() - dx), (int) (tmp2.getY() - dy), (int) tmp2.getWidth(), (int) tmp2.getHeight());*/
            //draw the buffer onto the main frame
            g.drawImage(tex, (int) (getX() - dx), (int) (getY() - dy), null);
        } else {
            initGraphics();
        }
    }

    @Override
    public void initGraphics() {
        try {
            //get the image
            raw_tex = Universe.getCache().getExplosionSprite(type);
            //create the usable version
            setHeight((int) size);
            setWidth((int) size);
            tex = new BufferedImage(getWidth() + 1, getHeight() + 1, BufferedImage.TYPE_INT_ARGB);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        String ret = "";
        {
            ret = "Debris";
        }
        return ret;
    }
}
