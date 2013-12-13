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
package org.ankh.unfall.planet.texgen.palette.ranges;

import java.awt.Color;
import org.ankh.unfall.planet.PlanetInformation;
import org.ankh.unfall.planet.texgen.palette.GaussianTerrainRange;
import org.ankh.unfall.planet.texgen.palette.TerrainPalette;
import org.ankh.unfall.planet.texgen.palette.TerrainRange;

public class LavaPalette extends TerrainPalette {

    public LavaPalette(PlanetInformation informations) {
        super(informations);
    }

    @Override
    public void initPalette() {

        TerrainRange upstairs = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel(), 4,
                -1, -1, 0, 0.8f,
                -258, -258, 50, 5.5f,
                Color.DARK_GRAY, Color.black);

        TerrainRange upupstairs = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 1.1f, 3,
                -1, -1, 5, 6,
                -15, 50, 25, 2,
                new Color(25, 23, 24), Color.black);

        TerrainRange very_deep_water = new GaussianTerrainRange(
                -1, getInformations().getWaterLevel(), 0, 50,
                -1, -1, 0, 5,
                -258, 100, 50, 5,
                Color.RED, Color.white);

        TerrainRange deep_water = new GaussianTerrainRange(
                -1, getInformations().getWaterLevel(), getInformations().getWaterLevel() * .5f, 50,
                -1, -1, 0, 5,
                -258, 100, 50, 5,
                Color.ORANGE, Color.white);

        TerrainRange light_water = new GaussianTerrainRange(
                -1, getInformations().getWaterLevel(), getInformations().getWaterLevel(), 5,
                -1, -1, 0, 1.0f,
                -258, 100, 50, 5,
                new Color(255, 35, 78), Color.white);

        TerrainRange desert = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 1.2f, 4,
                -1, -1, 0, 2f,
                -258, 100, 0, 3.0f,
                new Color(45, 43, 44), Color.black);

        TerrainRange desert_dune = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 1.3f, 4,
                -1, -1, 0, 2f,
                -1, 100, 50, 3.0f,
                new Color(35, 33, 34), Color.black);

        TerrainRange moutain = new GaussianTerrainRange(
                getInformations().getWaterLevel(), -1, getInformations().getWaterLevel() * 2f, 4,
                -1, -1, 10, 1.5f,
                -258, -258, 5, 3,
                Color.LIGHT_GRAY, Color.black);

        attachTerrainRange(upstairs);
        attachTerrainRange(upupstairs);
        attachTerrainRange(deep_water);
        attachTerrainRange(light_water);
        attachTerrainRange(very_deep_water);
        attachTerrainRange(desert);
        attachTerrainRange(desert_dune);
        attachTerrainRange(moutain);
    }
}
