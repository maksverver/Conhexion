package ch.verver.chilab;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/** Holds the shared app state, which is persisted. */
class AppState {
    private static final String SHARED_PREFERENCES_NAME = "main-prefs";
    private static final String RECT_PIECES_KEY = "rect-pieces";
    private static final String HEX_PIECES_KEY = "hex-pieces";
    private static final String ACTIVE_FRAGMENT_ID_KEY = "active-fragment";

    private FragmentId activeFragmentId = FragmentId.NONE;
    private ArrayList<Pos> rectPuzzlePiecePositions = new ArrayList<>();
    private ArrayList<Pos> hexPuzzlePiecePositions = new ArrayList<>();

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    FragmentId getActiveFragmentId() {
        return activeFragmentId;
    }

    void setActiveFragmentId(FragmentId newValue) {
        activeFragmentId = newValue;
        LogUtil.d("AppState: %s = %s", ACTIVE_FRAGMENT_ID_KEY, activeFragmentId.name());
    }

    ArrayList<Pos> getRectPuzzlePiecePositions() {
        return new ArrayList<>(rectPuzzlePiecePositions);
    }

    void setRectPuzzlePiecePositions(ArrayList<Pos> newValue) {
        rectPuzzlePiecePositions = new ArrayList<>(newValue);
        LogUtil.d("AppState: %s = %s", RECT_PIECES_KEY, RectPuzzle.encode(rectPuzzlePiecePositions));
    }

    ArrayList<Pos> getHexPuzzlePiecePositions() {
        return new ArrayList<>(hexPuzzlePiecePositions);
    }

    void setHexPuzzlePiecePositions(ArrayList<Pos> newValue) {
        hexPuzzlePiecePositions = new ArrayList<>(newValue);
        LogUtil.d("AppState: %s = %s", HEX_PIECES_KEY, HexPuzzle.encode(hexPuzzlePiecePositions));
    }

    void saveToSharedPreferences(Context context) {
        LogUtil.i("AppState: saving to shared preferences");
        getSharedPreferences(context).edit()
            .putString(RECT_PIECES_KEY, RectPuzzle.encode(rectPuzzlePiecePositions))
            .putString(HEX_PIECES_KEY, HexPuzzle.encode(hexPuzzlePiecePositions))
            .putString(ACTIVE_FRAGMENT_ID_KEY, activeFragmentId.name())
            .apply();
    }

    void restoreFromIntentExtras(@Nullable Bundle extras) {
        if (extras == null) {
            return;
        }
        if (restoreActiveFragmentId(extras)) {
            LogUtil.i("AppState: restored active fragment from intent extras");
        }
        if (restoreRectPuzzlePiecePositions(extras)) {
            LogUtil.i("AppState: restored rect puzzle pieces from intent extras");
        }
        if (restoreHexPuzzlePiecePositions(extras)) {
            LogUtil.i("AppState: restored hex puzzle pieces from intent extras");
        }
    }

    boolean restoreFromSharedPreferences(Context context) {
        SharedPreferences prefs = getSharedPreferences(context);
        boolean success = restoreActiveFragmentId(prefs);
        success &= restoreRectPuzzlePiecePositions(prefs);
        success &= restoreHexPuzzlePiecePositions(prefs);
        LogUtil.i("AppState: loaded from shared preferences [success=%s]", success);
        return success;
    }

    void fillInMissingFields() {
        if (rectPuzzlePiecePositions.isEmpty()) {
            rectPuzzlePiecePositions = RectPuzzle.getRandomPiecePositions();
        }
        if (hexPuzzlePiecePositions.isEmpty()) {
            hexPuzzlePiecePositions = HexPuzzle.getRandomPiecePositions();
        }
        if (activeFragmentId == FragmentId.NONE) {
            activeFragmentId = FragmentId.RECT_PUZZLE;
        }
    }

    private boolean restoreRectPuzzlePiecePositions(SharedPreferences prefs) {
        return restoreRectPuzzlePiecePositions(prefs.getString(RECT_PIECES_KEY, null));
    }

    private boolean restoreRectPuzzlePiecePositions(Bundle extras) {
        return restoreRectPuzzlePiecePositions(extras.getString(RECT_PIECES_KEY, null));
    }

    private boolean restoreRectPuzzlePiecePositions(@Nullable String encoded) {
        if (encoded == null) {
            return false;
        }
        ArrayList<Pos> newValue = RectPuzzle.decode(encoded);
        if (newValue == null) {
            return false;
        }
        rectPuzzlePiecePositions = newValue;
        return true;
    }

    private boolean restoreHexPuzzlePiecePositions(SharedPreferences prefs) {
        return restoreHexPuzzlePiecePositions(prefs.getString(HEX_PIECES_KEY, null));
    }

    private boolean restoreHexPuzzlePiecePositions(Bundle extras) {
        return restoreHexPuzzlePiecePositions(extras.getString(HEX_PIECES_KEY, null));
    }

    private boolean restoreHexPuzzlePiecePositions(@Nullable String encoded) {
        if (encoded == null) {
            return false;
        }
        ArrayList<Pos> newValue = HexPuzzle.decode(encoded);
        if (newValue == null) {
            return false;
        }
        hexPuzzlePiecePositions = newValue;
        return true;
    }

    @Nullable
    private static FragmentId parseFragmentId(@Nullable String name) {
        if (name == null) {
            return null;
        }
        try {
            return FragmentId.valueOf(name);
        } catch (IllegalArgumentException e) {
            LogUtil.e(e, "Could not parse fragment id \"%s\"", name);
            return null;
        }
    }

    private boolean restoreActiveFragmentId(SharedPreferences prefs) {
        return restoreActiveFragmentId(prefs.getString(ACTIVE_FRAGMENT_ID_KEY, null));
    }

    private boolean restoreActiveFragmentId(Bundle extras) {
        return restoreActiveFragmentId(extras.getString(ACTIVE_FRAGMENT_ID_KEY, null));
    }

    private boolean restoreActiveFragmentId(@Nullable String encoded) {
        FragmentId newValue = parseFragmentId(encoded);
        if (newValue == null) {
            return false;
        }
        activeFragmentId = newValue;
        return true;
    }

}
