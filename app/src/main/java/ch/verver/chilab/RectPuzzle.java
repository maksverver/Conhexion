package ch.verver.chilab;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rectangular version of the (usually hexagonal) China Labyrinth puzzle.
 */
public class RectPuzzle {
    public static final int GRID_WIDTH = 9;
    public static final int GRID_HEIGHT = 9;
    public static final int PIECE_COUNT = 15;

    public static ArrayList<Pos> getRandomPiecePositions() {
        ArrayList<Pos> positions = new ArrayList<>();
        for (int y = 0; y < GRID_HEIGHT; y += 2) {
            for (int x = 0; x < GRID_WIDTH; x += 2) {
                positions.add(new Pos(x, y));
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
        byte[] bytes = new byte[positions.size()];
        for (int i = 0; i < bytes.length; ++i) {
            Pos p = positions.get(i);
            if (p.x < 0 || p.x > 15 || p.y < 0 || p.y > 15) {
                throw new IllegalArgumentException("Position out of range");
            }
            bytes[i] = (byte) (p.x | (p.y << 4));
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    @Nullable
    public static ArrayList<Pos> decode(String s) {
        byte[] bytes;
        try {
            bytes = Base64.decode(s, Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
            return null;
        }
        ArrayList<Pos> positions = new ArrayList<>();
        positions.ensureCapacity(bytes.length);
        for (byte b : bytes) {
            int x = b & 0xf;
            int y = (b >> 4) & 0xf;
            positions.add(new Pos(x, y));
        }
        if (!Util.validatePositions(positions, PIECE_COUNT, GRID_WIDTH, GRID_HEIGHT)) {
            return null;
        }
        return positions;
    }

    private RectPuzzle() {}
}
