package ch.verver.conhexion;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;

/** Holds the shared app state, which is persisted. */
public class AppState extends AndroidViewModel {
    private static final String SHARED_PREFERENCES_NAME = "main-prefs";
    private static final String RECT_PIECES_KEY = "rect-pieces";
    private static final String HEX_PIECES_KEY = "hex-pieces";
    private static final String ACTIVE_FRAGMENT_ID_KEY = "active-fragment";
    private static final String ERROR_VISIBILITY_KEY = "error-visibility";

    private MutableLiveData<FragmentId> activeFragmentId = new MutableLiveData<FragmentId>() {
        @Override
        public void setValue(FragmentId value) {
            if (value == null || value == FragmentId.NONE) {
                throw new IllegalArgumentException();
            }
            super.setValue(value);
            LogUtil.d("AppState: %s = %s", ACTIVE_FRAGMENT_ID_KEY, value.name());
        }
    };
    private MutableLiveData<ImmutableList<Pos>> rectPuzzlePiecePositions = new MutableLiveData<ImmutableList<Pos>>() {
        @Override
        public void setValue(ImmutableList<Pos> value) {
            if (!RectPuzzle.validate(value)) {
                throw new IllegalArgumentException();
            }
            super.setValue(value);
            LogUtil.d("AppState: %s = %s", RECT_PIECES_KEY, RectPuzzle.encode(value));
        }
    };
    private MutableLiveData<ImmutableList<Pos>> hexPuzzlePiecePositions = new MutableLiveData<ImmutableList<Pos>>() {
        @Override
        public void setValue(ImmutableList<Pos> value) {
            if (!HexPuzzle.validate(value)) {
                throw new IllegalArgumentException();
            }
            super.setValue(value);
            LogUtil.d("AppState: %s = %s", HEX_PIECES_KEY, HexPuzzle.encode(value));
        }
    };
    private MutableLiveData<ErrorVisibility> errorVisibility = new MutableLiveData<ErrorVisibility>();

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /** Public constructor, called by the framework. Do not call this directly! */
    public AppState(Application application) {
        super(application);

        // Restore values from shared preferences. Since this does disk I/O, it strictly speaking
        // should be done in a background thread, but it's probably fast enough that it doesn't make
        // a noticeable difference in practice.
        restoreFromSharedPreferences();
        fillInMissingFields();
    }

    public MutableLiveData<FragmentId> getActiveFragmentId() {
        return activeFragmentId;
    }

    public MutableLiveData<ImmutableList<Pos>> getRectPuzzlePiecePositions() {
        return rectPuzzlePiecePositions;
    }

    public MutableLiveData<ImmutableList<Pos>> getHexPuzzlePiecePositions() {
        return hexPuzzlePiecePositions;
    }

    public MutableLiveData<ErrorVisibility> getErrorVisibility() {
        return errorVisibility;
    }

    @MainThread
    public void saveToSharedPreferences() {
        LogUtil.i("AppState: saving to shared preferences");
        getSharedPreferences(getApplication()).edit()
            .putString(RECT_PIECES_KEY, RectPuzzle.encode(rectPuzzlePiecePositions.getValue()))
            .putString(HEX_PIECES_KEY, HexPuzzle.encode(hexPuzzlePiecePositions.getValue()))
            .putString(ACTIVE_FRAGMENT_ID_KEY, activeFragmentId.getValue().name())
            .putString(ERROR_VISIBILITY_KEY, encodeErrorVisibility(errorVisibility.getValue()))
            .apply();
    }

    @MainThread
    public void restoreFromIntentExtras(@Nullable Bundle extras) {
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
        if (restoreErrorVisibility(extras)) {
            LogUtil.i("AppState: restored error visibility from intent extras");
        }
    }

    private void restoreFromSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(getApplication());
        restoreActiveFragmentId(prefs);
        restoreRectPuzzlePiecePositions(prefs);
        restoreHexPuzzlePiecePositions(prefs);
        restoreErrorVisibility(prefs);
        LogUtil.i("AppState: loaded from shared preferences");
    }

    private void fillInMissingFields() {
        if (rectPuzzlePiecePositions.getValue() == null) {
            LogUtil.i("AppState: randomly initializing %s", RECT_PIECES_KEY);
            rectPuzzlePiecePositions.setValue(ImmutableList.copyOf(RectPuzzle.getRandomPiecePositions()));
        }
        if (hexPuzzlePiecePositions.getValue() == null) {
            LogUtil.i("AppState: randomly initializing %s", HEX_PIECES_KEY);
            hexPuzzlePiecePositions.setValue(ImmutableList.copyOf(HexPuzzle.getRandomPiecePositions()));
        }
        if (activeFragmentId.getValue() == null) {
            LogUtil.i("AppState: initializing %s", ACTIVE_FRAGMENT_ID_KEY);
            activeFragmentId.setValue(FragmentId.INSTRUCTIONS1);
        }
        if (errorVisibility.getValue() == null) {
            LogUtil.i("AppState: initializing %s", ERROR_VISIBILITY_KEY);
            errorVisibility.setValue(ErrorVisibility.VISIBLE);
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
        rectPuzzlePiecePositions.setValue(ImmutableList.copyOf(newValue));
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
        hexPuzzlePiecePositions.setValue(ImmutableList.copyOf(newValue));
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
        activeFragmentId.setValue(newValue);
        return true;
    }

    @Nullable
    private static String encodeErrorVisibility(@Nullable ErrorVisibility value) {
        return value == null ? null : value.name();
    }

    private boolean restoreErrorVisibility(SharedPreferences prefs) {
        return restoreErrorVisibility(prefs.getString(ERROR_VISIBILITY_KEY, null));
    }

    private boolean restoreErrorVisibility(Bundle extras) {
        return restoreErrorVisibility(extras.getString(ERROR_VISIBILITY_KEY, null));
    }

    private boolean restoreErrorVisibility(@Nullable String encoded) {
        if (encoded == null) {
            return false;
        }
        ErrorVisibility value;
        try {
            value = ErrorVisibility.valueOf(encoded);
        } catch (IllegalArgumentException e) {
            LogUtil.w("Invalid error visibility: \"%s\"", encoded);
            return false;
        }
        errorVisibility.setValue(value);
        return true;
    }
}
