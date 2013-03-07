/*
 * The home window used in the main menu
 */
package gdi;

import engine.Engine;
import gdi.component.AstralLabel;
import gdi.component.AstralList;
import gdi.component.AstralWindow;
import java.awt.Font;
import java.awt.event.MouseEvent;

/**
 *
 * @author nwiehoff
 */
public class MenuHomeWindow extends AstralWindow {

    AstralLabel logoLabel = new AstralLabel();
    AstralList mainList = new AstralList(this);
    private Engine engine;

    public MenuHomeWindow(Engine engine) {
        super();
        this.engine = engine;
        generate();
    }

    private void generate() {
        //a nice color
        backColor = windowGrey;
        //setup dimensions
        setWidth(800);
        setHeight(600);
        //setup logo label
        logoLabel.setText("High Albedo");
        logoLabel.setFont(new Font("Monospaced", Font.PLAIN, 36));
        logoLabel.setX(0);
        logoLabel.setY(0);
        logoLabel.setWidth(getWidth());
        logoLabel.setHeight(50);
        logoLabel.setVisible(true);
        //setup menu list
        mainList.setX(getWidth() / 2 - 200);
        mainList.setY(getHeight() / 2 - 200);
        mainList.setWidth(400);
        mainList.setHeight(400);
        mainList.setVisible(true);
        mainList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        //store text
        mainList.addToList("Select an Option");
        mainList.addToList("");
        mainList.addToList("New Game");
        mainList.addToList("");
        mainList.addToList("Load Quicksave");
        mainList.addToList("Load Game");
        mainList.addToList("");
        //add components
        addComponent(logoLabel);
        addComponent(mainList);
        //make visible
        setVisible(true);
    }

    public void handleMouseClickedEvent(MouseEvent me) {
        super.handleMouseClickedEvent(me);
        if (mainList.isFocused()) {
            String command = (String) mainList.getItemAtIndex(mainList.getIndex());
            parseCommand(command);
        }
    }

    private void parseCommand(String command) {
        if (command.matches("New Game")) {
            setVisible(false);
            engine.start();
        } else if (command.matches("Load Quicksave")) {
            setVisible(false);
            engine.start();
            engine.load("quicksave.txt");
        } else if (command.matches("Load Game")) {
            
        }
    }
}
