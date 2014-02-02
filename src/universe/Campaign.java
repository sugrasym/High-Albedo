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
 * A complex, scripted, mission that the player can embark on. Has multiple
 * nodes involved.
 */
package universe;

import java.io.Serializable;

/**
 *
 * @author nwiehoff
 */
public class Campaign implements Serializable {
    private String name;
    
    public Campaign(String name) {
        this.name = name;
        System.out.println("Starting campaign: "+name);
    }
    
    public void periodicUpdate(double tpf) {
        //TODO
    }
    
    public String getName() {
        return name;
    }
}
