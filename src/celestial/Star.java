/*
 * Represents a star.
 */
package celestial;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.AstralIO;
import lib.Parser;

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
        BufferedImage tmp = new BufferedImage(RENDER_SIZE, RENDER_SIZE, BufferedImage.TYPE_INT_ARGB);
        //get graphics
        Shape circle = new Ellipse2D.Float(0, 0, RENDER_SIZE, RENDER_SIZE);
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
