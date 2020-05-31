package ch.verver.chilab;

// Coordinate system:
//
//     +---+       +---+       +---+
//    /     \     /     \     /     \
//   +  0,0  +---+  2,0  +---+  4,0  +
//    \     /     \     /     \     /
//     +---+  1,0  +---+  3,0  +---+
//    /     \     /     \     /     \
//   +  0,1  +---+  2,1  +---+  4,1  +
//    \     /     \     /     \     /
//     +---+  1,1  +---+  3,1  +---+
//    /     \     /     \     /     \
//   +  0,2  +---+  2,2  +---+  4,2  +
//    \     /     \     /     \     /
//     +---+  1,2  +---+  3,2  +---+
//          \     /     \     /
//           +---+       +---+
//
//
// Piece indices 0 through 63 (exclusive) correspond with piece types 1 through 64
// (exclusive) respectively. Each piece type is a bitmask of sides that are connected by
// paths:
//
//          1
//        +---+
//   32  /     \ 2
//      +   .   +
//   16  \     / 4
//        +---+
//          8

enum HexDirection implements Direction {

    NORTH {
        @Override
        public Direction opposite() {
            return SOUTH;
        }

        @Override
        public Pos step(Pos pos) {
            return new Pos(pos.x, pos.y - 1);
        }
    },

    NORTH_EAST {
        @Override
        public Direction opposite() {
            return SOUTH_WEST;
        }

        @Override
        public Pos step(Pos pos) {
            return new Pos(pos.x + 1, pos.y + (pos.x & 1) - 1);
        }
    },

    SOUTH_EAST {
        @Override
        public Direction opposite() {
            return NORTH_WEST;
        }

        @Override
        public Pos step(Pos pos) {
            return new Pos(pos.x + 1, pos.y + (pos.x & 1));
        }
    },

    SOUTH {
        @Override
        public Direction opposite() {
            return NORTH;
        }

        @Override
        public Pos step(Pos pos) {
            return new Pos(pos.x, pos.y + 1);
        }
    },

    SOUTH_WEST {
        @Override
        public Direction opposite() {
            return NORTH_EAST;
        }

        @Override
        public Pos step(Pos pos) {
            return new Pos(pos.x - 1, pos.y + (pos.x & 1));
        }
    },

    NORTH_WEST {
        @Override
        public Direction opposite() {
            return SOUTH_EAST;
        }

        @Override
        public Pos step(Pos pos) {
            return new Pos(pos.x - 1, pos.y + (pos.x & 1) - 1);
        }
    };

    public static final ImmutableList<HexDirection> VALUES = ImmutableList.copyOf(values());

    @Override
    public boolean hasPath(int pieceIndex) {
        int type = pieceIndex + 1;
        int mask = 1 << ordinal();
        return (type & mask) == mask;
    }
}
