Conhexion is a tiling puzzle that was originally conceived by Martin Medema,
who called it "The China Labyrinth":
http://www.mindsports.nl/index.php/puzzles/tilings/china-labyrinth/

I released this app under a different name to avoid spoiling the solution, and
because I thought the original name was not very fitting: the puzzle is not
Chinese and does not involve constructing a labyrinth (though with some
imagination solutions could be said to resemble the Great Wall of China).

The goal is to mark 64 fields on a hexagonal grid, so that each marked field
differs from the others in terms of which adjacent fields are marked too. (This
matches the notion of "transcendental solutions" on the MindSports page.)

This app presents the puzzle in a slightly different way:

  - Instead of asking for the placement of 64 identical pieces, this app
    explicitly marks the expected connection on each piece, creating 64 distinct
    pieces instead. Although this sometimes requires moving pieces around (since
    it's no longer sufficient to select 64 correct fields; which piece is placed
    where matters, too) it actually makes it easier to understand the goal of
    the puzzle and helps with visualizing solutions.

    For the same reason, the sides of pieces that should not touch another piece
    protrude, so that mismatched pieces overlap, which makes these errors more
    clear.

  - The 1 piece without any neighbors is omitted; adding this piece to any
    63-piece solution is trivial.

  - This app requires all pieces to be connected into a *single* group (excluding
    the piece mentioned above). This poses an additional challenge.

The app includes a tutorial level (played with 15 pieces on a rectangular grid)
and the real puzzle (played with 63 pieces on a hexagonal grid).

Hints for the hexagonal grid puzzle:

   - It's easier to construct a symmetrical solution!
   - A solution must have exactly two holes. (Proof?)
