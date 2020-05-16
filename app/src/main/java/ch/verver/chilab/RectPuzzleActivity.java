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

public class RectPuzzleActivity extends AppCompatActivity {

    public static final String INTENT_EXTRA_RECT_PIECES = "rect-pieces";

    private RectGridView rectGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rect_puzzle);
        rectGridView = findViewById(R.id.rect_grid_view);

        if (savedInstanceState == null) {
            rectGridView.setPiecePositions(restorePiecePositions());
        }

        rectGridView.setPiecePositionsChangedListener(
                new RectGridView.PiecePositionsChangedListener() {
                    @Override
                    public void piecePositionsChanged(RectGridView view) {
                        // Debug-print base-64 encoded piece positions. Can be restored with e.g.:
                        // adb shell am start-activity --es rect-pieces XXX ch.verver.chilab/.RectPuzzleActivity
                        PiecePositionIndex piecePositionIndex = view.getPiecePositionIndex();
                        LogUtil.i("rect-pieces %s", RectPuzzle.encode(view.getPiecePositions()));
                        recalculateVictoryConditions(piecePositionIndex);
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.rect_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_to_hex_puzzle:
                startActivity(new Intent(this, HexPuzzleActivity.class));
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
            String encoded = sharedPreferences.getString(Preferences.RECT_PIECES_KEY, null);
            if (encoded != null) {
                ArrayList<Pos> result = RectPuzzle.decode(encoded);
                if (result != null) {
                    LogUtil.i("Restored piece positions from shared preferences");
                    return result;
                }
                LogUtil.e("Could not restore piece positions from shared preferences!");
            }
        }

        // 3. get random initial position (when app is started for the first time)
        LogUtil.i("Generating random piece positions");
        return RectPuzzle.getRandomPiecePositions();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Preferences.getSharedPreferences(this).edit()
                .putString(
                        Preferences.RECT_PIECES_KEY,
                        RectPuzzle.encode(rectGridView.getPiecePositions()))
                .commit();
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
        String encoded = extras.getString(INTENT_EXTRA_RECT_PIECES);
        if (encoded == null) {
            return null;
        }
        ArrayList<Pos> positions = RectPuzzle.decode(encoded);
        if (positions == null) {
            LogUtil.e("Failed to decode positions from intent \"%s\"", encoded);
            return null;
        }
        return positions;
    }

    private void recalculateVictoryConditions(PiecePositionIndex piecePositionIndex) {
        Solution.Progress progress =
                Solution.calculateProgress(piecePositionIndex, RectDirection.values());
        LogUtil.i("groupCount=%d  unconnectedPathCount=%d  connectedBackCount=%d  solved=%s",
                progress.getGroupCount(), progress.getDisconnectionCount(),
                progress.getOverlapCount(), progress.isSolved());
    }
}
