package ch.verver.chilab;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HexPuzzle {

    public static final int GRID_WIDTH = 17;
    public static final int GRID_HEIGHT = 16;
    public static final int PIECE_COUNT = 63;

    public static ArrayList<Pos> getRandomPiecePositions() {
        ArrayList<Pos> points = new ArrayList<>();
        for (int y = 0; y < GRID_HEIGHT; y += 2) {
            for (int x = 0; x < GRID_WIDTH; x += 2) {
                points.add(new Pos(x, y));
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

    public static String encode(List<Pos> positions) {
        int n = positions.size();
        byte[] bytes = new byte[2*n];
        for (int i = 0; i < n; ++i) {
            Pos p = positions.get(i);
            if (p.x < 0 || p.x > 255 || p.y < 0 || p.y > 255) {
                throw new IllegalArgumentException("Position out of range");
            }
            bytes[2*i + 0] = (byte) p.x;
            bytes[2*i + 1] = (byte) p.y;
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
        int n = bytes.length / 2;
        positions.ensureCapacity(n);
        for (int i = 0; i < n; ++i) {
            positions.add(new Pos(bytes[2*i], bytes[2*i + 1]));
        }
        if (!Util.validatePositions(positions, PIECE_COUNT, GRID_WIDTH, GRID_HEIGHT)) {
            return null;
        }
        return positions;
    }
}
