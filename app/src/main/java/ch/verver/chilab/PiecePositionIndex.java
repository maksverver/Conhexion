package ch.verver.chilab;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

// TODO: use this class in RectGridView and RectPuzzle, too.

/**
 * Stores an ordered list of piece positions, and a reverse index to find a piece by position.
 *
 * <p>Note that this class could potentially implement the List interface, but it has many more
 * methods than we actually need, so it's not really worth the trouble.
 */
class PiecePositionIndex implements Iterable<Pos> {

    private final int gridWidth;
    private final int gridHeight;
    private ArrayList<Pos> positions;
    private int[][] index;

    /**
     * Creates an index with the given grid dimensions, but an empty list of pieces.
     * The piece list should be set afterwards by calling {@link #assign}.
     */
    public PiecePositionIndex(int gridWidth, int gridHeight) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.positions = new ArrayList<>();
        this.index = createIndex(gridWidth, gridHeight);
    }

    /** Creates a deep copy of on existing instance. */
    public PiecePositionIndex(PiecePositionIndex oldIndex) {
        this(oldIndex.gridWidth, oldIndex.gridHeight, oldIndex);
    }

    /**
     * Creates an index with the given grid dimensions, but with the list of piece positions copied
     * from {@code oldIndex}.
     *
     * <p>Important: all piece positions in {@code oldIndex} must fit in the new grid dimensions!
     *
     * @throws IllegalArgumentException if any piece position in {@code oldIndex} is out of range
     */
    public PiecePositionIndex(int gridWidth, int gridHeight, PiecePositionIndex oldIndex) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        assign(oldIndex.positions);
    }

    /** Assigns a new list of positions. All elements must be distinct! */
    public void assign(List<Pos> positions) {
        ArrayList<Pos> newPositions = new ArrayList<>(positions);
        int[][] newIndex = createIndex(gridWidth, gridHeight);
        for (int i = 0; i < newPositions.size(); ++i) {
            Pos pos = newPositions.get(i);
            if (!inRange(pos)) {
                throw new IllegalArgumentException("position out of grid range");
            }
            if (newIndex[pos.x][pos.y] != -1) {
                throw new IllegalArgumentException("duplicate piece positions");
            }
            newIndex[pos.x][pos.y] = i;
        }
        this.positions = newPositions;
        this.index = newIndex;
    }

    public ArrayList<Pos> toList() {
        return new ArrayList<>(positions);
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
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
        if (!inRange(dst)) {
            throw new IllegalArgumentException("position out of grid range");
        }
        Pos src = positions.get(i);
        if (src.equals(dst)) {
            return;
        }
        int j = indexOf(dst);
        positions.set(i, dst);
        if (j >= 0) {
            positions.set(j, src);
        }
        index[src.x][src.y] = j;
        index[dst.x][dst.y] = i;
    }

    /** Returns whether position {@code pos} is in range of the grid. */
    public boolean inRange(Pos pos) {
        return inRange(pos.x, pos.y);
    }

    /** Returns whether position (x, y) is in range of the grid. */
    public boolean inRange(int x, int y) {
        return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
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
        return indexOf(pos.x, pos.y);
    }

    /**
     * Returns the index of the piece at position (x, y), or -1 if there is no such piece.
     * If the requested position is out of range, no exception is thrown, and false is returned.
     */
    public int indexOf(int x, int y) {
        return inRange(x, y) ? index[x][y] : -1;
    }

    @Override
    @NonNull
    public Iterator<Pos> iterator() {
        // Note that we don't just return positions.iterator(), because that returns a mutable
        // iterator that support the remove() method, which we don't want.
        return new PositionIterator();
    }

    private static int[][] createIndex(int width, int height) {
        int[][] index = new int[width][height];
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                index[x][y] = -1;
            }
        }
        return index;
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
