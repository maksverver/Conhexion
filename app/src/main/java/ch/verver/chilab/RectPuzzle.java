package ch.verver.chilab;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Definitions for a variant of the China Labyrinth puzzle played on a square grid. */
abstract class RectPuzzle {
    public static final int PIECE_COUNT = 15;

    public static ArrayList<Pos> getRandomPiecePositions() {
        ArrayList<Pos> positions = new ArrayList<>();
        for (int y = 0; y < 6; ++y) {
            for (int x = 0; x < 5; ++x) {
                if ((x + y) % 2 == 1) {
                    positions.add(new Pos(x, y));
                }
            }
        }
        if (positions.size() < PIECE_COUNT) {
            throw new AssertionError();
        }
        Collections.shuffle(positions);
        while (positions.size() > PIECE_COUNT) {
            positions.remove(positions.size() - 1);
        }
        return positions;
    }

    public static String encode(List<Pos> positions) {
        return StateCodec.encodePositions(positions);
    }

    @Nullable
    public static ArrayList<Pos> decode(String s) {
        ArrayList<Pos> positions;
        try {
            positions = StateCodec.decodePositions(s);
        } catch (IllegalArgumentException e) {
            LogUtil.w(e, "Failed to decode state");
            return null;
        }
        if (!Util.validatePositions(positions, PIECE_COUNT)) {
            return null;
        }
        return positions;
    }

    private RectPuzzle() {}
}
