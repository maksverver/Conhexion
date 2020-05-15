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

    static Progress calculateProgress(PiecePositionIndex piecePositionIndex, Direction[] directions) {
        return new Solution.Progress(
                countGroups(piecePositionIndex, directions),
                countDisconnections(piecePositionIndex, directions),
                countOverlaps(piecePositionIndex, directions));
    }

    // Simple breadth-first search algorithm to find the number of connected groups.
    private static int countGroups(final PiecePositionIndex piecePositionIndex, final Direction[] directions) {
        class GroupCounter {
            GroupCounter() {
                for (int startIndex = 0; startIndex < piecePositionIndex.size(); ++startIndex) {
                    if (addToQueue(startIndex)) {
                        ++groupCount;
                        processQueue();
                    }
                }
            }

            boolean addToQueue(int i) {
                if (seen[i]) {
                    return false;
                }
                seen[i] = true;
                queue[queueSize++] = i;
                return true;
            }

            void processQueue() {
                while (queuePos < queueSize) {
                    int i = queue[queuePos++];
                    Pos pos = piecePositionIndex.get(i);
                    for (Direction dir : directions) {
                        if (dir.hasPath(i)) {
                            Pos newPos = dir.step(pos);
                            int j = piecePositionIndex.indexOf(newPos);
                            if (j >= 0 && dir.opposite().hasPath(j)) {
                                addToQueue(j);
                            }
                        }
                    }
                }
            }

            boolean[] seen = new boolean[piecePositionIndex.size()];
            int[] queue = new int[piecePositionIndex.size()];
            int queuePos = 0;
            int queueSize = 0;
            int groupCount = 0;
        }
        return new GroupCounter().groupCount;
    }

    private static int countDisconnections(PiecePositionIndex piecePositionIndex, Direction[] directions) {
        int result = 0;
        for (int i = 0; i < piecePositionIndex.size(); ++i) {
            Pos pos = piecePositionIndex.get(i);
            for (Direction dir : directions) {
                if (dir.hasPath(i)) {
                    int j = piecePositionIndex.indexOf(dir.step(pos));
                    if (j < 0 || !dir.opposite().hasPath(j)) {
LogUtil.i("Missing connection %d %s %s %d", i, pos, dir, j);
                        ++result;
                    }
                }
            }
        }
        return result;
    }

    private static int countOverlaps(PiecePositionIndex piecePositionIndex, Direction[] directions) {
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
