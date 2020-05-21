package ch.verver.chilab;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity implements HexPiecePositionsChangedListener, RectPiecePositionsChangedListener {

    private AppState appState;
    private RectPuzzleFragment rectPuzzleFragment;
    private HexPuzzleFragment hexPuzzleFragment;

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

        rectPuzzleFragment = (RectPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.RECT_PUZZLE.name());
        if (rectPuzzleFragment == null) {
            rectPuzzleFragment = new RectPuzzleFragment(appState.getRectPuzzlePiecePositions());
        }

        hexPuzzleFragment = (HexPuzzleFragment) getSupportFragmentManager().findFragmentByTag(FragmentId.HEX_PUZZLE.name());
        if (hexPuzzleFragment == null) {
            hexPuzzleFragment = new HexPuzzleFragment(appState.getHexPuzzlePiecePositions());
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

    private void setActiveFragment(FragmentId newFragmentId) {
        Fragment newFragment = getFragment(newFragmentId);
        if (newFragment == null) {
            LogUtil.e("Cannot switch to nonexistent fragment %s!", newFragmentId);
            return;
        }
        LogUtil.i("Switching main fragment to %s", newFragmentId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment, newFragmentId.name());
        // TODO: use setCustomAnimations() to slide in/out instead?
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.commit();
        appState.setActiveFragmentId(newFragmentId);
    }

    @Override
    public void rectPiecePositionsChanged(RectGridView view) {
        appState.setRectPuzzlePiecePositions(view.getPiecePositions());
        // TODO: recalculate victory conditions
    }

    @Override
    public void hexPiecePositionsChanged(HexGridView view) {
        appState.setHexPuzzlePiecePositions(view.getPiecePositions());
        // TODO: recalculate victory conditions
    }

    private Fragment getFragment(FragmentId id) {
        switch (id) {
            case RECT_PUZZLE:
                return rectPuzzleFragment;
            case HEX_PUZZLE:
                return hexPuzzleFragment;
        }
        return null;
    }
}
