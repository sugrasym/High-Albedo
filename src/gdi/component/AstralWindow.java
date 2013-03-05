/*
 * This is a window. It stores components.
 */
package gdi.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralWindow extends AstralComponent {

    protected Color backColor = Color.PINK;
    protected int order = 0;
    ArrayList<AstralComponent> components = new ArrayList<>();
    BufferedImage buffer;

    public AstralWindow() {
        super();
    }

    public void addComponent(AstralComponent component) {
        components.add(component);
    }

    public void removeComponent(AstralComponent component) {
        components.remove(component);
    }

    @Override
    public void periodicUpdate() {
        for (int a = 0; a < components.size(); a++) {
            components.get(a).periodicUpdate();
        }
    }

    @Override
    public void render(Graphics f) {
        try {
            if (visible) {
                if (buffer == null) {
                    buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                }
                //get graphics
                Graphics2D s = (Graphics2D) buffer.getGraphics();
                //render the backdrop
                s.setColor(backColor);
                s.fillRect(0, 0, width, height);
                //render components
                for (int a = 0; a < components.size(); a++) {
                    components.get(a).render(s);
                }
                //draw focus borders
                if (focused) {
                    s.setColor(getFocusColor());
                    s.drawRect(0, 0, width - 1, height - 1);
                }
                //push frame
                f.drawImage(buffer, x, y, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleKeyTypedEvent(KeyEvent ke) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyTypedEvent(ke);
                }
            }
        }
    }

    @Override
    public void handleKeyPressedEvent(KeyEvent ke) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyPressedEvent(ke);
                }
            }
        }
    }

    @Override
    public void handleKeyReleasedEvent(KeyEvent ke) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyReleasedEvent(ke);
                }
            }
        }
    }

    @Override
    public void handleMouseClickedEvent(MouseEvent me) {
        /*
         * Mouse position selects focus, determine what to select
         */
        if (visible) {
            Rectangle mRect = new Rectangle(me.getX() - x, me.getY() - y, 1, 1);
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).intersects(mRect)) {
                    components.get(a).setFocused(true);
                    components.get(a).handleMouseClickedEvent(me);
                } else {
                    components.get(a).setFocused(false);
                }
            }
        }
    }

    @Override
    public void handleMousePressedEvent(MouseEvent me) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleMousePressedEvent(me);
                }
            }
        }
    }

    @Override
    public void handleMouseReleasedEvent(MouseEvent me) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleMouseReleasedEvent(me);
                }
            }
        }
    }

    @Override
    public void handleMouseMovedEvent(MouseEvent me) {
        if (visible) {
            /*
             * Mouse position selects focus, determine what to select
             */
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleMouseMovedEvent(me);
                }
            }
        }
    }

    public Color getBackColor() {
        return backColor;
    }

    public void setBackColor(Color backColor) {
        this.backColor = backColor;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
