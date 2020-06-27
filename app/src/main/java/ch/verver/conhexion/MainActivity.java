package ch.verver.conhexion;

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
import android.view.animation.TranslateAnimation;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private AppState appState;

    private MutableLiveData<FragmentId> activeFragmentIdLiveData;
    private MutableLiveData<ImmutableList<Pos>> rectPuzzlePiecePositionsLiveData;
    private MutableLiveData<ImmutableList<Pos>> hexPuzzlePiecePositionsLiveData;

    private FragmentId currentFragmentId = FragmentId.NONE;
    private @Nullable Solution.Progress rectPuzzleProgress = null;
    private @Nullable Solution.Progress hexPuzzleProgress = null;

    private View solvedView;
    private boolean solvedViewShown = false;

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

        AutoSaver.attach(appState, this);

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

        solvedView = findViewById(R.id.solved_view);
        solvedView.setVisibility(View.INVISIBLE);
        solvedViewShown = false;
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
                return true;

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

            case R.id.solved_reset_pieces_button:
                promptResetPiecePositions();
                return;

            case R.id.solved_continue_button:
                if (currentFragmentId == FragmentId.RECT_PUZZLE) {
                    activeFragmentIdLiveData.setValue(FragmentId.RECT_PUZZLE_SOLVED);
                    return;
                }
                if (currentFragmentId == FragmentId.HEX_PUZZLE) {
                    activeFragmentIdLiveData.setValue(FragmentId.HEX_PUZZLE_SOLVED);
                    return;
                }

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
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.replace(R.id.fragment_container, newFragment, newFragmentId.name());
        transaction.commit();
        currentFragmentId = newFragmentId;
        solvedView.setVisibility(View.INVISIBLE);
        solvedViewShown = false;
    }

    private void onRectPiecePositionsChanged(ImmutableList<Pos> piecePositions) {
        Solution.Progress oldRectPuzzleProgress = rectPuzzleProgress;
        rectPuzzleProgress = Solution.calculateProgress(piecePositions, RectDirection.VALUES);
        if (oldRectPuzzleProgress == null) {
            // This is the first time progress is calculated, probably because we first loaded
            // this view. Don't update the solved view in this case.
            return;
        }
        if (rectPuzzleProgress.isSolved() && !oldRectPuzzleProgress.isSolved()) {
            LogUtil.i("Rect puzzle is solved!");
            RectPuzzleFragment rectPuzzleFragment =
                    (RectPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.RECT_PUZZLE.name());
            if (rectPuzzleFragment != null) {
                RectGridView rectGridView = rectPuzzleFragment.getRectGridView();
                if (rectGridView != null) {
                    rectGridView.startVictoryAnimation();
                }
                showSolvedView();
            }
        }

        if (!rectPuzzleProgress.isSolved() && oldRectPuzzleProgress.isSolved()) {
            hideSolvedView();
        }
    }

    private void onHexPiecePositionsChanged(ImmutableList<Pos> piecePositions) {
        Solution.Progress oldHexPuzzleProgress = hexPuzzleProgress;
        hexPuzzleProgress = Solution.calculateProgress(piecePositions, HexDirection.VALUES);
        if (oldHexPuzzleProgress == null) {
            // This is the first time progress is calculated, probably because we first loaded
            // this view. Don't update the solved view in this case.
            return;
        }
        if (hexPuzzleProgress.isSolved() && !oldHexPuzzleProgress.isSolved()) {
            LogUtil.i("Hex puzzle is solved!");
            HexPuzzleFragment hexPuzzleFragment =
                    (HexPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.HEX_PUZZLE.name());
            if (hexPuzzleFragment != null) {
                HexGridView hexGridView = hexPuzzleFragment.getHexGridView();
                if (hexGridView != null) {
                    hexGridView.startVictoryAnimation();
                }
                showSolvedView();
            }
        }
        if (!hexPuzzleProgress.isSolved() && oldHexPuzzleProgress.isSolved()) {
            hideSolvedView();
        }
    }

    private void showSolvedView() {
        solvedView.setVisibility(View.VISIBLE);
        if (!solvedViewShown) {
            TranslateAnimation animation = new TranslateAnimation(0, 0, solvedView.getHeight(), 0);
            animation.setStartOffset(12000);  // 12 seconds
            animation.setDuration(1000);  // 1 seconds
            solvedView.startAnimation(animation);
            solvedView.findViewById(R.id.solved_continue_button).setOnClickListener(this);
            solvedView.findViewById(R.id.solved_reset_pieces_button).setOnClickListener(this);
        }
        solvedViewShown = true;
    }

    private void hideSolvedView() {
        if (solvedViewShown) {
            TranslateAnimation animation = new TranslateAnimation(0, 0, 0, solvedView.getHeight());
            animation.setDuration(500);  // 0.5 seconds
            solvedView.startAnimation(animation);
            solvedView.setOnClickListener(null);
            solvedView.findViewById(R.id.solved_continue_button).setOnClickListener(null);
            solvedView.findViewById(R.id.solved_reset_pieces_button).setOnClickListener(null);
        }
        solvedView.setVisibility(View.INVISIBLE);
        solvedViewShown = false;
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
