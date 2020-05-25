package ch.verver.chilab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AppState appState;

    private MutableLiveData<FragmentId> activeFragmentIdLiveData;

    private FragmentId currentFragmentId = FragmentId.NONE;
    private @Nullable Solution.Progress rectPuzzleProgress;
    private @Nullable Solution.Progress hexPuzzleProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // appState has already been restored from SharedPreferences in App.onCreate().
        appState = App.getAppState();

        if (savedInstanceState == null && getIntent() != null) {
            // Restore app state from intent extras. This allows overriding part of the app state
            // with an intent, for debugging purposes. See DEBUGGING.txt for examples.
            appState.restoreFromIntentExtras(getIntent().getExtras());
        }

        activeFragmentIdLiveData = appState.getActiveFragmentId();
        FragmentId fragmentId = activeFragmentIdLiveData.getValue();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentId.name());
        if (fragment != null && fragment.isAdded() && !fragment.isRemoving()) {
            LogUtil.i("MainActivity: reusing existing fragment");
            this.currentFragmentId = fragmentId;
        }
        activeFragmentIdLiveData.observe(this, new Observer<FragmentId>() {
            @Override
            public void onChanged(FragmentId fragmentId) {
                onActiveFragmentIdChanged(fragmentId);
            }
        });
        appState.getRectPuzzlePiecePositions().observe(this, new Observer<ImmutableList<Pos>>() {
            @Override
            public void onChanged(ImmutableList<Pos> rectPuzzlePiecePositions) {
                onRectPiecePositionsChanged(rectPuzzlePiecePositions);

            }
        });
        appState.getHexPuzzlePiecePositions().observe(this, new Observer<ImmutableList<Pos>>() {
            @Override
            public void onChanged(ImmutableList<Pos> hexPuzzlePiecePositions) {
                onHexPiecePositionsChanged(hexPuzzlePiecePositions);
            }
        });
    }

    @Override
    protected void onPause() {
        App.getAppState().saveToSharedPreferences();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_to_rect_puzzle:
                activeFragmentIdLiveData.setValue(FragmentId.RECT_PUZZLE);
                return true;

            case R.id.switch_to_hex_puzzle:
                activeFragmentIdLiveData.setValue(FragmentId.HEX_PUZZLE);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_hex_puzzle:
                activeFragmentIdLiveData.setValue(FragmentId.HEX_PUZZLE);
                return;
            case R.id.replay_rect_puzzle:
                // TODO: should ask to reset pieces
                activeFragmentIdLiveData.setValue(FragmentId.RECT_PUZZLE);
                return;
            case R.id.replay_hex_puzzle:
                // TODO: should ask to reset pieces
                activeFragmentIdLiveData.setValue(FragmentId.HEX_PUZZLE);
                return;
        }
        LogUtil.w("Unknown view clicked: %s", v);
    }

    private void onActiveFragmentIdChanged(FragmentId newFragmentId) {
        if (newFragmentId == currentFragmentId) {
            LogUtil.i("Ignoring switch to fragment %s, which is already current.", newFragmentId);
            return;
        }
        Fragment newFragment = createFragment(newFragmentId);
        if (newFragment == null) {
            LogUtil.e("Cannot switch to nonexistent fragment %s!", newFragmentId);
            return;
        }
        LogUtil.i("Switching main fragment to %s", newFragmentId);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if ((currentFragmentId == FragmentId.RECT_PUZZLE && newFragmentId == FragmentId.RECT_PUZZLE_SOLVED) ||
            (currentFragmentId == FragmentId.HEX_PUZZLE && newFragmentId == FragmentId.HEX_PUZZLE_SOLVED)) {
            transaction.setCustomAnimations(R.anim.puzzle_solved_fade_in, R.anim.puzzle_solved_fade_out);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        transaction.replace(R.id.fragment_container, newFragment, newFragmentId.name());
        transaction.commit();
        currentFragmentId = newFragmentId;
    }

    private void onRectPiecePositionsChanged(ImmutableList<Pos> piecePositions) {
        Solution.Progress oldProgress = rectPuzzleProgress;
        rectPuzzleProgress = Solution.calculateProgress(piecePositions, RectDirection.values());
        if (rectPuzzleProgress.isSolved() && oldProgress != null && !oldProgress.isSolved()) {
            LogUtil.i("Rect puzzle is solved!");
            RectPuzzleFragment rectPuzzleFragment =
                    (RectPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.RECT_PUZZLE.name());
            if (rectPuzzleFragment != null) {
                RectGridView rectGridView = rectPuzzleFragment.getRectGridView();
                if (rectGridView != null) {
                    rectGridView.startVictoryAnimation();
                }
            }
            activeFragmentIdLiveData.setValue(FragmentId.RECT_PUZZLE_SOLVED);
        }
    }

    private void onHexPiecePositionsChanged(ImmutableList<Pos> piecePositions) {
        Solution.Progress oldProgress = hexPuzzleProgress;
        hexPuzzleProgress = Solution.calculateProgress(piecePositions, HexDirection.values());
        if (hexPuzzleProgress.isSolved() && oldProgress != null && !oldProgress.isSolved()) {
            LogUtil.i("Hex puzzle is solved!");
            HexPuzzleFragment hexPuzzleFragment =
                    (HexPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.HEX_PUZZLE.name());
            if (hexPuzzleFragment != null) {
                HexGridView hexGridView = hexPuzzleFragment.getHexGridView();
                if (hexGridView != null) {
                    hexGridView.startVictoryAnimation();
                }
            }
            activeFragmentIdLiveData.setValue(FragmentId.HEX_PUZZLE_SOLVED);
        }
    }

    @Nullable
    private Fragment createFragment(FragmentId id) {
        switch (id) {
            case RECT_PUZZLE:
                return new RectPuzzleFragment();
            case RECT_PUZZLE_SOLVED:
                return new RectPuzzleSolvedFragment();
            case HEX_PUZZLE:
                return new HexPuzzleFragment();
            case HEX_PUZZLE_SOLVED:
                return new HexPuzzleSolvedFragment();
        }
        return null;
    }
}
