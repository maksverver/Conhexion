package ch.verver.chilab;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;

/**
 * Interface of classes that know how to draw a grid with puzzle pieces, and can translate
 * between pixel and grid coordinates.
 *
 * <p>Instances of this class are expected to be stateless; the relevant state is passed in to
 * each method by {@link BaseGridView}, which uses the drawer to render the puzzle grid.
 *
 * @see BaseGridView
 * @see DrawDimensions
 * @see ViewPort
 */
interface GridDrawer {
    /** Returns the grid position corresponding with the given pixel coordinates. */
    Pos calculateGridPos(DrawDimensions drawDimensions, float pixelX, float pixelY);

    /** Gets the pixel coordinates of the center of the field at the given grid coordinates. */
    PointF calculateFieldCenter(DrawDimensions drawDimensions, int x, int y);

    /**
     * Recalculates the draw dimensions for the grid, given the current zoom state.
     *
     * @param viewPort dimensions of the view port to draw in
     * @param gridBounds bounding box of the pieces on the grid
     * @param zoomFactor the current zoom factor, as a multiplier of the draw scale when the grid
     *                   is zoomed to fit the view; between 1 (zoomed out) and 10 (zoomed in)
     * @param zoomCx x-coordinate of the zoom center; between the {@link DrawDimensions#minZoomCx}
     *               and {@link DrawDimensions#maxZoomCx} of the previous draw dimensions
     * @param zoomCy y-coordinate of the zoom center; between the {@link DrawDimensions#minZoomCy}
     *               and {@link DrawDimensions#maxZoomCy} of the previous draw dimensions
     * @return the new draw dimensions for the current view size, piece positions, and zoom state
     */
    DrawDimensions calculateDrawDimensions(
            ViewPort viewPort, Rect gridBounds, float zoomFactor, float zoomCx, float zoomCy);

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
