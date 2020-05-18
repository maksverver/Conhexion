package ch.verver.chilab;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

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
interface GridDrawer {
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
     * <p>If a piece is currently being dragged, {@code draggedPieceIndex} is a nonnegative integer
     * and {@code dragDeltaX} and {@code dragDeltaY} give the current drag offset. If no piece is
     * being dragged, {@code draggedPieceIndex == -1} and {@code dragDeltaX} and {@code dragDeltaY}
     * should be ignored.
     */
    void draw(Canvas canvas, DrawDimensions drawDimensions,
              ReadonlyPiecePositionIndex piecePositions,
              int draggedPieceIndex, float dragDeltaX, float dragDeltaY);
}
