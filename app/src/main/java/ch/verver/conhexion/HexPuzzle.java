package ch.verver.conhexion;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Definitions for the real China Labyrinth puzzle played on a hexagonal grid. */
abstract class HexPuzzle {

    public static final int PIECE_COUNT = 63;

    public static ArrayList<Pos> getRandomPiecePositions() {
        ArrayList<Pos> points = new ArrayList<>();
        for (int y = 0; y < 9; ++y) {
            for (int x = 0; x < 7; ++x) {
                points.add(new Pos(2*x, 2*y));
            }
        }
        if (points.size() < PIECE_COUNT) {
            throw new AssertionError();
        }
        Collections.shuffle(points);
        while (points.size() > PIECE_COUNT) {
            points.remove(points.size() - 1);
        }
        return points;
    }

    public static boolean validate(@Nullable List<Pos> positions) {
        return positions != null && Util.validatePositions(positions, PIECE_COUNT);
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
        if (!validate(positions)) {
            return null;
        }
        return positions;
    }
}
