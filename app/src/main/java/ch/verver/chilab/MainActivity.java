package ch.verver.chilab;

import androidx.annotation.NonNull;
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
    private RectPuzzleFragment rectPuzzleFragment;
    private RectPuzzleSolvedFragment rectPuzzleSolvedFragment;
    private HexPuzzleFragment hexPuzzleFragment;
    private HexPuzzleSolvedFragment hexPuzzleSolvedFragment;

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

        rectPuzzleFragment = (RectPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.RECT_PUZZLE.name());
        if (rectPuzzleFragment == null) {
            rectPuzzleFragment = new RectPuzzleFragment(appState.getRectPuzzlePiecePositions());
        }

        rectPuzzleSolvedFragment = (RectPuzzleSolvedFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.RECT_PUZZLE_SOLVED.name());
        if (rectPuzzleSolvedFragment == null) {
            rectPuzzleSolvedFragment = new RectPuzzleSolvedFragment();
        }

        hexPuzzleFragment = (HexPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.HEX_PUZZLE.name());
        if (hexPuzzleFragment == null) {
            hexPuzzleFragment = new HexPuzzleFragment(appState.getHexPuzzlePiecePositions());
        }

        hexPuzzleSolvedFragment = (HexPuzzleSolvedFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.HEX_PUZZLE_SOLVED.name());
        if (hexPuzzleSolvedFragment == null) {
            hexPuzzleSolvedFragment = new HexPuzzleSolvedFragment();
        }

        setActiveFragment(appState.getActiveFragmentId());
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
        Fragment newFragment = getFragment(newFragmentId);
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
        // Don't add the first transaction to the back-stack, otherwise pressing "Back" while the
        // very first fragment is being shown will transition to an empty activity!
        if (!fragmentManager.getFragments().isEmpty()) {
            // Add fragment to the back stack. I don't really want to do this, but without it the
            // app crashes with "Restarter must be created only during owner's initialization stage"
            // if a transaction is started while the previous one is still in progress.
            transaction.addToBackStack(newFragmentId.name());
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
            hexPuzzleFragment.getHexGridView().startVictoryAnimation();
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

    private Fragment getFragment(FragmentId id) {
        switch (id) {
            case RECT_PUZZLE:
                return rectPuzzleFragment;
            case RECT_PUZZLE_SOLVED:
                return rectPuzzleSolvedFragment;
            case HEX_PUZZLE:
                return hexPuzzleFragment;
            case HEX_PUZZLE_SOLVED:
                return hexPuzzleSolvedFragment;
        }
        return null;
    }
}
