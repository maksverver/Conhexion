package ch.verver.chilab;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

class RectGridDrawer implements GridDrawer {

    private final Drawable[] pieceBackDrawables;
    private final Drawable[] pieceFrontDrawables;
    private final Drawable overlapHorizDrawable;
    private final Drawable overlapVertiDrawable;
    private final Paint gridStrokePaint;
    private final Paint opaquePaint;

    public RectGridDrawer(Resources res, @Nullable Resources.Theme theme) {
        pieceBackDrawables = getDrawables(res, theme,
                R.drawable.rect_1_back,
                R.drawable.rect_2_back,
                R.drawable.rect_3_back,
                R.drawable.rect_4_back,
                R.drawable.rect_5_back,
                R.drawable.rect_6_back,
                R.drawable.rect_7_back,
                R.drawable.rect_8_back,
                R.drawable.rect_9_back,
                R.drawable.rect_10_back,
                R.drawable.rect_11_back,
                R.drawable.rect_12_back,
                R.drawable.rect_13_back,
                R.drawable.rect_14_back,
                R.drawable.rect_15_back);
        pieceFrontDrawables = getDrawables(res, theme,
                R.drawable.rect_1_front,
                R.drawable.rect_2_front,
                R.drawable.rect_3_front,
                R.drawable.rect_4_front,
                R.drawable.rect_5_front,
                R.drawable.rect_6_front,
                R.drawable.rect_7_front,
                R.drawable.rect_8_front,
                R.drawable.rect_9_front,
                R.drawable.rect_10_front,
                R.drawable.rect_11_front,
                R.drawable.rect_12_front,
                R.drawable.rect_13_front,
                R.drawable.rect_14_front,
                R.drawable.rect_15_front);
        overlapHorizDrawable = ResourcesCompat.getDrawable(res, R.drawable.rect_overlap_horiz, theme);
        overlapVertiDrawable = ResourcesCompat.getDrawable(res, R.drawable.rect_overlap_verti, theme);

        gridStrokePaint = new Paint();
        gridStrokePaint.setColor(ResourcesCompat.getColor(res, R.color.rectGridGridLines, theme));
        gridStrokePaint.setStyle(Paint.Style.STROKE);
        gridStrokePaint.setStrokeCap(Paint.Cap.ROUND);

        opaquePaint = new Paint();
        opaquePaint.setStyle(Paint.Style.FILL);
    }

    private static Drawable[] getDrawables(
            Resources res, @Nullable Resources.Theme theme, int... resourceIds) {
        Drawable[] drawables = new Drawable[resourceIds.length];
        for (int i = 0; i < drawables.length; ++i) {
            drawables[i] = ResourcesCompat.getDrawable(res, resourceIds[i], theme);
        }
        return drawables;
    }

    @Override
    public Direction[] getConnectionDirections() {
        return RectDirection.values();
    }

    @Override
    public Direction[] getErrorDirections() {
        return new Direction[] { RectDirection.UP, RectDirection.LEFT };
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
    public RectF calculateCanvasBounds(Rect gridBounds) {
        return new RectF(gridBounds);
    }

    @Override
    public void draw(
            Canvas canvas, DrawDimensions drawDimensions, ReadonlyPiecePositionIndex piecePositions,
            ImmutableList<Pair<Pos, Direction>> overlapErrors,
            long draggedPieces, float dragDeltaX, float dragDeltaY) {
        drawGridLines(canvas, drawDimensions);

        final int n = piecePositions.size();

        // Draw pieces
        for (int i = 0; i < n; ++i) {
            if (!Util.isDragged(draggedPieces, i)) {
                Pos pos = piecePositions.get(i);
                drawPiece(canvas, drawDimensions, i, pos.x, pos.y, 0.0f, 0.0f, null, null);
            }
        }

        // Draw overlap errors
        for (Pair<Pos, Direction> error : overlapErrors) {
            Pos pos = error.first;
            switch ((RectDirection) error.second) {
                case LEFT:
                    drawDrawable(canvas, drawDimensions, overlapVertiDrawable,
                            pos.x - 0.25f, (float) pos.y, pos.x + 0.25f, pos.y + 1.0f,
                            0.0f, 0.0f, null);
                    break;

                case UP:
                    drawDrawable(canvas, drawDimensions, overlapHorizDrawable,
                            pos.x, pos.y - 0.25f, pos.x + 1.0f, pos.y + 0.25f,
                            0.0f, 0.0f, null);
                    break;
            }
        }

        // Draw dragged pieces last, to ensure they are displayed on top!
        if (draggedPieces != 0) {
            for (int i = 0; i < n; ++i) {
                if (Util.isDragged(draggedPieces, i)) {
                    Pos pos = piecePositions.get(i);
                    drawPiece(canvas, drawDimensions, i, pos.x, pos.y, dragDeltaX, dragDeltaY, ColorFilters.LIGHTER, null);
                }
            }
        }
    }

    @Override
    public void animateVictory(Canvas canvas, DrawDimensions drawDimensions, ReadonlyPiecePositionIndex piecePositions,
                               float frameTime) {
        drawGridLines(canvas, drawDimensions);

        ColorFilter colorFilter = ColorFilters.hueShift(frameTime / 4.0f);
        for (int i = 0, n = piecePositions.size(); i < n; ++i) {
            Pos pos = piecePositions.get(i);
            drawPiece(canvas, drawDimensions, i, pos.x, pos.y, 0.0f, 0.0f, colorFilter, colorFilter);
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

    private void drawPiece(
            Canvas canvas, DrawDimensions drawDimensions, int pieceIndex, int gridX, int gridY,
            float pixelOffsetX, float pixelOffsetY,
            @Nullable ColorFilter backColorFilter, @Nullable ColorFilter frontColorFilter) {
        drawDrawable(canvas, drawDimensions, pieceBackDrawables[pieceIndex],
                gridX - 0.5f, gridY - 0.5f, gridX + 1.5f, gridY + 1.5f,
                pixelOffsetX, pixelOffsetY, backColorFilter);
        drawDrawable(canvas, drawDimensions, pieceFrontDrawables[pieceIndex],
                gridX - 0.5f, gridY - 0.5f, gridX + 1.5f, gridY + 1.5f,
                pixelOffsetX, pixelOffsetY, frontColorFilter);
    }

    private static void drawDrawable(
            Canvas canvas, DrawDimensions drawDimensions, Drawable drawable,
             float gridLeft, float gridTop, float gridRight, float gridBottom,
             float pixelOffsetX, float pixelOffsetY, @Nullable ColorFilter colorFilter) {
        drawable.setBounds(
                Math.round(gridToPixelX(drawDimensions, gridLeft) + pixelOffsetX),
                Math.round(gridToPixelY(drawDimensions, gridTop) + pixelOffsetY),
                Math.round(gridToPixelX(drawDimensions, gridRight) + pixelOffsetX),
                Math.round(gridToPixelY(drawDimensions, gridBottom) + pixelOffsetY));
        drawable.setColorFilter(colorFilter);
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
