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
 * Represents a star.
 */
package celestial;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.AstralIO;
import lib.Parser;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Star extends Planet {

    public Star(String name, Parser.Term texture, int diameter) {
        super(name, texture, diameter);
    }

    @Override
    public void initGraphics() {
        BufferedImage tmp = new BufferedImage(getUniverse().getSettings().RENDER_SIZE, getUniverse().getSettings().RENDER_SIZE, BufferedImage.TYPE_INT_ARGB);
        //get graphics
        Shape circle = new Ellipse2D.Float(0, 0, getUniverse().getSettings().RENDER_SIZE, getUniverse().getSettings().RENDER_SIZE);
        Graphics2D gfx = (Graphics2D) tmp.getGraphics();
        //only draw inside the circle
        gfx.setClip(circle);
        gfx.clip(circle);
        //get logical texture
        if (getTexture().getValue("group").matches("basic")) {
            //get the asset
            String asset = getTexture().getValue("asset");
            try {
                //just load an image
                raw_tex = AstralIO.loadImage("planet/" + asset);
            } catch (NullPointerException | URISyntaxException ex) {
                Logger.getLogger(Jumphole.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
