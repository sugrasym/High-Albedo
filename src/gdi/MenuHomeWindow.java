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
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import lib.AstralIO;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class MenuHomeWindow extends AstralWindow {

    AstralLabel logoLabel = new AstralLabel();
    AstralList mainList = new AstralList(this);
    AstralList gameList = new AstralList(this);
    AstralList saveList = new AstralList(this);
    AstralList settingList = new AstralList(this);
    private Engine engine;

    public MenuHomeWindow(Engine engine) {
        super();
        this.engine = engine;
        generate();
    }

    private void generate() {
        setFocused(true);
        //a nice color
        backColor = windowGrey;
        //setup dimensions
        setWidth(800);
        setHeight(600);
        //setup logo label
        logoLabel.setText("High Albedo: Development Build");
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
        populateMainMenuList();
        //setup save game list
        gameList.setX(getWidth() / 2 - 200);
        gameList.setY(getHeight() / 2 - 200);
        gameList.setWidth(400);
        gameList.setHeight(400);
        gameList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        gameList.setVisible(false);
        //setup load game list
        saveList.setX(getWidth() / 2 - 200);
        saveList.setY(getHeight() / 2 - 200);
        saveList.setWidth(400);
        saveList.setHeight(400);
        saveList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        saveList.setVisible(false);
        //setup settings list
        settingList.setX(getWidth() / 2 - 200);
        settingList.setY(getHeight() / 2 - 200);
        settingList.setWidth(400);
        settingList.setHeight(400);
        settingList.setFont(new Font("Monospaced", Font.PLAIN, 16));
        settingList.setVisible(false);
        //add components
        addComponent(logoLabel);
        addComponent(mainList);
        addComponent(gameList);
        addComponent(saveList);
        addComponent(settingList);
        //make visible
        setVisible(true);
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (mainList.isVisible()) {
            populateMainMenuList();
        }
    }

    public void handleMouseClickedEvent(MouseEvent me) {
        super.handleMouseClickedEvent(me);
        String command = "";
        if (mainList.isVisible()) {
            command = (String) mainList.getItemAtIndex(mainList.getIndex());
        } else if (gameList.isVisible()) {
            command = (String) gameList.getItemAtIndex(gameList.getIndex());
        } else if (settingList.isVisible()) {
            command = (String) settingList.getItemAtIndex(settingList.getIndex());
        }
        parseCommand(command);
    }

    private void parseCommand(String command) {
        if (mainList.isVisible()) {
            if (command.matches("New Game")) {
                setVisible(false);
                engine.suicide();
                engine.newGame();
                engine.start();
            } else if (command.matches("Load Quicksave")) {
                setVisible(false);
                engine.start();
                engine.load("quicksave");
            } else if (command.matches("Load Game")) {
                mainList.setVisible(false);
                saveList.setVisible(false);
                settingList.setVisible(false);
                populateLoadGameList();
                gameList.setVisible(true);
            } else if (command.matches("Save Game")) {
                mainList.setVisible(false);
                gameList.setVisible(false);
                settingList.setVisible(false);
                populateSaveGameList();
                saveList.setVisible(true);
            } else if (command.matches("Settings")) {
                mainList.setVisible(false);
                saveList.setVisible(false);
                gameList.setVisible(false);
                populateSettingList();
                settingList.setVisible(true);
            } else if (command.matches("Resume")) {
                engine.menu();
            }
        } else if (gameList.isVisible()) {
            int index = gameList.getIndex();
            if (index > 2) {
                engine.start();
                engine.load((String) gameList.getItemAtIndex(index));
            } else if (index == 1) {
                mainList.setVisible(true);
                gameList.setVisible(false);
                saveList.setVisible(false);
                settingList.setVisible(false);
                populateMainMenuList();
            }
        } else if (saveList.isVisible()) {
            int index = saveList.getIndex();
            if (index > 2) {
                try {
                    new AstralIO().saveGame(engine.getUniverse(), (String) saveList.getItemAtIndex(index));
                    populateSaveGameList();
                } catch (Exception ex) {
                    Logger.getLogger(MenuHomeWindow.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else if (index == 1) {
                mainList.setVisible(true);
                gameList.setVisible(false);
                saveList.setVisible(false);
                settingList.setVisible(false);
                populateMainMenuList();
            }
        } else if (settingList.isVisible()) {
            int index = settingList.getIndex();
            if (index > 2) {
                try {
                    String[] arr = command.split(":");
                    if (arr.length == 2) {
                        String set = arr[0];
                        if (set.matches("Planet Detail")) {
                            int curr = Integer.parseInt(arr[1].trim());
                            int pick = 0;
                            //find current in array
                            for (int a = 0; a < getUniverse().getSettings().RENDER_SIZE_OPTS.length; a++) {
                                if (curr == getUniverse().getSettings().RENDER_SIZE_OPTS[a]) {
                                    pick = a + 1;
                                    break;
                                }
                            }
                            //modulo
                            pick %= getUniverse().getSettings().RENDER_SIZE_OPTS.length;
                            //store
                            getUniverse().getSettings().RENDER_SIZE = getUniverse().getSettings().RENDER_SIZE_OPTS[pick];
                            //refresh
                            populateSettingList();
                        } else if (set.matches("Enable Sound Effects")) {
                            boolean curr = Boolean.parseBoolean(arr[1].trim());
                            getUniverse().getSettings().SOUND_EFFECTS = !curr;
                            populateSettingList();
                        } else if (set.matches("Enable Music")) {
                            boolean curr = Boolean.parseBoolean(arr[1].trim());
                            getUniverse().getSettings().MUSIC = !curr;
                            populateSettingList();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (index == 1) {
                mainList.setVisible(true);
                gameList.setVisible(false);
                saveList.setVisible(false);
                settingList.setVisible(false);
                populateMainMenuList();
            }
        }
    }

    private void populateMainMenuList() {
        mainList.clearList();
        //store text
        mainList.addToList("Select an Option");
        mainList.addToList("");
        mainList.addToList("New Game");
        mainList.addToList("");
        mainList.addToList("Load Quicksave");
        mainList.addToList("Load Game");
        if (engine.getUniverse() != null) {
            if (engine.getUniverse().isReady()) {
                mainList.addToList("");
                mainList.addToList("Save Game");
                mainList.addToList("");
                mainList.addToList("Settings");
                mainList.addToList("");
                mainList.addToList("Resume");
            }
        }
    }

    private void populateSaveGameList() {
        saveList.clearList();
        //add menu cruft
        saveList.addToList("Select a Game to Save");
        saveList.addToList("Return to Main Menu");
        saveList.addToList("");
        addSaves(saveList);
        saveList.addToList("Game " + countSaves());
    }

    private void populateLoadGameList() {
        gameList.clearList();
        //add menu cruft
        gameList.addToList("Select a Game to Load");
        gameList.addToList("Return to Main Menu");
        gameList.addToList("");
        addSaves(gameList);
    }

    private void populateSettingList() {
        settingList.clearList();
        //add menu cruft
        settingList.addToList("Select a Setting to Change");
        settingList.addToList("Return to Main Menu");
        settingList.addToList("");
        //options
        settingList.addToList("Planet Detail: " + getUniverse().getSettings().RENDER_SIZE);
        settingList.addToList("Enable Sound Effects: " + getUniverse().getSettings().SOUND_EFFECTS);
        settingList.addToList("Enable Music: " + getUniverse().getSettings().MUSIC);
        //settings

    }

    private Universe getUniverse() {
        return engine.getUniverse();
    }

    private int countSaves() {
        String path = System.getProperty("user.home") + "/.highalbedo/";
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        return listOfFiles.length;
    }

    private void addSaves(AstralList list) {
        //add all the files
        String path = System.getProperty("user.home") + AstralIO.SAVE_GAME_DIR;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                list.addToList(listOfFiles[i].getName());
            }
        }
    }
}
