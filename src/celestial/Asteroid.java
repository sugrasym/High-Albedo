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
 * This is the method by which ships can move between solar systems.
 */
package celestial;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import lib.AstralIO;

public class Asteroid extends Planet {

    private transient BufferedImage tex;

    public Asteroid(String name) {
        super(name, null, 800);
        setState(State.ALIVE);
    }

    @Override
    public void initGraphics() {
        try {
            raw_tex = AstralIO.loadImage("planet/Asteroid.png");
            //create the usable version
            ImageIcon icon = new ImageIcon(raw_tex);
            setHeight(icon.getIconHeight());
            setWidth(icon.getIconWidth());
            tex = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            //setup the buffer's graphics
            Graphics2D g = tex.createGraphics();
            //clear the buffer
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setComposite(AlphaComposite.Src);
            //enable anti aliasing
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            //draw the updated version
            {
                //create an affine transform
                AffineTransform rot = new AffineTransform();
                rot.rotate(getTheta() - (Math.PI / 2), getWidth() / 2, getHeight() / 2);
                //apply transform
                g.transform(rot);
                g.drawImage(raw_tex, 0, 0, null);
            }
        } catch (NullPointerException | URISyntaxException ex) {
            Logger.getLogger(Asteroid.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void render(Graphics f, double dx, double dy) {
        if (tex != null) {
            f.drawImage(tex, (int) (getX() - dx), (int) (getY() - dy), null);
        } else {
            initGraphics();
        }
    }

    @Override
    public void disposeGraphics() {
        raw_tex = null;
        tex = null;
    }

    @Override
    public void alive() {
        //update bound
        getBounds().clear();
        getBounds().add(new Rectangle((int) getX(), (int) getY(), getDiameter(), getDiameter()));
    }
}
