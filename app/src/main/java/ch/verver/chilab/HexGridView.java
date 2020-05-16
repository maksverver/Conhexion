package ch.verver.chilab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class HexGridView extends View {

    public interface PiecePositionsChangedListener {
        void piecePositionsChanged(HexGridView view);
    }

    private static final int GRID_PADDING = 1;
    private static final float SQRT3F = (float) Math.sqrt(3);
    private static final float MIN_ZOOM_FACTOR = 1.0f;
    private static final float MAX_ZOOM_FACTOR = 10.0f;

    private static final HexDirection BEAM_DRAW_ORDER[] = {
            HexDirection.SOUTH, HexDirection.NORTH_EAST, HexDirection.NORTH_WEST,
            HexDirection.SOUTH_EAST, HexDirection.SOUTH_WEST, HexDirection.NORTH};

    private static final HexDirection OVERLAP_ERROR_DIRECTIONS[] = {
            HexDirection.NORTH_EAST, HexDirection.SOUTH_EAST, HexDirection.SOUTH};

    private Context context;
    private Drawable tileBackground;
    private Drawable tileCenter;
    private EnumMap<HexDirection, Drawable> tileBeams;
    private EnumMap<HexDirection, Drawable> tileBacksides;
    private EnumMap<HexDirection, Drawable> tileOverlapErrors;
    private ScaleGestureDetector scaleGestureDetector;

    private PiecePositionIndex piecePositions = new PiecePositionIndex();

    // Current client zoom state.
    // (zoomCx, zoomCy) is the point that should be rendered at the center of the view.
    float zoomCx = 0.5f;  // between 0 and 1
    float zoomCy = 0.5f;  // between 0 and 1
    float zoomFactor = MIN_ZOOM_FACTOR;

    // Current draw dimensions. Depends on view dimensions (width, height, padding) and zoom state,
    // and should be recalculated on change.
    private DrawDimensions drawDimensions = new DrawDimensions(this);

    @Nullable private DragState dragState = null;

    @Nullable private PiecePositionsChangedListener piecePositionsChangedListener = null;

    public HexGridView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public HexGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public HexGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private Drawable getDrawable(@DrawableRes int id) {
        return ResourcesCompat.getDrawable(getResources(), id, context.getTheme());
    }

    private int getColor(@ColorRes int id) {
        return ResourcesCompat.getColor(getResources(), id, context.getTheme());
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        this.context = context;

        tileBackground = getDrawable(R.drawable.hex_background);
        tileCenter = getDrawable(R.drawable.hex_center);

        tileBeams = new EnumMap<>(HexDirection.class);
        tileBeams.put(HexDirection.NORTH, getDrawable(R.drawable.hex_north));
        tileBeams.put(HexDirection.NORTH_EAST, getDrawable(R.drawable.hex_north_east));
        tileBeams.put(HexDirection.SOUTH_EAST, getDrawable(R.drawable.hex_south_east));
        tileBeams.put(HexDirection.SOUTH, getDrawable(R.drawable.hex_south));
        tileBeams.put(HexDirection.SOUTH_WEST, getDrawable(R.drawable.hex_south_west));
        tileBeams.put(HexDirection.NORTH_WEST, getDrawable(R.drawable.hex_north_west));

        tileBacksides = new EnumMap<>(HexDirection.class);
        tileBacksides.put(HexDirection.NORTH, getDrawable(R.drawable.hex_back_north));
        tileBacksides.put(HexDirection.NORTH_EAST, getDrawable(R.drawable.hex_back_north_east));
        tileBacksides.put(HexDirection.SOUTH_EAST, getDrawable(R.drawable.hex_back_south_east));
        tileBacksides.put(HexDirection.SOUTH, getDrawable(R.drawable.hex_back_south));
        tileBacksides.put(HexDirection.SOUTH_WEST, getDrawable(R.drawable.hex_back_south_west));
        tileBacksides.put(HexDirection.NORTH_WEST, getDrawable(R.drawable.hex_back_north_west));

        tileOverlapErrors = new EnumMap<>(HexDirection.class);
        tileOverlapErrors.put(HexDirection.NORTH_EAST, getDrawable(R.drawable.hex_error_north_east));
        tileOverlapErrors.put(HexDirection.SOUTH_EAST, getDrawable(R.drawable.hex_error_south_east));
        tileOverlapErrors.put(HexDirection.SOUTH, getDrawable(R.drawable.hex_error_south));

        // Support pinch to zoom.
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            float beginZoomFactor;

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                LogUtil.v("Zoom started");
                beginZoomFactor = zoomFactor;
                return super.onScaleBegin(detector);
            }

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                setZoomFactor(beginZoomFactor * detector.getCurrentSpan() / detector.getPreviousSpan());
                return super.onScale(detector);
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                LogUtil.v("Zoom finished");
                super.onScaleEnd(detector);
            }
        });
    }

    public void setPiecePositionsChangedListener(@Nullable PiecePositionsChangedListener piecePositionsChangedListener) {
        this.piecePositionsChangedListener = piecePositionsChangedListener;
    }

    private void piecePositionsChanged() {
        if (piecePositionsChangedListener != null) {
            piecePositionsChangedListener.piecePositionsChanged(this);
        }
        // Grid bounding box may have changed.
        updateDrawDimensions();
    }

    public ArrayList<Pos> getPiecePositions() {
        return piecePositions.toList();
    }

    public PiecePositionIndex getPiecePositionIndex() {
        return new PiecePositionIndex(piecePositions);
    }

    public void setPiecePositions(List<Pos> positions) {
        if (positions.size() != HexPuzzle.PIECE_COUNT) {
            throw new IllegalArgumentException();
        }

        cancelDrag();
        piecePositions.assign(positions);
        invalidate();
        piecePositionsChanged();
    }

    private void updateDrawDimensions() {
        drawDimensions = new DrawDimensions(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cancelDrag();
        updateDrawDimensions();
        setZoomCenter(zoomCx, zoomCy);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int draggedIndex = dragState == null ? -1 : dragState.pieceIndex;

        // Draw grid in the background
        drawGridLines(canvas);

        // Draw pieces (except dragged one)
        for (int i = 0; i < piecePositions.size(); ++i) {
            if (i != draggedIndex) {
                drawPiece(canvas, i, piecePositions.get(i));
            }
        }

        // Draw overlap errors.
        drawOverlapErrors(canvas, draggedIndex);

        // Draw dragged piece last, so it's on top of everything else.
        if (draggedIndex != -1) {
            drawPiece(canvas, draggedIndex, piecePositions.get(draggedIndex),
                    dragState.deltaX, dragState.deltaY);
        }
    }

    private void drawGridLines(Canvas canvas) {
        final float viewWidth = drawDimensions.viewWidth;
        final float viewHeight = drawDimensions.viewHeight;
        final float scale = drawDimensions.scale;
        final float offsetX = drawDimensions.drawOffsetX;
        final float offsetY = drawDimensions.drawOffsetY;
        Paint paint = new Paint();
        paint.setColor(getColor(R.color.hexGridGridLines));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.1f * scale);
        paint.setStrokeCap(Paint.Cap.ROUND);

        int minQ = (int) Math.floor((-offsetX / scale - 2.0f) / 1.5f);
        int maxQ = (int) Math.ceil(((viewWidth - offsetX) / scale + 1.0f) / 1.5f);

        int minR = (int) Math.floor(-offsetY / (scale * SQRT3F) - 1.0f);
        int maxR = (int) Math.ceil((viewHeight - offsetY) / (scale * SQRT3F));

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
                canvas.drawLine(p2x, p2y, p3x, p3y, paint);
                canvas.drawLine(p3x, p3y, p4x, p4y, paint);
                canvas.drawLine(p4x, p4y, p5x, p5y, paint);
            }
        }
    }

    private Rect getTileBounds(Pos pos) {
        return getTileBounds(pos, 0.0f, 0.0f);
    }

    private Rect getTileBounds(Pos pos, float offsetX, float offsetY) {
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

    private static void draw(Canvas canvas, Rect bounds, Drawable drawable) {
        drawable.setBounds(bounds);
        drawable.draw(canvas);
    }

    private void drawPiece(Canvas canvas, int pieceIndex, Pos pos) {
        drawPiece(canvas, pieceIndex, pos, 0.0f, 0.0f);
    }

    private void drawPiece(Canvas canvas, int pieceIndex, Pos pos, float dragOffsetX, float dragOffsetY) {
        Rect bounds = getTileBounds(pos, dragOffsetX, dragOffsetY);

        draw(canvas, bounds, tileBackground);

        for (HexDirection direction : BEAM_DRAW_ORDER) {
            if (!direction.hasPath(pieceIndex)) {
                draw(canvas, bounds, tileBacksides.get(direction));
            }
        }

        draw(canvas, bounds, tileCenter);

        // Order in which beams are drawn matters! We want to draw back-to-front.
        for (HexDirection direction : BEAM_DRAW_ORDER) {
            if (direction.hasPath(pieceIndex)) {
                draw(canvas, bounds, tileBeams.get(direction));
            }
        }
    }

    private void drawOverlapErrors(Canvas canvas, int draggedIndex) {
        for (int i = 0; i < piecePositions.size(); ++i) {
            if (i != draggedIndex) {
                Pos pos = piecePositions.get(i);
                for (HexDirection direction : OVERLAP_ERROR_DIRECTIONS) {
                    int j = piecePositions.indexOf(direction.step(pos));
                    if (j != -1 && j != draggedIndex && (!direction.hasPath(i) || !direction.opposite().hasPath(j))) {
                        draw(canvas, getTileBounds(pos), tileOverlapErrors.get(direction));
                    }
                }
            }
        }
    }

    private void setZoomFactor(float newZoomFactor) {
        zoomFactor = clamp(newZoomFactor, MIN_ZOOM_FACTOR, MAX_ZOOM_FACTOR);
        updateDrawDimensions();  // must be called before setZoomCenter()!
        setZoomCenter(zoomCx, zoomCy);
        invalidate();
    }

    /** Moves the view by (x, y), measured in pixels. */
    private void dragViewBy(float deltaX, float deltaY) {
        setZoomCenter(
                zoomCx - deltaX / drawDimensions.renderedPixelWidth,
                zoomCy - deltaY / drawDimensions.renderedPixelHeight);
    }

    private void setZoomCenter(float cx, float cy) {
        zoomCx = clamp(cx, drawDimensions.minZoomCx, drawDimensions.maxZoomCx);
        zoomCy = clamp(cy, drawDimensions.minZoomCy, drawDimensions.maxZoomCy);
        updateDrawDimensions();
        invalidate();
    }

    private Pos calculateGridPos(float pixelX, float pixelY) {
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

    private int findPieceIndex(MotionEvent event) {
        return piecePositions.indexOf(calculateGridPos(event.getX(), event.getY()));
    }

    private PointF getFieldCenter(int q, int r) {
        boolean even = (q % 2) == 0;
        float scale = drawDimensions.scale;
        return new PointF(
                drawDimensions.drawOffsetX + scale * (1.0f + 1.5f * q),
                drawDimensions.drawOffsetY + scale * (SQRT3F * (even ? r + 0.5f : r + 1.0f)));
    }

    private void movePieceBy(int i, float deltaX, float deltaY) {
        Pos source = piecePositions.get(i);
        PointF origin = getFieldCenter(source.x, source.y);
        Pos destination = calculateGridPos(origin.x + deltaX, origin.y + deltaY);
        piecePositions.moveOrSwap(i, destination);
        piecePositionsChanged();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        // Handle pinch-to-zoom gesture.
        handled |= scaleGestureDetector.onTouchEvent(event);
        if (scaleGestureDetector.isInProgress()) {
            cancelDrag();
        } else {
            handled |= detectDrag(event);
        }
        return handled;
    }

    private boolean detectDrag(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragState = new DragState(event, findPieceIndex(event));
                LogUtil.v("Drag started");
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragState == null || !dragState.update(event)) {
                    return false;
                }
                if (dragState.pieceIndex == -1) {
                    dragViewBy(dragState.deltaDeltaX, dragState.deltaDeltaY);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (dragState == null || !dragState.update(event)) {
                    return false;
                }
                LogUtil.v("Drag finished");
                if (dragState.pieceIndex >= 0) {
                    movePieceBy(dragState.pieceIndex, dragState.deltaX, dragState.deltaY);
                }
                dragState = null;
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                return cancelDrag();
        }
        return false;
    }

    private boolean cancelDrag() {
        if (dragState != null) {
            dragState = null;
            LogUtil.v("Drag cancelled");
            invalidate();
        }
        return false;
    }

    private static class DragState {
        final int pointerId;
        final int pieceIndex;
        final float startX, startY;
        float lastX, lastY;
        float deltaX = 0.0f, deltaY = 0.0f;
        float deltaDeltaX = 0.0f, deltaDeltaY = 0.0f;

        private DragState(MotionEvent e, int pieceIndex) {
            this.startX = this.lastX = e.getX();
            this.startY = this.lastY = e.getY();
            this.pointerId = e.getPointerId(0);
            this.pieceIndex = pieceIndex;
        }

        private boolean update(MotionEvent e) {
            int pointerIndex = e.findPointerIndex(pointerId);
            if (pointerIndex < 0) {
                return false;
            }
            float x = e.getX(pointerIndex);
            float y = e.getY(pointerIndex);
            deltaDeltaX = x - lastX;
            deltaDeltaY = y - lastY;
            deltaX = x - startX;
            deltaY = y - startY;
            lastX = x;
            lastY = y;
            return true;
        }
    }

    private static class DrawDimensions {
        // Size of the view (in pixels)
        final float viewWidth;
        final float viewHeight;

        // Size of the view minus padding
        final float contentWidth;
        final float contentHeight;

        // Size of the bounding box of the grid, if hexagons are rendered with side length 1.
        final float renderedWidth;
        final float renderedHeight;

        // Maximal zoom factor so that rendered content fits the content area
        final float zoomToFit;

        // Side length of a hexagon drawn at the current zoom level.
        final float scale;

        // Size of the bounding box of the grid, when drawn at the current zoom level.
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

        DrawDimensions(HexGridView view) {
            Rect gridBounds = view.piecePositions.getBoundingRect();
            gridBounds.left -= GRID_PADDING;
            gridBounds.top -= GRID_PADDING;
            gridBounds.right += GRID_PADDING;
            gridBounds.bottom += GRID_PADDING;
            int gridWidth = gridBounds.right - gridBounds.left;
            int gridHeight = gridBounds.bottom - gridBounds.top;
            viewWidth = view.getWidth();
            viewHeight = view.getHeight();
            contentWidth = viewWidth - view.getPaddingLeft() - view.getPaddingRight();
            contentHeight = viewHeight - view.getPaddingTop() - view.getPaddingBottom();
            renderedWidth = 0.5f + 1.5f * gridWidth;
            renderedHeight = SQRT3F * (gridHeight + (gridWidth > 1 ? 0.5f : 0.0f));
            zoomToFit = Math.min(contentWidth/renderedWidth, contentHeight/renderedHeight);
            scale = zoomToFit * view.zoomFactor;
            renderedPixelWidth = scale * renderedWidth;
            renderedPixelHeight = scale * renderedHeight;
            minZoomCx = Math.min(0.5f*contentWidth / renderedPixelWidth, 0.5f);
            maxZoomCx = 1.0f - minZoomCx;
            minZoomCy = Math.min(0.5f*contentHeight / renderedPixelHeight, 0.5f);
            maxZoomCy = 1.0f - minZoomCy;
            drawOffsetX = 0.5f * viewWidth - renderedPixelWidth * view.zoomCx - scale * 1.5f * gridBounds.left;
            drawOffsetY = 0.5f * viewHeight - renderedPixelHeight * view.zoomCy - scale * SQRT3F * gridBounds.top;
        }
    }

    private static float clamp(float x, float min, float max) {
        return x < min ? min : x > max ? max : x;
    }

    private static class SavedState extends BaseSavedState {
        final List<Pos> piecePositions;
        final float zoomCx;
        final float zoomCy;
        final float zoomFactor;

        public SavedState(HexGridView view, Parcelable superState) {
            super(superState);
            piecePositions = view.piecePositions.toList();
            zoomCx = view.zoomCx;
            zoomCy = view.zoomCy;
            zoomFactor = view.zoomFactor;
        }

        public SavedState(Parcel in) {
            super(in);
            piecePositions = new ArrayList<>();
            in.readList(piecePositions, null);
            zoomCx = in.readFloat();
            zoomCy = in.readFloat();
            zoomFactor = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeList(piecePositions);
            out.writeFloat(zoomCx);
            out.writeFloat(zoomCy);
            out.writeFloat(zoomFactor);
        }

        void restore(HexGridView view) {
            view.setPiecePositions(piecePositions);

            view.zoomFactor = zoomFactor;
            view.zoomCx = zoomCx;
            view.zoomCy = zoomCy;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        LogUtil.v("HexGridView saving instance state");
        return new SavedState(this, super.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        LogUtil.v("HexGridView restoring instance state");
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        savedState.restore(this);
    }
}
