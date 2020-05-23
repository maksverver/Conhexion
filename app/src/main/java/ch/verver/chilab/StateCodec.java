package ch.verver.chilab;

import java.util.ArrayList;
import java.util.List;

/** Utility class to encode/decode puzzle state to/from String. */
abstract class StateCodec {

    static String encodePositions(List<Pos> positions) {
        int[] ints = new int[positions.size() * 2];
        for (int i = 0; i < positions.size(); ++i) {
            Pos pos = positions.get(i);
            ints[2*i + 0] = pos.x;
            ints[2*i + 1] = pos.y;
        }
        return encodeInts(ints);
    }

    /**
     * Decodes a list of positions from as a string in the format returned by {@link
     * #encodePositions}.
     *
     * @throws IllegalArgumentException if the string is formatted incorrectly
     */
    static ArrayList<Pos> decodePositions(String string) {
        int[] ints = decodeInts(string);
        if (ints.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of coordinates");
        }
        int n = ints.length / 2;
        ArrayList<Pos> result = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            int x = ints[2 * i + 0];
            int y = ints[2 * i + 1];
            result.add(new Pos(x, y));
        }
        return result;
    }

    /**
     * Encodes an array of integers as a comma-seperated list of decimal numbers.
     *
     * <p>For example, encodeInts({0, -1, 3}) == "0,-1,3";
     */
    private static String encodeInts(int[] ints) {
        StringBuilder sb = new StringBuilder(ints.length * 3);
        boolean first = true;
        for (int i : ints) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(i);
        }
        return sb.toString();
    }

    /**
     * Decodes an array of integers from a string in the format returned by {@link #encodeInts}.
     *
     * @throws NumberFormatException (which is a subclass of {@link IllegalArgumentException})
     *      if the string is formatted incorrectly
     */
    private static int[] decodeInts(String string) {
        if (string.isEmpty()) {
            return new int[0];
        }
        String[] parts = string.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            result[i] = Integer.parseInt(parts[i]);  // may throw NumberFormatException
        }
        return result;
    }

    private StateCodec() {}
}
