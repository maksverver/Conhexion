package ch.verver.chilab;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

abstract class ColorFilters {
    static ColorFilter NEGATIVE =
            new ColorMatrixColorFilter(new ColorMatrix(new float[] {
                    -1,  0,  0,  0, 255, // red
                     0, -1,  0,  0, 255, // green
                     0,  0, -1,  0, 255, // blue
                     0,  0,  0,  1,   0  // alpha
            }));

    static ColorFilter LIGHTER =
            new ColorMatrixColorFilter(new ColorMatrix(new float[] {
                    1, 0, 0, 0, 64, // red
                    0, 1, 0, 0, 64, // green
                    0, 0, 1, 0, 64, // blue
                    0, 0, 0, 1,  0  // alpha
            }));

    static ColorFilter hueShift(float fraction) {
        float x =  3.0f * (float) (fraction - Math.floor(fraction));
        float[] matrix;
        if (x < 1.0f) {
            matrix = new float[] {
                    1.0f - x, 0,        x,        0, 0, // red
                    x,        1.0f - x, 0,        0, 0, // green
                    0,        x,        1.0f - x, 0, 0, // blue
                    0,        0,        0,        1, 0  // alpha
            };
        } else if (x < 2.0f) {
            x -= 1.0f;
            matrix = new float[] {
                    0,        x,        1.0f - x, 0, 0, // red
                    1.0f - x, 0,        x,        0, 0, // green
                    x,        1.0f - x, 0,        0, 0, // blue
                    0,        0,        0,        1, 0  // alpha
            };
        } else {
            x -= 2.0f;
            matrix = new float[] {
                    x,        1.0f - x, 0,        0, 0, // red
                    0,        x,        1.0f - x, 0, 0, // green
                    1.0f - x, 0,        x,        0, 0, // blue
                    0,        0,        0,        1, 0  // alpha
            };
        }
        return new ColorMatrixColorFilter(new ColorMatrix(matrix));
    }

    private ColorFilters() {}
}
