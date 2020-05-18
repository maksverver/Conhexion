package ch.verver.chilab;

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

    private Util() {}
}
