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

import java.applet.AudioClip;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

/**
 *
 * @author nwiehoff
 */
public class Soundling {

    private String name;
    private Sound sound;
    private boolean loop;

    public Soundling(String name, String target, boolean loop) {
        this.name = name;
        this.loop = loop;
        sound = new Sound(target);
    }

    public String getName() {
        return name;
    }

    public boolean isPlaying() {
        return sound.playing;
    }

    public void reset() {
        sound.playing = false;
        sound.stop();
        sound.clip.setFramePosition(0);
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

        private Clip clip;
        private boolean playing = false;

        public Sound(String target) {
            try {
                clip = AstralIO.getClip(target);
                //setup listener so we know when it is safe to replay
                clip.addLineListener(new LineListener() {
                    public void update(LineEvent event) {
                        if (event.getType() == LineEvent.Type.STOP) {
                            playing = false;
                            clip.setFramePosition(0);
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void stop() {
            playing = false;
            clip.stop();
        }

        private void play() {
            playing = true;
            clip.start();
        }

        private void loop(final int count) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    playing = true;
                    clip.loop(count);
                }
            }).start();
        }
    }
}
