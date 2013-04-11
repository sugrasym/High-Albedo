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
 * Message frame for sending queries to the player. It is up to the sender
 * to monitor for replies.
 */
package lib;

import celestial.Ship.Ship;
import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author nwiehoff
 */
public class AstralMessage implements Serializable {

    private Ship sender;
    private String message;
    private ArrayList<Binling> choices;
    private boolean repliedTo = false;

    public AstralMessage(Ship sender, String message, ArrayList<Binling> choices) {
        this.sender = sender;
        this.message = message;
        this.choices = choices;
    }

    public Ship getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public ArrayList<Binling> getChoices() {
        return choices;
    }

    public boolean isRepliedTo() {
        return repliedTo;
    }

    public void setRepliedTo(boolean repliedTo) {
        this.repliedTo = repliedTo;
    }

    public void reply(Binling choice) {
        if (sender != null) {
            sender.recieveReply(this, choice);
        }
        repliedTo = true;
    }
}
