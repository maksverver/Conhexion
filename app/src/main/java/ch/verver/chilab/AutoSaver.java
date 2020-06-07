package ch.verver.chilab;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;

/** Automatically saves AppState onPause and a few seconds after a change is made. */
class AutoSaver implements LifecycleObserver, Runnable {

    // Minimum and maximum delay (in milliseconds) for auto-saving changes. This applies only to
    // change-triggered saving; on pause, saving happens immediately and unconditionally.
    //
    // When a change is detected, AutoSaver schedules a task to be run after MIN_DELAY_MILLIS. If a
    // subsequent change comes in, the task is rescheduled MIN_DELAY_MILLIS into the future.
    // However, the task is run no later than MAX_DELAY_MILLIS after the initial change.
    //
    // In effect, a single change will be saved no sooner than MIN_DELAY_MILLIS. If changes happen
    // continuously (with less than MIN_DELAY_MILLIS between consecutive changes) they are batched
    // up and the state is saved only once every MAX_DELAY_MILLIS.
    private static final long MIN_DELAY_MILLIS = 15_000;    // 15 seconds
    private static final long MAX_DELAY_MILLIS = 120_000;   // 120 seconds

    private final AppState appState;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long earliestUnsavedChangeUpTimeMillis = 0;
    private @Nullable FragmentId activeFragmentId;
    private @Nullable ImmutableList<Pos> rectPuzzlePiecePositions;
    private @Nullable ImmutableList<Pos> hexPuzzlePiecePositions;

    public static void attach(AppState appState, LifecycleOwner lifecycleOwner) {
        new AutoSaver(appState, lifecycleOwner);
    }

    private AutoSaver(AppState appState, LifecycleOwner lifecycleOwner) {
        this.appState = appState;

        appState.getActiveFragmentId().observe(lifecycleOwner,
                new Observer<FragmentId>() {
                    @Override
                    public void onChanged(FragmentId newValue) {
                        FragmentId oldValue = activeFragmentId;
                        activeFragmentId = newValue;
                        if (oldValue != null && oldValue != newValue) {
                            changed();
                        }
                    }
                });
        appState.getRectPuzzlePiecePositions().observe(lifecycleOwner,
                new Observer<ImmutableList<Pos>>() {
                    @Override
                    public void onChanged(ImmutableList<Pos> newValue) {
                        ImmutableList<Pos> oldValue = rectPuzzlePiecePositions;
                        rectPuzzlePiecePositions = newValue;
                        if (oldValue != null && !oldValue.equals(newValue)) {
                            changed();
                        }
                    }
                });
        appState.getHexPuzzlePiecePositions().observe(lifecycleOwner,
                new Observer<ImmutableList<Pos>>() {
                    @Override
                    public void onChanged(ImmutableList<Pos> newValue) {
                        ImmutableList<Pos> oldValue = hexPuzzlePiecePositions;
                        hexPuzzlePiecePositions = newValue;
                        if (oldValue != null && !oldValue.equals(newValue)) {
                            changed();
                        }
                    }
                });

        lifecycleOwner.getLifecycle().addObserver(this);
   }

    @Override
    public void run() {
        LogUtil.i("AutoSaver: saving");
        earliestUnsavedChangeUpTimeMillis = 0;
        appState.saveToSharedPreferences();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void pause() {
        LogUtil.i("AutoSaver: pause");
        handler.removeCallbacks(this);
        run();
    }

    private void changed() {
        LogUtil.i("AutoSaver: data changed");
        long upTimeMillis = SystemClock.uptimeMillis();
        if (earliestUnsavedChangeUpTimeMillis == 0) {
            earliestUnsavedChangeUpTimeMillis = upTimeMillis;
        }
        handler.removeCallbacks(this);
        handler.postAtTime(this,
                Math.min(upTimeMillis + MIN_DELAY_MILLIS,
                        earliestUnsavedChangeUpTimeMillis + MAX_DELAY_MILLIS));
    }
}
