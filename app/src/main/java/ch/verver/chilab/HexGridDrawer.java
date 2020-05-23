package ch.verver.chilab;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.EnumMap;

// See HexDirection.java for a summary of the coordinate system used for the hex grid.
class HexGridDrawer implements GridDrawer {

    private static final float SQRT3F = (float) Math.sqrt(3);

    private static final HexDirection BEAM_DRAW_ORDER[] = {
            HexDirection.SOUTH, HexDirection.NORTH_EAST, HexDirection.NORTH_WEST,
            HexDirection.SOUTH_EAST, HexDirection.SOUTH_WEST, HexDirection.NORTH};

    private static final HexDirection OVERLAP_ERROR_DIRECTIONS[] = {
            HexDirection.NORTH_EAST, HexDirection.SOUTH_EAST, HexDirection.SOUTH};

    private final Paint hexGridLinesPaint;
    private final Drawable tileBackground;
    private final Drawable tileCenter;
    private final EnumMap<HexDirection, Drawable> tileBeams;
    private final EnumMap<HexDirection, Drawable> tileBacksides;
    private final EnumMap<HexDirection, Drawable> tileOverlapErrors;

    public HexGridDrawer(Resources res, @Nullable Resources.Theme theme) {
        hexGridLinesPaint = new Paint();
        hexGridLinesPaint.setColor(ResourcesCompat.getColor(res, R.color.hexGridGridLines, theme));
        hexGridLinesPaint.setStyle(Paint.Style.STROKE);
        hexGridLinesPaint.setStrokeCap(Paint.Cap.ROUND);

        tileBackground = ResourcesCompat.getDrawable(res, R.drawable.hex_background, theme);
        tileCenter = ResourcesCompat.getDrawable(res, R.drawable.hex_center, theme);

        tileBeams = new EnumMap<>(HexDirection.class);
        tileBeams.put(HexDirection.NORTH, ResourcesCompat.getDrawable(res, R.drawable.hex_beam_north, theme));
        tileBeams.put(HexDirection.NORTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_beam_north_east, theme));
        tileBeams.put(HexDirection.SOUTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_beam_south_east, theme));
        tileBeams.put(HexDirection.SOUTH, ResourcesCompat.getDrawable(res, R.drawable.hex_beam_south, theme));
        tileBeams.put(HexDirection.SOUTH_WEST, ResourcesCompat.getDrawable(res, R.drawable.hex_beam_south_west, theme));
        tileBeams.put(HexDirection.NORTH_WEST, ResourcesCompat.getDrawable(res, R.drawable.hex_beam_north_west, theme));

        tileBacksides = new EnumMap<>(HexDirection.class);
        tileBacksides.put(HexDirection.NORTH, ResourcesCompat.getDrawable(res, R.drawable.hex_back_north, theme));
        tileBacksides.put(HexDirection.NORTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_back_north_east, theme));
        tileBacksides.put(HexDirection.SOUTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_back_south_east, theme));
        tileBacksides.put(HexDirection.SOUTH, ResourcesCompat.getDrawable(res, R.drawable.hex_back_south, theme));
        tileBacksides.put(HexDirection.SOUTH_WEST, ResourcesCompat.getDrawable(res, R.drawable.hex_back_south_west, theme));
        tileBacksides.put(HexDirection.NORTH_WEST, ResourcesCompat.getDrawable(res, R.drawable.hex_back_north_west, theme));

        tileOverlapErrors = new EnumMap<>(HexDirection.class);
        tileOverlapErrors.put(HexDirection.NORTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_error_north_east, theme));
        tileOverlapErrors.put(HexDirection.SOUTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_error_south_east, theme));
        tileOverlapErrors.put(HexDirection.SOUTH, ResourcesCompat.getDrawable(res, R.drawable.hex_error_south, theme));
    }

    @Override
    public Direction[] getConnectionDirections() {
        return HexDirection.values();
    }

    @Override
    public Pos calculateGridPos(DrawDimensions drawDimensions, float pixelX, float pixelY) {
        float renderX = (pixelX - drawDimensions.drawOffsetX)/drawDimensions.scale;
        float renderY = (pixelY - drawDimensions.drawOffsetY)/drawDimensions.scale;

        //   a  b  a' b'  a
        //  .5  1 .5  1  .5   etc.
        //  |-|---|-|---|-|-
        //  | |   | |   | |
        //  | +---+ |   | +-
        //  |/|   |\|   |/|
        //  + |   | +---+ |
        //  |\|   |/|   |\|
        //  | +---+ |   | +-
        //  |/|   |\|   |/|
        //  + |   | +---+ |
        //     even  odd

        int q = (int) Math.floor(renderX / 1.5f);
        boolean even = q % 2 == 0;
        int r = (int) Math.floor(renderY / SQRT3F - (even ? 0.0f : 0.5f));
        if (renderX - Math.floor(renderX / 1.5f) * 1.5 < 0.5f) {
            float xx = (float) (renderX - Math.floor(renderX / 1.5f) * 1.5f) * 2.0f;  // [0..1]
            float y = renderY - (even ? 0 : 0.5f * SQRT3F);
            float yy = (float) (y - Math.floor(y / SQRT3F) * SQRT3F) / (0.5f * SQRT3F);  // [0..2]

            //         xx
            //       0     1
            //     0 +-----+
            //       |   / |
            //       | /   |
            // yy  1 +     |
            //       | \   |
            //       |   \ |
            //     2 +-----+
            if (yy < 1.0f && xx < 1.0f - yy) {
                --q;
                if (even) {
                    --r;
                }
            } else if (yy > 1.0f && xx < yy - 1.0f) {
                --q;
                if (!even) {
                    ++r;
                }
            }
        }
        return new Pos(q, r);
    }

    @Override
    public PointF calculateFieldCenter(DrawDimensions drawDimensions, int q, int r) {
        boolean even = (q % 2) == 0;
        float scale = drawDimensions.scale;
        return new PointF(
                drawDimensions.drawOffsetX + scale * (1.0f + 1.5f * q),
                drawDimensions.drawOffsetY + scale * (SQRT3F * (even ? r + 0.5f : r + 1.0f)));
    }

    @Override
    public RectF calculateCanvasBounds(Rect gridBounds) {
        // Size of the bounding box of the grid, if hexagons are rendered with side length 1.
        float left = 1.5f * gridBounds.left;
        float top = SQRT3F * gridBounds.top;
        float right = 1.5f * gridBounds.right + 0.5f;
        float bottom = SQRT3F * (gridBounds.bottom + 0.5f);
        return new RectF(left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas, DrawDimensions drawDimensions, ReadonlyPiecePositionIndex piecePositions, long draggedPieces, float dragDeltaX, float dragDeltaY) {
        final int n = piecePositions.size();

        // Draw grid in the background
        drawGridLines(canvas, drawDimensions);

        // Draw pieces (except dragged one)
        for (int i = 0; i < n; ++i) {
            if (!Util.isDragged(draggedPieces, i)) {
                drawPiece(canvas, drawDimensions, i, piecePositions.get(i), 0.0f, 0.0f, null);
            }
        }

        // Draw overlap errors.
        drawOverlapErrors(canvas, drawDimensions, piecePositions, draggedPieces);

        // Draw dragged pieces last, so they're on top of everything else.
        if (draggedPieces != 0) {
            ColorFilter colorFilter = Util.isMultiDrag(draggedPieces) ? ColorFilters.NEGATIVE : null;
            for (int i = 0; i < n; ++i) {
                if (Util.isDragged(draggedPieces, i)) {
                    drawPiece(canvas, drawDimensions, i, piecePositions.get(i),
                            dragDeltaX, dragDeltaY, colorFilter);
                }
            }
        }
    }

    private void drawGridLines(Canvas canvas, DrawDimensions drawDimensions) {
        final float width = canvas.getWidth();
        final float height = canvas.getHeight();
        final float scale = drawDimensions.scale;
        final float offsetX = drawDimensions.drawOffsetX;
        final float offsetY = drawDimensions.drawOffsetY;

        int minQ = (int) Math.floor((-offsetX / scale - 2.0f) / 1.5f);
        int maxQ = (int) Math.ceil(((width - offsetX) / scale + 1.0f) / 1.5f);

        int minR = (int) Math.floor(-offsetY / (scale * SQRT3F) - 1.0f);
        int maxR = (int) Math.ceil((height - offsetY) / (scale * SQRT3F));

        hexGridLinesPaint.setStrokeWidth(0.1f * scale);

        for (int q = minQ; q < maxQ; ++q) {
            for (int r = minR; r < maxR; ++r) {
                boolean even = (q % 2) == 0;
                float cx = offsetX + scale * (1.0f + 1.5f * q);
                float cy = offsetY + scale * SQRT3F * (even ? r + 0.5f : r + 1.0f);

                // Points on hex perimeter:
                //
                //    1  2
                //        \
                //  0      3
                //        /
                //    5--4
                float p2x = cx + 0.5f * scale, p2y = cy - 0.5f * scale * SQRT3F;
                float p3x = cx + 1.0f * scale, p3y = cy;
                float p4x = cx + 0.5f * scale, p4y = cy + 0.5f * scale * SQRT3F;
                float p5x = cx - 0.5f * scale, p5y = cy + 0.5f * scale * SQRT3F;
                canvas.drawLine(p2x, p2y, p3x, p3y, hexGridLinesPaint);
                canvas.drawLine(p3x, p3y, p4x, p4y, hexGridLinesPaint);
                canvas.drawLine(p4x, p4y, p5x, p5y, hexGridLinesPaint);
            }
        }
    }

    private static Rect getTileBounds(DrawDimensions drawDimensions, Pos pos) {
        return getTileBounds(drawDimensions, pos, 0.0f, 0.0f);
    }

    private static Rect getTileBounds(DrawDimensions drawDimensions, Pos pos, float offsetX, float offsetY) {
        int q = pos.x;
        int r = pos.y;

        boolean even = (q % 2) == 0;
        float scale = drawDimensions.scale;
        float x1 = drawDimensions.drawOffsetX + scale * 1.5f * q + offsetX;
        float y1 = drawDimensions.drawOffsetY + scale * (SQRT3F * (even ? r + 0.5f : r + 1.0f) - 1.0f) + offsetY;
        float x2 = x1 + drawDimensions.scale * 2.0f;
        float y2 = y1 + drawDimensions.scale * 2.0f;

        return new Rect(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2));
    }

    private static void draw(Canvas canvas, Rect bounds, Drawable drawable, @Nullable ColorFilter colorFilter) {
        drawable.setBounds(bounds);
        drawable.setColorFilter(colorFilter);
        drawable.draw(canvas);
    }

    private void drawPiece(Canvas canvas, DrawDimensions drawDimensions, int pieceIndex, Pos pos,
                           float dragOffsetX, float dragOffsetY, @Nullable ColorFilter colorFilter) {
        Rect bounds = getTileBounds(drawDimensions, pos, dragOffsetX, dragOffsetY);

        draw(canvas, bounds, tileBackground, colorFilter);

        for (HexDirection direction : BEAM_DRAW_ORDER) {
            if (!direction.hasPath(pieceIndex)) {
                draw(canvas, bounds, tileBacksides.get(direction), colorFilter);
            }
        }

        draw(canvas, bounds, tileCenter, null);

        // Order in which beams are drawn matters! We want to draw back-to-front.
        for (HexDirection direction : BEAM_DRAW_ORDER) {
            if (direction.hasPath(pieceIndex)) {
                draw(canvas, bounds, tileBeams.get(direction), null);
            }
        }
    }

    private void drawOverlapErrors(Canvas canvas, DrawDimensions drawDimensions,
                                   ReadonlyPiecePositionIndex piecePositions, long draggedPieces) {
        for (int i = 0, n = piecePositions.size(); i < n; ++i) {
            if (!Util.isDragged(draggedPieces, i))  {
                Pos pos = piecePositions.get(i);
                for (HexDirection direction : OVERLAP_ERROR_DIRECTIONS) {
                    int j = piecePositions.indexOf(direction.step(pos));
                    if (j != -1 && !Util.isDragged(draggedPieces, j) &&
                            (!direction.hasPath(i) || !direction.opposite().hasPath(j))) {
                        draw(canvas, getTileBounds(drawDimensions, pos), tileOverlapErrors.get(direction), null);
                    }
                }
            }
        }
    }
}
