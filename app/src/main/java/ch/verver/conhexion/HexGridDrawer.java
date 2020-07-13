package ch.verver.conhexion;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.EnumMap;

// See HexDirection.java for a summary of the coordinate system used for the hex grid.
class HexGridDrawer implements GridDrawer<HexDirection> {

    private static final float SQRT3F = (float) Math.sqrt(3);

    private static final HexDirection BEAM_DRAW_ORDER[] = {
            HexDirection.SOUTH, HexDirection.NORTH_EAST, HexDirection.NORTH_WEST,
            HexDirection.SOUTH_EAST, HexDirection.SOUTH_WEST, HexDirection.NORTH};

    private static final ImmutableList<HexDirection> OVERLAP_ERROR_DIRECTIONS = ImmutableList.of(
            HexDirection.NORTH_EAST, HexDirection.SOUTH_EAST, HexDirection.SOUTH);

    private final Paint hexGridLinesPaint;
    private final DrawablePiece[] drawablePieces;
    private final EnumMap<HexDirection, Drawable> tileOverlapErrors;

    private class DrawablePiece {
        private final Drawable background;
        private final Drawable center;
        private final ImmutableList<Drawable> backsides;
        private final ImmutableList<Drawable> beams;

        DrawablePiece(
                Drawable background,
                Drawable center,
                ImmutableList<Drawable> backsides,
                ImmutableList<Drawable> beams) {
            this.background = background;
            this.center = center;
            this.backsides = backsides;
            this.beams = beams;
        }

        void draw(Canvas canvas, DrawDimensions drawDimensions,
                  Pos pos, float dragOffsetX, float dragOffsetY,
                @Nullable ColorFilter backColorFilter, @Nullable ColorFilter frontColorFilter) {
            Rect bounds = getTileBounds(drawDimensions, pos, dragOffsetX, dragOffsetY);

            HexGridDrawer.draw(canvas, bounds, background, backColorFilter);
            for (Drawable backside : backsides) {
                HexGridDrawer.draw(canvas, bounds, backside, backColorFilter);
            }

            HexGridDrawer.draw(canvas, bounds, center, frontColorFilter);
            for (Drawable beam : beams) {
                HexGridDrawer.draw(canvas, bounds, beam, frontColorFilter);
            }
        }
    }

    public HexGridDrawer(Resources res, @Nullable Resources.Theme theme) {
        hexGridLinesPaint = new Paint();
        hexGridLinesPaint.setColor(ResourcesCompat.getColor(res, R.color.hexGridGridLines, theme));
        hexGridLinesPaint.setStyle(Paint.Style.STROKE);
        hexGridLinesPaint.setStrokeCap(Paint.Cap.ROUND);

        drawablePieces = createDrawablePieces(res, theme);

        tileOverlapErrors = new EnumMap<>(HexDirection.class);
        tileOverlapErrors.put(HexDirection.NORTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_error_north_east, theme).mutate());
        tileOverlapErrors.put(HexDirection.SOUTH_EAST, ResourcesCompat.getDrawable(res, R.drawable.hex_error_south_east, theme).mutate());
        tileOverlapErrors.put(HexDirection.SOUTH, ResourcesCompat.getDrawable(res, R.drawable.hex_error_south, theme).mutate());
    }

    private DrawablePiece[] createDrawablePieces(Resources res, @Nullable Resources.Theme theme) {
        EnumMap<HexDirection, Integer> beamDrawableIds = new EnumMap<>(HexDirection.class);
        beamDrawableIds.put(HexDirection.NORTH, R.drawable.hex_beam_north);
        beamDrawableIds.put(HexDirection.NORTH_EAST, R.drawable.hex_beam_north_east);
        beamDrawableIds.put(HexDirection.SOUTH_EAST, R.drawable.hex_beam_south_east);
        beamDrawableIds.put(HexDirection.SOUTH, R.drawable.hex_beam_south);
        beamDrawableIds.put(HexDirection.SOUTH_WEST, R.drawable.hex_beam_south_west);
        beamDrawableIds.put(HexDirection.NORTH_WEST, R.drawable.hex_beam_north_west);

        EnumMap<HexDirection, Integer> backDrawableIds = new EnumMap<>(HexDirection.class);
        backDrawableIds.put(HexDirection.NORTH, R.drawable.hex_back_north);
        backDrawableIds.put(HexDirection.NORTH_EAST, R.drawable.hex_back_north_east);
        backDrawableIds.put(HexDirection.SOUTH_EAST, R.drawable.hex_back_south_east);
        backDrawableIds.put(HexDirection.SOUTH, R.drawable.hex_back_south);
        backDrawableIds.put(HexDirection.SOUTH_WEST, R.drawable.hex_back_south_west);
        backDrawableIds.put(HexDirection.NORTH_WEST, R.drawable.hex_back_north_west);

        DrawablePiece[] drawablePieces = new DrawablePiece[HexPuzzle.PIECE_COUNT];
        Drawable[] beamsBuffer = new Drawable[6];
        Drawable[] backsBuffer = new Drawable[6];
        for (int i = 0; i < drawablePieces.length; ++i) {
            // Calling mutate() creates a private copy of the loaded resource. This is necessary
            // because on API level 26, applying a color filter to the shared resource affects all
            // other drawables (even if the color filter is cleared between draw() calls) which
            // leads to incorrect rendering of selected pieces (specifically, it causes parts of
            // non-selected pieces to be highlighted as well).
            Drawable background = ResourcesCompat.getDrawable(res, R.drawable.hex_background, theme).mutate();
            Drawable center = ResourcesCompat.getDrawable(res, R.drawable.hex_center, theme).mutate();
            int beamsCount = 0;
            int backsCount = 0;
            for (HexDirection direction : BEAM_DRAW_ORDER) {
                if (direction.hasPath(i)) {
                    beamsBuffer[beamsCount++] = ResourcesCompat.getDrawable(res, beamDrawableIds.get(direction), theme).mutate();
                } else {
                    backsBuffer[backsCount++] = ResourcesCompat.getDrawable(res, backDrawableIds.get(direction), theme).mutate();
                }
            }
            drawablePieces[i] = new DrawablePiece(background, center,
                    ImmutableList.copyOf(backsBuffer, backsCount),
                    ImmutableList.copyOf(beamsBuffer, beamsCount));
        }
        return drawablePieces;
    }

    @Override
    public ImmutableList<HexDirection> getConnectionDirections() {
        return HexDirection.VALUES;
    }

    @Override
    public ImmutableList<HexDirection> getErrorDirections() {
        return OVERLAP_ERROR_DIRECTIONS;
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
    public void draw(
            Canvas canvas, DrawDimensions drawDimensions, ReadonlyPiecePositionIndex piecePositions,
            ImmutableList<Pair<Pos, HexDirection>> overlapErrors,
            long draggedPieces, float dragDeltaX, float dragDeltaY) {
        final int n = piecePositions.size();

        // Draw grid in the background
        drawGridLines(canvas, drawDimensions);

        // Draw pieces (except dragged one)
        for (int i = 0; i < n; ++i) {
            if (!Util.isDragged(draggedPieces, i)) {
                drawPiece(canvas, drawDimensions, i, piecePositions.get(i), 0.0f, 0.0f, null, null);
            }
        }

        // Draw overlap errors.
        drawOverlapErrors(canvas, drawDimensions, overlapErrors);

        // Draw dragged pieces last, so they're on top of everything else.
        if (draggedPieces != 0) {
            for (int i = 0; i < n; ++i) {
                if (Util.isDragged(draggedPieces, i)) {
                    drawPiece(canvas, drawDimensions, i, piecePositions.get(i),
                            dragDeltaX, dragDeltaY, ColorFilters.LIGHTER, null);
                }
            }
        }
    }

    @Override
    public void animateVictory(Canvas canvas, DrawDimensions drawDimensions, ReadonlyPiecePositionIndex piecePositions, float frameTime) {
        drawGridLines(canvas, drawDimensions);
        ColorFilter colorFilter = ColorFilters.hueShift(frameTime / 4.0f);
        for (int i = 0, n = piecePositions.size(); i < n; ++i) {
            drawPiece(canvas, drawDimensions, i, piecePositions.get(i), 0.0f, 0.0f, colorFilter, colorFilter);
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
                           float dragOffsetX, float dragOffsetY,
                           @Nullable ColorFilter backColorFilter, @Nullable ColorFilter frontColorFilter) {
        drawablePieces[pieceIndex].draw(canvas, drawDimensions, pos, dragOffsetX, dragOffsetY, backColorFilter, frontColorFilter);
    }

    private void drawOverlapErrors(Canvas canvas, DrawDimensions drawDimensions,
                                   ImmutableList<Pair<Pos, HexDirection>> overlapErrors) {
        for (Pair<Pos, HexDirection> error : overlapErrors) {
            Rect errorBounds = getTileBounds(drawDimensions, error.first);
            Drawable errorDrawable = tileOverlapErrors.get(error.second);
            draw(canvas, errorBounds, errorDrawable, null);
        }
    }
}
