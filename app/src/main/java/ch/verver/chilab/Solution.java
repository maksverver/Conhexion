package ch.verver.chilab;

/** Utility class to determine whether a puzzle is solved. */
class Solution {

    /** Describes the progress of a puzzle solution. */
    public static class Progress {
        private final int groupCount;
        private final int disconnectionCount;
        private final int overlapCount;

        public Progress(int groupCount, int disconnectionCount, int overlapCount) {
            this.groupCount = groupCount;
            this.disconnectionCount = disconnectionCount;
            this.overlapCount = overlapCount;
        }

        /** Returns the number of disjoint groups. In a solved puzzle, this number should be 1. */
        public int getGroupCount() {
            return groupCount;
        }

        /**
         * Returns the number of disconnections, i.e. the number of sides of tiles that contain
         * a path, but are not connected to a matching tile. In a solved puzzle, this number should
         * be 0.
         */
        public int getDisconnectionCount() {
            return disconnectionCount;
        }

        /**
         * Returns the number tile sides that touch/overlap other tiles. In a solved puzzle, this
         * number should be zero.
         */
        public int getOverlapCount() {
            return overlapCount;
        }

        /**
         * Returns whether the puzzle is completely solved, which is the case when all tiles are
         * connected into a single group, with no unconnected paths, and no overlapping tiles.
         */
        public boolean isSolved() {
            return groupCount == 1 && disconnectionCount == 0 && overlapCount == 0;
        }
    }

    static Progress calculateProgress(ReadonlyPiecePositionIndex piecePositionIndex, Direction[] directions) {
        return new Solution.Progress(
                GroupFinder.countGroups(directions, piecePositionIndex),
                countDisconnections(piecePositionIndex, directions),
                countOverlaps(piecePositionIndex, directions));
    }

    private static int countDisconnections(ReadonlyPiecePositionIndex piecePositionIndex, Direction[] directions) {
        int result = 0;
        for (int i = 0; i < piecePositionIndex.size(); ++i) {
            Pos pos = piecePositionIndex.get(i);
            for (Direction dir : directions) {
                if (dir.hasPath(i)) {
                    int j = piecePositionIndex.indexOf(dir.step(pos));
                    if (j < 0 || !dir.opposite().hasPath(j)) {
                        ++result;
                    }
                }
            }
        }
        return result;
    }

    private static int countOverlaps(ReadonlyPiecePositionIndex piecePositionIndex, Direction[] directions) {
        int result = 0;
        for (int i = 0; i < piecePositionIndex.size(); ++i) {
            Pos pos = piecePositionIndex.get(i);
            for (Direction dir : directions) {
                if (!dir.hasPath(i) && piecePositionIndex.contains(dir.step(pos))) {
                    ++result;
                }
            }
        }
        return result;
    }

    private Solution() {}
}
