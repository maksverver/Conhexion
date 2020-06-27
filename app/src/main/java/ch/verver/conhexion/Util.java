package ch.verver.conhexion;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class Util {

    /**
     * Checks that a list of positions is a valid puzzle piece configuration.
     *
     * <p>Specifically, verifies that {@code positions} contains exactly {@code count} elements, and
     * that all positions are distinct.
     */
    static boolean validatePositions(List<Pos> positions, int count) {
        return positions.size() == count && allDistinct(positions);
    }

    /**
     * Returns whether all elements in the collection are distinct. Requires that values implement
     * {@link Object#equals(Object)} and {@link Object#hashCode()} correctly.
     */
    static boolean allDistinct(Collection<?> elements) {
        Set<Object> seen = new HashSet<>();
        for (Object o : elements) {
            if (!seen.add(o)) {
                return false;
            }
        }
        return true;
    }

    /** Utility method to test if {@code pieceIndex} is set in the {@code draggedPieces} bitmask. */
    static boolean isDragged(long draggedPieces, int pieceIndex) {
        return (draggedPieces & ((long) 1 << pieceIndex)) != 0;
    }

    /** Returns whether the number of piece indices in {@code draggedPieces} is greater than 1. */
    static boolean isMultiDrag(long draggedPieces) {
        return (draggedPieces & (draggedPieces - 1)) != 0;
    }

    /**
     * Returns the lowest piece index in {@code draggedPieces}, or -1 if {@code draggedPieces == 0}.
     */
    static int getDraggedIndex(long draggedPieces) {
        return draggedPieces == 0 ? -1 : Long.numberOfTrailingZeros(draggedPieces);
    }

    private Util() {}
}
