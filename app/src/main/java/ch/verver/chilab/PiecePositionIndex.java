package ch.verver.chilab;

import android.graphics.Rect;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Stores an ordered list of piece positions, and a reverse index to find a piece by position.
 *
 * <p>Note that this class could potentially implement the List interface, but that interface has
 * many more methods than the app would actually use, so it's not really worth the trouble.
 */
class PiecePositionIndex implements Iterable<Pos> {

    private ArrayList<Pos> positions;
    private HashMap<Pos, Integer> index;

    /**
     * Creates an index with but an empty list of pieces.
     * The piece list should be set afterwards by calling {@link #assign}.
     */
    public PiecePositionIndex() {
        this.positions = new ArrayList<>();
        this.index = new HashMap<>();
    }

    /** Creates a deep copy of on existing instance. */
    public PiecePositionIndex(PiecePositionIndex oldIndex) {
        assign(oldIndex.positions);
    }

    /** Assigns a new list of positions. All elements must be distinct! */
    public void assign(List<Pos> positions) {
        ArrayList<Pos> newPositions = new ArrayList<>(positions);
        HashMap<Pos, Integer> newIndex = new HashMap<>(positions.size());
        for (int i = 0; i < newPositions.size(); ++i) {
            Pos pos = newPositions.get(i);
            if (newIndex.put(pos, i) != null) {
                throw new IllegalArgumentException("duplicate piece positions");
            }
        }
        this.positions = newPositions;
        this.index = newIndex;
    }

    public ArrayList<Pos> toList() {
        return new ArrayList<>(positions);
    }

    /**
     * Returns a minimum bounding rectangle (left, top, right, bottom) such that for all positions,
     * {@code left <= pos.x < right && top <= pos.y < bottom}.
     */
    public Rect getBoundingRect() {
        if (positions.isEmpty()) {
            return new Rect(0, 0, 0, 0);
        }
        Pos firstPos = positions.get(0);
        Rect result = new Rect(firstPos.x, firstPos.y, firstPos.x, firstPos.y);
        for (int i = 1; i < positions.size(); ++i) {
            Pos pos = positions.get(i);
            result.left = Math.min(result.left, pos.x);
            result.top = Math.min(result.top, pos.y);
            result.right = Math.max(result.right, pos.x + 1);
            result.bottom = Math.max(result.bottom, pos.y + 1);
        }
        return result;
    }

    public int size() {
        return positions.size();
    }

    public Pos get(int i) {
        return positions.get(i);
    }

    /**
     * Moves the piece at index {@code i} to position {@code dst}. If that position is already
     * occupied by a different piece, the pieces positions are swapped (i.e., the other piece is
     * moved to the i-th piece's old position). Either way, the i-th piece ends up at {@code dst}.
     */
    public void moveOrSwap(int i, Pos dst) {
        Pos src = positions.get(i);
        if (src.equals(dst)) {
            return;
        }
        int j = indexOf(dst);
        if (j < 0) {
            index.remove(src);
        } else {
            positions.set(j, src);
            index.put(src, j);
        }
        positions.set(i, dst);
        index.put(dst, i);
    }

    /**
     * Returns whether positions {@code pos} contains a piece.
     * If the requested position is out of range, no exception is thrown, and false is returned.
     */
    public boolean contains(Pos pos) {
        return contains(pos.x, pos.y);
    }

    /**
     * Returns whether position (x, y) contains a piece.
     * If the requested position is out of range, no exception is thrown, and false is returned.
     */
    public boolean contains(int x, int y) {
        return indexOf(x, y) >= 0;
    }

    /**
     * Returns the index of the piece at position {@code pos}, or -1 if there is no such piece.
     * If the requested position is out of range, no exception is thrown, and false is returned.
     */
    public int indexOf(Pos pos) {
        Integer value = index.get(pos);
        return value != null ? value : -1;
    }

    /**
     * Returns the index of the piece at position (x, y), or -1 if there is no such piece.
     * If the requested position is out of range, no exception is thrown, and false is returned.
     */
    public int indexOf(int x, int y) {
        return indexOf(new Pos(x, y));
    }

    @Override
    @NonNull
    public Iterator<Pos> iterator() {
        // Note that we don't just return positions.iterator(), because that returns a mutable
        // iterator that supports the remove() method, which we don't want.
        return new PositionIterator();
    }

    private class PositionIterator implements Iterator<Pos> {
        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < positions.size();
        }

        @Override
        public Pos next() {
            if (index < positions.size()) {
                return positions.get(index);
            }
            throw new NoSuchElementException();
        }
    }
}
