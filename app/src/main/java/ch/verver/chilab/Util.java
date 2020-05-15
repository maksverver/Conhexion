package ch.verver.chilab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

abstract class Util {

    /**
     * Checks that a list of positions is a valid puzzle piece configuration.
     *
     * <p>Specifically, this verifies that:</p>
     * <ol>
     *     <li>{@code positions} contains exactly {@code count} elements.</li>
     *     <li>Each position x-coordinate is between 0 and {@code width} (exclusive).</li>
     *     <li>Each position y-coordinate is between 0 and {@code height} (exclusive).</li>
     *     <li>All positions are distinct.</li>
     * </ol>
     *
     * @return true if all of the above properties hold, false otherwise
     */
    static boolean validatePositions(List<Pos> positions, int count) {
        if (positions.size() != count) {
            return false;
        }
        Set<Pos> seen = new HashSet<>();
        for (Pos p : positions) {
            if (!seen.add(p)) {
                return false;
            }
        }
        return true;
    }

    private Util() {}
}
