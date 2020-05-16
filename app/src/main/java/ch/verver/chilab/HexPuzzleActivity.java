package ch.verver.chilab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

public class HexPuzzleActivity extends AppCompatActivity {

    public static final String INTENT_EXTRA_HEX_PIECES = "hex-pieces";

    private HexGridView hexGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hex_puzzle);
        hexGridView = findViewById(R.id.hex_grid_view);

        if (savedInstanceState == null) {
            hexGridView.setPiecePositions(restorePiecePositions());
        }

        hexGridView.setPiecePositionsChangedListener(
                new HexGridView.PiecePositionsChangedListener() {
                    @Override
                    public void piecePositionsChanged(HexGridView view) {
                        PiecePositionIndex piecePositions = view.getPiecePositionIndex();
                        // Debug-print base-64 encoded piece positions. Can be restored with e.g.:
                        // adb shell am start-activity --es hex-pieces XXX ch.verver.chilab/.HexPuzzleActivity
                        LogUtil.i("hex-pieces %s", HexPuzzle.encode(piecePositions.toList()));
                        recalculateVictoryConditions(piecePositions);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hex_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_to_rect_puzzle:
                startActivity(new Intent(this, RectPuzzleActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private ArrayList<Pos> restorePiecePositions() {
        // 1. from intent (used for debugging)
        {
            ArrayList<Pos> result = getPiecePositionsFromIntent(getIntent());
            if (result != null) {
                LogUtil.i("Restored piece positions from intent extra");
                return result;
            }
        }

        // 2. from shared preferences (when app is restarted)
        {
            SharedPreferences sharedPreferences = Preferences.getSharedPreferences(this);
            String encoded = sharedPreferences.getString(Preferences.HEX_PIECES_KEY, null);
            if (encoded != null) {
                ArrayList<Pos> result = HexPuzzle.decode(encoded);
                if (result != null) {
                    LogUtil.i("Restored piece positions from shared preferences");
                    return result;
                }
                LogUtil.e("Could not restore piece positions from shared preferences!");
            }
        }

        // 3. get random initial position (when app is started for the first time)
        LogUtil.i("Generating random piece positions");
        return HexPuzzle.getRandomPiecePositions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        LogUtil.i("Saving hex pieces to shared preferences");
        Preferences.getSharedPreferences(this).edit()
                .putString(
                        Preferences.HEX_PIECES_KEY,
                        HexPuzzle.encode(hexGridView.getPiecePositions()))
                .commit();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtil.i("onStop called");
    }

    @Nullable
    private static ArrayList<Pos> getPiecePositionsFromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        String encoded = extras.getString(INTENT_EXTRA_HEX_PIECES);
        if (encoded == null) {
            return null;
        }
        ArrayList<Pos> positions = HexPuzzle.decode(encoded);
        if (positions == null) {
            LogUtil.e("Failed to decode positions from intent \"%s\"", encoded);
            return null;
        }
        return positions;
    }

    private void recalculateVictoryConditions(PiecePositionIndex piecePositionIndex) {
        Solution.Progress progress =
                Solution.calculateProgress(piecePositionIndex, HexDirection.values());
        LogUtil.i("groupCount=%d  unconnectedPathCount=%d  connectedBackCount=%d  solved=%s",
                progress.getGroupCount(), progress.getDisconnectionCount(),
                progress.getOverlapCount(), progress.isSolved());
    }
}
