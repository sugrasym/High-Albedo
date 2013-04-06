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
 * Pairs a sound with additional information about the clip
 */
package lib;

import javax.sound.sampled.Clip;
import org.lwjgl.util.WaveData;

/**
 *
 * @author nwiehoff
 */
public class Soundling {

    private String name;
    private Sound sound;
    private String target;
    private boolean loop;

    public Soundling(String name, String target, boolean loop) {
        this.name = name;
        this.loop = loop;
        this.target = target;
        sound = new Sound(target);
    }

    public String getName() {
        return name;
    }

    public boolean isPlaying() {
        return sound.playing;
    }

    /*
     * controls
     */
    public void play() {
        if (!loop) {
            sound.play();
        } else {
            sound.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stop() {
        sound.stop();
    }

    /*
     * sound data container
     */
    private class Sound {

        private WaveData waveFile;
        private boolean playing = false;

        public Sound(String target) {
            try {
                waveFile  = WaveData.create(AstralIO.RESOURCE_DIR+"/"+target);
                //setup listener so we know when it is safe to replay
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void stop() {
            playing = false;
        }

        private void play() {
            playing = true;
        }

        private void loop(final int count) {
            playing = true;
            //TODO LOOPING
        }
    }

    public String toString() {
        return name + ", " + target + ", " + loop;
    }
}
