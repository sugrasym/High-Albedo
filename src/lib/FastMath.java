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
 * Uses look up tables and is therefore significantly faster than the Math.atan2
 * implementation.
 * 
 * Modified from an implementation by 'Riven' in 2006.
 */
package lib;

public class FastMath {

    private static final int ATAN2_BITS = 7;
    private static final int ATAN2_BITS2 = ATAN2_BITS << 1;
    private static final int ATAN2_MASK = ~(-1 << ATAN2_BITS2);
    private static final int ATAN2_COUNT = ATAN2_MASK + 1;
    private static final int ATAN2_DIM = (int) Math.sqrt(ATAN2_COUNT);
    private static final float INV_ATAN2_DIM_MINUS_1 = 1.0f / (ATAN2_DIM - 1);
    private static final float DEG = 180.0f / (float) Math.PI;
    private static final float[] ATAN2 = new float[ATAN2_COUNT];

    static {
        for (int i = 0; i < ATAN2_DIM; i++) {
            for (int j = 0; j < ATAN2_DIM; j++) {
                float x0 = (float) i / ATAN2_DIM;
                float y0 = (float) j / ATAN2_DIM;

                ATAN2[j * ATAN2_DIM + i] = (float) Math.atan2(y0, x0);
            }
        }
    }

    public static float atan2Deg(float y, float x) {
        return FastMath.atan2(y, x) * DEG;
    }

    public static float atan2DegStrict(float y, float x) {
        return (float) Math.atan2(y, x) * DEG;
    }

    public static float atan2(double y, double x) {
        return atan2((float) y, (float) x);
    }

    public static float atan2(float y, float x) {
        float add, mul;

        if (x < 0.0f) {
            if (y < 0.0f) {
                x = -x;
                y = -y;

                mul = 1.0f;
            } else {
                x = -x;
                mul = -1.0f;
            }

            add = -3.141592653f;
        } else {
            if (y < 0.0f) {
                y = -y;
                mul = -1.0f;
            } else {
                mul = 1.0f;
            }

            add = 0.0f;
        }

        float invDiv = 1.0f / (((x < y) ? y : x) * INV_ATAN2_DIM_MINUS_1);

        int xi = (int) (x * invDiv);
        int yi = (int) (y * invDiv);

        return (ATAN2[yi * ATAN2_DIM + xi] + add) * mul;
    }
}
