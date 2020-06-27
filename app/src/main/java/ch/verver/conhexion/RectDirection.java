package ch.verver.conhexion;

// Coordinate system
//
//     x------------------>
//
//   y  (0,0)  (1,0)  (2,0)
//   |  (0,1)  (1,1)  (2,1)
//   |  (0,2)  (1,2)  (2,2)
//   v
//
// Piece indices 0 through 15 (exclusive) correspond with piece types 1 through 16
// (exclusive) respectively. Each piece type is a bitmask of sides that are connected by
// paths:
//
//     1        index 0    index 1    index 2          index 9
//   . | .       . | .      .   .     . | .            .   .
// 8 --+-- 2       +          +--       \--      ..    --+--    etc.
//   . | .       .   .      .   .     .   .            .   .
//     4        type 1     type 2     type 3           type 10
//
// e.g. piece with index 9 has type 10 is a horizontal piece that connect right-to-left.

enum RectDirection implements Direction {
    UP(0, -1) {
        @Override
        public Direction opposite() {
            return DOWN;
        }
    },
    RIGHT(1, 0) {
        @Override
        public Direction opposite() {
            return LEFT;
        }
    },
    DOWN(0, +1) {
        @Override
        public Direction opposite() {
            return UP;
        }
    },
    LEFT(-1, 0) {
        @Override
        public Direction opposite() {
            return RIGHT;
        }
    };

    public static final ImmutableList<RectDirection> VALUES = ImmutableList.copyOf(values());

    RectDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public Pos step(Pos pos) {
        return new Pos(pos.x + dx, pos.y + dy);
    }

    @Override
    public boolean hasPath(int pieceIndex) {
        int type = pieceIndex + 1;
        int mask = 1 << ordinal();
        return (type & mask) == mask;
    }

    final int dx, dy;
}
