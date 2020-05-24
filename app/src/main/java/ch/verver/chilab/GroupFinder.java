package ch.verver.chilab;

import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Utility class that finds connected groups using breadth-first search. Groups are maximal sets of
 * pieces that are connected by bidirectional paths.
 */
class GroupFinder {

    /**
     * One step in the breadth-first search. Each piece in a group, except for the first, is
     * reached from a previous piece (indicated by {@code previousStepIndex}) by moving into a
     * specific direction ({@code previousDirection}). The sequence of steps encodes the shape of
     * the group and can be used to reconstruct the other piece positions given the position of
     * the first.
     */
    static class Step {
        /** Index of the piece reached this step */
        final int pieceIndex;

        /** Index of the step this piece was reached from (note: NOT the piece index!), or -1. */
        final int previousStepIndex;

        /** Direction taken from the previous step, or null if previousStepIndex == -1. */
        final @Nullable Direction lastStepDirection;

        private Step(int pieceIndex, int previousStepIndex, Direction lastStepDirection) {
            this.pieceIndex = pieceIndex;
            this.previousStepIndex = previousStepIndex;
            this.lastStepDirection = lastStepDirection;
        }
    }

    /** Returns a count of the number of connected groups. */
    public static int countGroups(Direction[] directions, ReadonlyPiecePositionIndex piecePositions) {
        return new GroupFinder(directions, piecePositions, false).countGroups();
    }

    /** Finds the group that the piece with the given index belongs to. */
    public static Step[] calculateSteps(Direction[] directions, ReadonlyPiecePositionIndex piecePositions, int firstPieceIndex) {
        return new GroupFinder(directions, piecePositions, true).calculateSteps(firstPieceIndex);
    }

    /** Returns an array of piece indices for the given list of steps. */
    public static int[] getPieces(Step[] steps) {
        int[] pieces = new int[steps.length];
        for (int i = 0; i < steps.length; ++i) {
            pieces[i] = steps[i].pieceIndex;
        }
        return pieces;
    }

    /** Returns a bitmask of piece indices for the given list of steps. */
    public static long getPieceMask(Step[] steps) {
        long pieces = 0;
        for (Step step : steps) {
            if (step.pieceIndex < 0 || step.pieceIndex >= Long.SIZE) {
                throw new IllegalArgumentException();
            }
            pieces |= (long) 1 << step.pieceIndex;
        }
        return pieces;
    }

    /**
     * Returns the positions of the pieces in the group identified by {@code steps}, given that the
     * first piece is placed at {@code firstPiecePos}.
     */
    public static Pos[] reconstructPositions(Step[] steps, Pos firstPiecePos) {
        Pos[] positions = new Pos[steps.length];
        positions[0] = firstPiecePos;
        for (int i = 1; i < steps.length; ++i) {
            positions[i] = steps[i].lastStepDirection.step(positions[steps[i].previousStepIndex]);
        }
        return positions;
    }

    private GroupFinder(Direction[] directions, ReadonlyPiecePositionIndex piecePositionIndex, boolean calculateSteps) {
        this.directions = directions;
        this.piecePositionIndex = piecePositionIndex;
        int n = piecePositionIndex.size();
        this.seen = new boolean[n];
        this.queue = new int[n];
        this.steps = calculateSteps ? new Step[n] : null;
    }

    private int countGroups() {
        int groupCount = 0;
        for (int i = 0, n = piecePositionIndex.size(); i < n; ++i) {
            if (addToQueue(i, null)) {
                ++groupCount;
                processQueue();
            }
        }
        return groupCount;
    }

    private Step[] calculateSteps(int firstPieceIndex) {
        addToQueue(firstPieceIndex, null);
        processQueue();
        return Arrays.copyOf(steps, queueSize);
    }

    private boolean addToQueue(int i, @Nullable Direction previousDirection) {
        if (seen[i]) {
            return false;
        }
        seen[i] = true;
        queue[queueSize++] = i;
        if (steps != null) {
            steps[queueSize - 1] = new Step(i, queuePos - 1, previousDirection);
        }
        return true;
    }

    private void processQueue() {
        while (queuePos < queueSize) {
            int i = queue[queuePos++];
            Pos pos = piecePositionIndex.get(i);
            for (Direction dir : directions) {
                if (dir.hasPath(i)) {
                    Pos newPos = dir.step(pos);
                    int j = piecePositionIndex.indexOf(newPos);
                    if (j >= 0 && dir.opposite().hasPath(j)) {
                        addToQueue(j, dir);
                    }
                }
            }
        }
    }

    private final Direction[] directions;
    private final ReadonlyPiecePositionIndex piecePositionIndex;
    private final boolean[] seen;
    private final int[] queue;
    private final @Nullable Step[] steps;
    private int queuePos = 0;
    private int queueSize = 0;
}
