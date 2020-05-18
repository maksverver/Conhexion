package ch.verver.chilab;

class DrawDimensions {
    // Size in pixels of a grid cell (e.g. length of the side of a square or hexagon).
    final float scale;

    // Size in pixels of the bounding box of the grid, when drawn at the current zoom level.
    final float renderedPixelWidth;
    final float renderedPixelHeight;

    // Bounds on the zoom center which keeps the grid in view.
    final float minZoomCx;  // between 0 and 0.5
    final float maxZoomCx;  // between 0.5 and 1
    final float minZoomCy;  // between 0 and 0.5
    final float maxZoomCy;  // between 0.5 and 1

    // Pixel coordinates of the top left corner of the grid, when drawn at the current zoom
    // level and center, and including padding.
    final float drawOffsetX;
    final float drawOffsetY;

    DrawDimensions(float scale, float renderedPixelWidth, float renderedPixelHeight,
                   float minZoomCx, float maxZoomCx, float minZoomCy, float maxZoomCy,
                   float drawOffsetX, float drawOffsetY) {
        this.scale = scale;
        this.renderedPixelWidth = renderedPixelWidth;
        this.renderedPixelHeight = renderedPixelHeight;
        this.minZoomCx = minZoomCx;
        this.maxZoomCx = maxZoomCx;
        this.minZoomCy = minZoomCy;
        this.maxZoomCy = maxZoomCy;
        this.drawOffsetX = drawOffsetX;
        this.drawOffsetY = drawOffsetY;
    }
}
