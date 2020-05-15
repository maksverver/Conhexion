package ch.verver.chilab;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

class Preferences {
    public static final String SHARED_PREFERENCES_NAME = "main-prefs";
    public static final String RECT_PIECES_KEY = "rect-pieces";
    public static final String HEX_PIECES_KEY = "hex-pieces";

    public static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
    }

    private Preferences() {}
}
