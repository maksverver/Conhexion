package ch.verver.conhexion;

interface Direction {
    Direction opposite();
    Pos step(Pos pos);
    boolean hasPath(int pieceIndex);
}
