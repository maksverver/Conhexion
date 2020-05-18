package ch.verver.chilab;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

class RectGridDrawer implements GridDrawer {

    private final Drawable[] pieceDrawables;
    private final Drawable overlapHorizDrawable;
    private final Drawable overlapVertiDrawable;
    private final Paint gridStrokePaint;
    private final Paint opaquePaint;

    public RectGridDrawer(Resources res, @Nullable Resources.Theme theme) {
        final int[] pieceDrawableIds = {
                R.drawable.rect_1,
                R.drawable.rect_2,
                R.drawable.rect_3,
                R.drawable.rect_4,
                R.drawable.rect_5,
                R.drawable.rect_6,
                R.drawable.rect_7,
                R.drawable.rect_8,
                R.drawable.rect_9,
                R.drawable.rect_10,
                R.drawable.rect_11,
                R.drawable.rect_12,
                R.drawable.rect_13,
                R.drawable.rect_14,
                R.drawable.rect_15,
        };
        pieceDrawables = new Drawable[pieceDrawableIds.length];
        for (int i = 0; i < pieceDrawableIds.length; ++i) {
            pieceDrawables[i] = ResourcesCompat.getDrawable(res, pieceDrawableIds[i], theme);
        }
        overlapHorizDrawable = ResourcesCompat.getDrawable(res, R.drawable.rect_overlap_horiz, theme);
        overlapVertiDrawable = ResourcesCompat.getDrawable(res, R.drawable.rect_overlap_verti, theme);

        gridStrokePaint = new Paint();
        gridStrokePaint.setColor(ResourcesCompat.getColor(res, R.color.rectGridGridLines, theme));
        gridStrokePaint.setStyle(Paint.Style.STROKE);
        gridStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        opaquePaint = new Paint();
        opaquePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public Pos calculateGridPos(DrawDimensions drawDimensions, float pixelX, float pixelY) {
        return new Pos(
                (int) Math.floor(pixelToGridX(drawDimensions, pixelX)),
                (int) Math.floor(pixelToGridY(drawDimensions, pixelY)));
    }

    @Override
    public PointF calculateFieldCenter(DrawDimensions drawDimensions, int gridX, int gridY) {
        return new PointF(
                gridToPixelX(drawDimensions, gridX + 0.5f),
                gridToPixelY(drawDimensions, gridY + 0.5f));
    }

    @Override
    public DrawDimensions calculateDrawDimensions(ViewPort viewPort, Rect gridBounds, float zoomFactor, float zoomCx, float zoomCy) {
        int gridWidth = gridBounds.width();
        int gridHeight = gridBounds.height();
        int usedWidth = Math.min(viewPort.contentWidth, viewPort.contentHeight * gridWidth / gridHeight);
        int usedHeight = Math.min(viewPort.contentHeight, viewPort.contentWidth * gridHeight / gridWidth);
        float zoomToFit = Math.min(
                (float) viewPort.contentWidth / gridWidth,
                (float) viewPort.contentHeight / gridHeight);
        float drawScale = zoomToFit * zoomFactor;

        float renderedPixelWidth = drawScale * gridWidth;
        float renderedPixelHeight = drawScale * gridHeight;

        float drawOffsetX = viewPort.paddingLeft + viewPort.contentWidth / 2
                - drawScale * gridWidth * zoomCx - gridBounds.left * drawScale;
        float drawOffsetY = viewPort.paddingTop + viewPort.contentHeight / 2
                - drawScale * gridHeight * zoomCy - gridBounds.top * drawScale;

        float minZoomCx = Math.min(0.5f * viewPort.contentWidth / renderedPixelWidth, 0.5f);
        float maxZoomCx = 1.0f - minZoomCx;
        float minZoomCy = Math.min(0.5f * viewPort.contentHeight / renderedPixelHeight, 0.5f);
        float maxZoomCy = 1.0f - minZoomCy;

        return new DrawDimensions(drawScale, usedWidth, usedHeight,
                minZoomCx, maxZoomCx, minZoomCy, maxZoomCy, drawOffsetX, drawOffsetY);
    }

    @Override
    public void draw(Canvas canvas, DrawDimensions drawDimensions, ReadonlyPiecePositionIndex piecePositions,
                     int draggedPieceIndex, float dragDeltaX, float dragDeltaY) {
         drawGridLines(canvas, drawDimensions);

        // Draw pieces
        for (int i = 0; i < piecePositions.size(); ++i) {
            if (i != draggedPieceIndex) {
                Pos pos = piecePositions.get(i);
                drawPiece(canvas, drawDimensions, i, pos.x, pos.y);
            }
        }

        // Draw overlap errors
        for (int i = 0; i < piecePositions.size(); ++i) {
            Pos pos = piecePositions.get(i);
            if (i != draggedPieceIndex) {
                int j = piecePositions.indexOf(pos.x - 1, pos.y);
                if (j != -1 && j != draggedPieceIndex &&
                        (!RectDirection.LEFT.hasPath(i) || !RectDirection.RIGHT.hasPath(j))) {
                    drawDrawable(canvas, drawDimensions, overlapVertiDrawable,
                            pos.x - 0.25f, pos.y, pos.x + 0.25f, pos.y + 1.0f);
                }

                j = piecePositions.indexOf(pos.x, pos.y - 1);
                if (j != -1 && j != draggedPieceIndex &&
                        (!RectDirection.UP.hasPath(i) || !RectDirection.DOWN.hasPath(j))) {
                    drawDrawable(canvas, drawDimensions, overlapHorizDrawable,
                            pos.x, pos.y - 0.25f, pos.x + 1.0f, pos.y + 0.25f);
                }
            }
        }

        // Draw dragged piece last to ensure it is displayed on top!
        if (draggedPieceIndex != -1) {
            Pos pos = piecePositions.get(draggedPieceIndex);
            drawPiece(canvas, drawDimensions, draggedPieceIndex, pos.x, pos.y, dragDeltaX, dragDeltaY);
        }
    }

    private void drawGridLines(Canvas canvas, DrawDimensions drawDimensions) {
        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        float scale = drawDimensions.scale;
        gridStrokePaint.setStrokeWidth(0.05f * scale);
        int y = (int) drawDimensions.drawOffsetY;
        while (y > 0) {
            y -= scale;
        }
        while (y < viewHeight) {
            canvas.drawLine(0, y, viewWidth, y, gridStrokePaint);
            y += scale;
        }
        int x = (int) drawDimensions.drawOffsetX;
        while (x > 0) {
            x -= scale;
        }
        while (x < viewWidth) {
            canvas.drawLine(x, 0, x, viewHeight, gridStrokePaint);
            x += scale;
        }
    }

    private void drawPiece(Canvas canvas, DrawDimensions drawDimensions, int pieceIndex, int gridX, int gridY) {
        drawPiece(canvas, drawDimensions, pieceIndex, gridX, gridY, 0.0f, 0.0f);
    }

    private void drawPiece(Canvas canvas, DrawDimensions drawDimensions, int pieceIndex, int gridX, int gridY, float pixelOffsetX, float pixelOffsetY) {
        if (pieceIndex >= 0 && pieceIndex < pieceDrawables.length) {
            Drawable pieceDrawable = pieceDrawables[pieceIndex];
            if (pieceDrawable != null) {
                drawDrawable(canvas, drawDimensions, pieceDrawable,
                        gridX - 0.5f, gridY - 0.5f, gridX + 1.5f, gridY + 1.5f,
                        pixelOffsetX, pixelOffsetY);
                return;
            }
        }
        // Fallback: draw piece as a colored square
        opaquePaint.setColor(Color.rgb(255 * pieceIndex / 14, 0, 255 - 255 * pieceIndex / 14));
        canvas.drawRect(
                gridToPixelX(drawDimensions, gridX) + pixelOffsetX,
                gridToPixelY(drawDimensions, gridY) + pixelOffsetY,
                gridToPixelX(drawDimensions, gridX + 1) + pixelOffsetX,
                gridToPixelY(drawDimensions, gridY + 1) + pixelOffsetY,
                opaquePaint);
    }

    private static void drawDrawable(Canvas canvas, DrawDimensions drawDimensions, Drawable drawable,
            float gridLeft, float gridTop, float gridRight, float gridBottom) {
        drawDrawable(canvas, drawDimensions, drawable, gridLeft, gridTop, gridRight, gridBottom, 0.0f, 0.0f);
    }

    private static void drawDrawable(Canvas canvas, DrawDimensions drawDimensions, Drawable drawable,
            float gridLeft, float gridTop, float gridRight, float gridBottom,
             float pixelOffsetX, float pixelOffsetY) {
        drawable.setBounds(
                Math.round(gridToPixelX(drawDimensions, gridLeft) + pixelOffsetX),
                Math.round(gridToPixelY(drawDimensions, gridTop) + pixelOffsetY),
                Math.round(gridToPixelX(drawDimensions, gridRight) + pixelOffsetX),
                Math.round(gridToPixelY(drawDimensions, gridBottom) + pixelOffsetY));
        drawable.draw(canvas);
    }

    private static float gridToPixelX(DrawDimensions drawDimensions, float x) {
        return drawDimensions.drawOffsetX + drawDimensions.scale * x;
    }

    private static float gridToPixelY(DrawDimensions drawDimensions, float y) {
        return drawDimensions.drawOffsetY + drawDimensions.scale * y;
    }

    private static float pixelToGridX(DrawDimensions drawDimensions, float x) {
        return (x - drawDimensions.drawOffsetX) / drawDimensions.scale;
    }

    private static float pixelToGridY(DrawDimensions drawDimensions, float y) {
        return (y - drawDimensions.drawOffsetY) / drawDimensions.scale;
    }
}
