package ch.verver.chilab;

interface Direction {
    Direction opposite();
    Pos step(Pos pos);
    boolean hasPath(int pieceIndex);
}
