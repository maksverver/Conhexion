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

    private ColorFilters() {}
}
