package ch.verver.chilab;

import android.graphics.Rect;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

interface ReadonlyPiecePositionIndex extends Iterable<Pos> {

    /** Returns a copy of the current piece positions. */
    ArrayList<Pos> toList();

    /**
     * Returns a minimum bounding rectangle (left, top, right, bottom) such that for all positions,
     * {@code left <= pos.x < right && top <= pos.y < bottom}.
     */
    Rect getBoundingRect();

    /** Returns the number of pieces. */
    int size();

    /**
     * Returns the position of the i-th piece.
     * @throws IndexOutOfBoundsException if i < 0 or i >= size()
     */
    Pos get(int i);

    /** Returns whether there is a piece at position {@code pos}. */
    boolean contains(Pos pos);

    /** Returns whether there is a piece at position (x, y). */
    boolean contains(int x, int y);

    /** Returns the index of the piece at position {@code pos}, or -1 if there is no such piece. */
    int indexOf(Pos pos);

    /** Returns the index of the piece at position (x, y), or -1 if there is no such piece. */
    int indexOf(int x, int y);
}
