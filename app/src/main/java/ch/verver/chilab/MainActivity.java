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
    private FragmentId activeFragmentId = FragmentId.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appState = App.getAppState();

        rectPuzzleFragment = new RectPuzzleFragment();
        hexPuzzleFragment = new HexPuzzleFragment();

        switchTo(appState.getActiveFragmentId());
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
                switchTo(FragmentId.RECT_PUZZLE);
                return true;

            case R.id.switch_to_hex_puzzle:
                switchTo(FragmentId.HEX_PUZZLE);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void switchTo(FragmentId newFragmentId) {
        LogUtil.i("Switching main fragment from %s to %s", activeFragmentId, newFragmentId);
        if (activeFragmentId == newFragmentId) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment oldFragment = getFragment(activeFragmentId);
        Fragment newFragment = getFragment(newFragmentId);
        if (newFragment == null) {
            if (oldFragment != null) {
                transaction.remove(oldFragment);
            }
        } else {  // newFragment != null
            transaction.replace(R.id.fragment_container, newFragment, null);
            // TODO: use setCustomAnimations() to slide in/out instead?
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        if (oldFragment != null) {
            /* Could add the old state to the back stack like this. But: how would the MainActivity
                know that the active fragment has changed? */
            // transaction.setReorderingAllowed(true);
            // transaction.addToBackStack(activeFragmentId.name());
        }
        transaction.commit();
        activeFragmentId = newFragmentId;
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
