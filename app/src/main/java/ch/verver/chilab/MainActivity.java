package ch.verver.chilab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity
        implements HexPiecePositionsChangedListener, RectPiecePositionsChangedListener, View.OnClickListener {

    private AppState appState;

    private Solution.Progress rectPuzzleProgress;
    private Solution.Progress hexPuzzleProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // appState has already been restored from SharedPreferences in App.onCreate().
        appState = App.getAppState();

        updateRectPuzzleProgress();
        updateHexPuzzleProgress();

        if (savedInstanceState == null && getIntent() != null) {
            // Restore app state from intent extras. This allows overriding part of the app state
            // with an intent, for debugging purposes. See DEBUGGING.txt for examples.
            appState.restoreFromIntentExtras(getIntent().getExtras());
        }

        FragmentId activeFragmentId = appState.getActiveFragmentId();
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(activeFragmentId.name());
        if (fragment != null && fragment.isVisible()) {
            LogUtil.i("MainActivity: reusing existing fragment");
        } else {
            setActiveFragment(activeFragmentId);
        }
    }

    @Override
    protected void onPause() {
        App.getAppState().saveToSharedPreferences(this);
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
                setActiveFragment(FragmentId.RECT_PUZZLE);
                return true;

            case R.id.switch_to_hex_puzzle:
                setActiveFragment(FragmentId.HEX_PUZZLE);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_hex_puzzle:
                setActiveFragment(FragmentId.HEX_PUZZLE);
                return;
            case R.id.replay_rect_puzzle:
                // TODO: should ask to reset pieces
                setActiveFragment(FragmentId.RECT_PUZZLE);
                return;
            case R.id.replay_hex_puzzle:
                // TODO: should ask to reset pieces
                setActiveFragment(FragmentId.HEX_PUZZLE);
                return;
        }
        LogUtil.w("Unknown view clicked: %s", v);
    }

    private void setActiveFragment(FragmentId newFragmentId) {
        setActiveFragment(newFragmentId, 0, 0);
    }

    private void setActiveFragment(FragmentId newFragmentId, int enterAnimId, int exitAnimId) {
        Fragment newFragment = createFragment(newFragmentId);
        if (newFragment == null) {
            LogUtil.e("Cannot switch to nonexistent fragment %s!", newFragmentId);
            return;
        }
        LogUtil.i("Switching main fragment to %s", newFragmentId);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        if (enterAnimId != 0 && exitAnimId != 0) {
            transaction.setCustomAnimations(enterAnimId, exitAnimId);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        transaction.replace(R.id.fragment_container, newFragment, newFragmentId.name());
        transaction.commit();
        appState.setActiveFragmentId(newFragmentId);
    }

    @Override
    public void rectPiecePositionsChanged(RectGridView view) {
        boolean wasSolved = rectPuzzleProgress.isSolved();
        appState.setRectPuzzlePiecePositions(view.getPiecePositions());
        updateRectPuzzleProgress();
        if (rectPuzzleProgress.isSolved() && !wasSolved) {
            LogUtil.i("Rect puzzle is solved!");
            RectPuzzleFragment rectPuzzleFragment =
                    (RectPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.RECT_PUZZLE.name());
            if (rectPuzzleFragment != null) {
                RectGridView rectGridView = rectPuzzleFragment.getRectGridView();
                if (rectGridView != null) {
                    rectGridView.startVictoryAnimation();
                }
            }
            rectPuzzleFragment.getRectGridView().startVictoryAnimation();
            setActiveFragment(FragmentId.RECT_PUZZLE_SOLVED, R.anim.puzzle_solved_fade_in, R.anim.puzzle_solved_fade_out);
        }
    }

    @Override
    public void hexPiecePositionsChanged(HexGridView view) {
        boolean wasSolved = hexPuzzleProgress.isSolved();
        appState.setHexPuzzlePiecePositions(view.getPiecePositions());
        updateHexPuzzleProgress();
        if (hexPuzzleProgress.isSolved() && !wasSolved) {
            LogUtil.i("Hex puzzle is solved!");
            HexPuzzleFragment hexPuzzleFragment =
                    (HexPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.HEX_PUZZLE.name());
            if (hexPuzzleFragment != null) {
                HexGridView hexGridView = hexPuzzleFragment.getHexGridView();
                if (hexGridView != null) {
                    hexGridView.startVictoryAnimation();
                }
            }
            setActiveFragment(FragmentId.HEX_PUZZLE_SOLVED, R.anim.puzzle_solved_fade_in, R.anim.puzzle_solved_fade_out);
        }
    }

    private void updateRectPuzzleProgress() {
        PiecePositionIndex piecePositions = new PiecePositionIndex(appState.getRectPuzzlePiecePositions());
        rectPuzzleProgress = Solution.calculateProgress(piecePositions, RectDirection.values());
    }

    private void updateHexPuzzleProgress() {
        PiecePositionIndex piecePositions = new PiecePositionIndex(appState.getHexPuzzlePiecePositions());
        hexPuzzleProgress = Solution.calculateProgress(piecePositions, HexDirection.values());
    }

    @Nullable
    private Fragment createFragment(FragmentId id) {
        switch (id) {
            case RECT_PUZZLE:
                return new RectPuzzleFragment(appState.getRectPuzzlePiecePositions());
            case RECT_PUZZLE_SOLVED:
                return new RectPuzzleSolvedFragment();
            case HEX_PUZZLE:
                return new HexPuzzleFragment(appState.getHexPuzzlePiecePositions());
            case HEX_PUZZLE_SOLVED:
                return new HexPuzzleSolvedFragment();
        }
        return null;
    }
}
