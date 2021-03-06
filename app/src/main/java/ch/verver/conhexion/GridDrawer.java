package ch.verver.conhexion;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;

/**
 * Interface of classes that know how to draw a grid with puzzle pieces, and can translate
 * between pixel and grid coordinates.
 *
 * <p>Instances of this class are expected to be stateless; the relevant state is passed in to
 * each method by {@link BaseGridView}, which uses the drawer to render the puzzle grid.
 *
 * @see BaseGridView
 * @see DrawDimensions
 */
interface GridDrawer<D extends Direction> {

    /**
     * Returns an array of grid directions along which pieces may be connected.
     * Used to detect connected components when selecting multiple pieces by long-pressing.
     */
    ImmutableList<D> getConnectionDirections();

    /** Returns an array of directions in which to check for overlap errors. */
    ImmutableList<D> getErrorDirections();

    /** Returns the grid position corresponding with the given pixel coordinates. */
    Pos calculateGridPos(DrawDimensions drawDimensions, float pixelX, float pixelY);

    /** Gets the pixel coordinates of the center of the field at the given grid coordinates. */
    PointF calculateFieldCenter(DrawDimensions drawDimensions, int gridX, int gridY);

    /**
     * Calculates the canvas bounds for the given grid bounds. The canvas bounds are the bounds of
     * the grid when drawn at unit size.
     */
    RectF calculateCanvasBounds(Rect gridBounds);

    /**
     * Draws the current grid and pieces.
     *
     * <p>{@code draggedPieces} is a bitmask of pieces currently being dragged. When nonzero,
     * {@code dragDeltaX} and {@code dragDeltaY} give the current drag offset. If no piece is
     * being dragged, {@code draggedPieces == 0} and {@code dragDeltaX} and {@code dragDeltaY}
     * should be ignored.
     */
    void draw(Canvas canvas, DrawDimensions drawDimensions,
              ReadonlyPiecePositionIndex piecePositions,
              ImmutableList<Pair<Pos, D>> overlapErrors,
              long draggedPieces, float dragDeltaX, float dragDeltaY);

    /**
     * Similar to {@link #draw}, but used to animate the view when the puzzle is solved.
     *
     * <p>@{code frameTime} is the time in seconds which will run from 0 to 12 currently.
     */
    void animateVictory(Canvas canvas, DrawDimensions drawDimensions,
                        ReadonlyPiecePositionIndex piecePositions, float frameTime);
}
