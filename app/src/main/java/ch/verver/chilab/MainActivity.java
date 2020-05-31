package ch.verver.chilab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AppState appState;

    private MutableLiveData<FragmentId> activeFragmentIdLiveData;
    private MutableLiveData<ImmutableList<Pos>> rectPuzzlePiecePositionsLiveData;
    private MutableLiveData<ImmutableList<Pos>> hexPuzzlePiecePositionsLiveData;

    private FragmentId currentFragmentId = FragmentId.NONE;
    private @Nullable Solution.Progress rectPuzzleProgress = null;
    private @Nullable Solution.Progress hexPuzzleProgress = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appState = new ViewModelProvider.AndroidViewModelFactory(getApplication()).create(AppState.class);
        // appState has already been restored from SharedPreferences in App.onCreate().
        appState = ((App) getApplication()).getAppState();

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
        rectPuzzlePiecePositionsLiveData = appState.getRectPuzzlePiecePositions();
        rectPuzzlePiecePositionsLiveData.observe(this, new Observer<ImmutableList<Pos>>() {
            @Override
            public void onChanged(ImmutableList<Pos> rectPuzzlePiecePositions) {
                onRectPiecePositionsChanged(rectPuzzlePiecePositions);

            }
        });
        hexPuzzlePiecePositionsLiveData = appState.getHexPuzzlePiecePositions();
        hexPuzzlePiecePositionsLiveData.observe(this, new Observer<ImmutableList<Pos>>() {
            @Override
            public void onChanged(ImmutableList<Pos> hexPuzzlePiecePositions) {
                onHexPiecePositionsChanged(hexPuzzlePiecePositions);
            }
        });
    }

    @Override
    protected void onPause() {
        ((App) getApplication()).getAppState().saveToSharedPreferences();
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
            case R.id.switch_to_instructions:
                activeFragmentIdLiveData.setValue(FragmentId.INSTRUCTIONS1);
                return true;

            case R.id.switch_to_rect_puzzle:
                activeFragmentIdLiveData.setValue(FragmentId.RECT_PUZZLE);
                return true;

            case R.id.switch_to_hex_puzzle:
                activeFragmentIdLiveData.setValue(FragmentId.HEX_PUZZLE);
                return true;

            case R.id.reset_puzzle_pieces:
                promptResetPiecePositions();

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void promptResetPiecePositions() {
        if (currentFragmentId != FragmentId.RECT_PUZZLE && currentFragmentId != FragmentId.HEX_PUZZLE) {
            LogUtil.w("Cannot reset puzzle pieces while active fragment is %s\n", currentFragmentId);
            return;
        }
        AlertDialog.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    switch (currentFragmentId) {
                        case RECT_PUZZLE:
                            rectPuzzlePiecePositionsLiveData.setValue(
                                    ImmutableList.copyOf(RectPuzzle.getRandomPiecePositions()));
                            break;
                        case HEX_PUZZLE:
                            hexPuzzlePiecePositionsLiveData.setValue(
                                    ImmutableList.copyOf(HexPuzzle.getRandomPiecePositions()));
                            break;
                    }
                }
            }
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_pieces_dialog_title)
                .setMessage(R.string.reset_pieces_dialog_message)
                .setNegativeButton(R.string.reset_pieces_dialog_cancel_button, onClickListener)
                .setPositiveButton(R.string.reset_pieces_dialog_reset_button, onClickListener)
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .create()
                .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.instructions1_next_button:
                activeFragmentIdLiveData.setValue(FragmentId.INSTRUCTIONS2);
                return;

            case R.id.instructions2_previous_button:
                activeFragmentIdLiveData.setValue(FragmentId.INSTRUCTIONS1);
                return;

            case R.id.instructions2_next_button:
            case R.id.replay_rect_puzzle:
                activeFragmentIdLiveData.setValue(FragmentId.RECT_PUZZLE);
                return;

            case R.id.play_hex_puzzle:
            case R.id.replay_hex_puzzle:
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

        if ((newFragmentId == FragmentId.RECT_PUZZLE && rectPuzzleProgress != null && rectPuzzleProgress.isSolved()) ||
            (newFragmentId == FragmentId.HEX_PUZZLE && hexPuzzleProgress != null && hexPuzzleProgress.isSolved())) {
            promptResetPiecePositions();
        }
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
            case INSTRUCTIONS1:
                return new Instructions1Fragment();
            case INSTRUCTIONS2:
                return new Instructions2Fragment();
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
