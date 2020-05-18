package ch.verver.chilab;

// When drawing grids there are three important coordinate systems to consider.
//
// - Grid bounds (Rect)
//   Dimensions of the logical grid, always given in integer sizes. For example, if the grid bounds
//   form a bounding box of the pieces, then for every piece position left <= pos.x < right and
//   top <= pos.y < bottom. Note that BaseGridView adds 1 field padding around the bounding box
//   returned by piecePositionIndex.getBoundingBox().
//
// - Canvas bounds (RectF)
//   Dimensions of the canvas when the grid is drawn at a scale of unit length. For the rectangular
//   grid, each square is simply 1 unit wide and 1 unit tall, so the canvas bounds match the grid
//   bounds. For the hexagonal grid however, each hexagon is 1.5 units wide, and sqrt(3) units tall,
//   plus some extra space around the edge to accommodate the alternating columns of the grid, so
//   the canvas bounds are larger than the grid bounds.
//
// - Viewport: consisting of the view dimensions and padding. The content area is the view area
//   minus padding.
//
// The job of the GridDrawer implementation is to convert grid bounds to canvas bounds. BaseGridView
// then calculates the maximum scale based on the viewport content area and the canvas bounds. The
// actual scale is obtained by multiplying by the current zoom factor. Finally, offsetX and offsetY
// indicate the translation due to panning when drawing the grid (in pixels).
class DrawDimensions {
    // Size in pixels of a grid cell (e.g., length of the side of a square or hexagon).
    final float scale;

    // Pixel coordinates of the origin of the grid, when drawn at the current zoom level and center,
    // including viewport padding.
    final float drawOffsetX;
    final float drawOffsetY;

    DrawDimensions(float scale, float drawOffsetX, float drawOffsetY) {
        this.scale = scale;
        this.drawOffsetX = drawOffsetX;
        this.drawOffsetY = drawOffsetY;
    }
}
