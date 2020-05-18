package ch.verver.chilab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for grid-based puzzle views.
 *
 * <p>This class contains the {@link PiecePositionIndex} and the drag and zoom state. It is
 * instantiated with a {@link GridDrawer} that is used to draw the grid and its pieces, and to
 * map from pixel coordinates to grid coordinates and back.
 */
abstract class BaseGridView extends View {
    private static final int GRID_PADDING = 1;

    private static final float MIN_ZOOM_FACTOR = 1.0f;
    private static final float MAX_ZOOM_FACTOR = 10.0f;

    private final GridDrawer gridDrawer;
    private final ScaleGestureDetector scaleGestureDetector;

    // Current piece positions
    private PiecePositionIndex piecePositions = new PiecePositionIndex();
    private ReadonlyPiecePositionIndex readonlyPiecePositions = piecePositions.readonlyWrapper();

    // Current bounding box of piece positions. Updated whenever piece positions change.
    private Rect gridBounds = piecePositions.getBoundingRect();

    // Current grid canvas size. Updated whenever piece positions change.
    private RectF canvasBounds;

    // Current draw dimensions. Depends on view dimensions (width, height, padding) and zoom state,
    // and should be recalculated on change.
    private DrawDimensions drawDimensions = null;

    // Current bounds on the zoom center. Recalculated whenever DrawDimensions change.
    // `left` and `top` must be between 0 and 0.5. `top` and `right` must be between 0.5 and 1.
    private RectF zoomCenterBounds = new RectF(0.5f, 0.5f, 0.5f, 0.5f);

    // Current client zoom state.
    // (zoomCx, zoomCy) is the point that should be rendered at the center of the view.
    private float zoomCx = 0.5f;  // between zoomCenterBounds.left and zoomCenterBounds.right
    private float zoomCy = 0.5f;  // between zoomCenterBounds.top and zoomCenterBounds.bottom
    private float zoomFactor = MIN_ZOOM_FACTOR;

    // Current drag state. null when nothing is being dragged.
    @Nullable private DragState dragState = null;

    // Callback called whenever the piece positions change.
    @Nullable private PiecePositionsChangedListener piecePositionsChangedListener = null;

    public interface PiecePositionsChangedListener {
        void piecePositionsChanged(BaseGridView baseGridView);
    }

    public BaseGridView(Context context, GridDrawer gridDrawer) {
        super(context);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        init();
    }

    public BaseGridView(Context context, GridDrawer gridDrawer, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        init();
    }

    public BaseGridView(Context context, GridDrawer gridDrawer, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        init();
    }

    public BaseGridView(Context context, GridDrawer gridDrawer, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        init();
    }

    private void init() {
        updateCanvasBounds();
    }

    public ArrayList<Pos> getPiecePositions() {
        return piecePositions.toList();
    }

    public PiecePositionIndex getPiecePositionIndex() {
        return new PiecePositionIndex(piecePositions);
    }

    public void setPiecePositions(List<Pos> positions) {
        cancelDrag();
        piecePositions.assign(positions);
        piecePositionsChanged();
        invalidate();
    }

    public void setPiecePositionsChangedListener(PiecePositionsChangedListener piecePositionsChangedListener) {
        this.piecePositionsChangedListener = piecePositionsChangedListener;
    }

    @Override
    final public boolean onTouchEvent(MotionEvent event) {
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

    @Override
    final protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cancelDrag();
        updateDrawDimensions();
        setZoomCenter(zoomCx, zoomCy);
    }

    @Override
    final protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (drawDimensions == null) {
            LogUtil.e("BaseGridView: cannot draw until onSizeChanged() is called!");
            return;
        }

        int draggedPieceIndex = -1;
        float dragDeltaX = 0.0f;
        float dragDeltaY = 0.0f;
        if (dragState != null && dragState.pieceIndex != -1) {
            draggedPieceIndex = dragState.pieceIndex;
            dragDeltaX = dragState.deltaX;
            dragDeltaY = dragState.deltaY;
        }
        gridDrawer.draw(canvas, drawDimensions, readonlyPiecePositions,
                draggedPieceIndex, dragDeltaX, dragDeltaY);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        LogUtil.v("%s saving instance state", getClass().getSimpleName());
        return new SavedState(this, super.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        LogUtil.v("%s restoring instance state", getClass().getSimpleName());
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        savedState.restore(this);
    }

    private void piecePositionsChanged() {
        if (piecePositionsChangedListener != null) {
            piecePositionsChangedListener.piecePositionsChanged(this);
        }

        Rect newGridBounds = piecePositions.getBoundingRect();
        if (gridBounds.equals(newGridBounds)) {
            return;
        }

        // Grid bounding box has changed!
        this.gridBounds = newGridBounds;
        updateCanvasBounds();
        updateDrawDimensions();
    }

    private void updateCanvasBounds() {
        Rect paddedGridBounds = new Rect(gridBounds);
        paddedGridBounds.left -= GRID_PADDING;
        paddedGridBounds.top -= GRID_PADDING;
        paddedGridBounds.right += GRID_PADDING;
        paddedGridBounds.bottom += GRID_PADDING;
        this.canvasBounds = gridDrawer.calculateCanvasBounds(paddedGridBounds);
    }

    /** Returns the zero-based index of the piece at the given pixel coordinates, or -1 if none.*/
    private int findPieceIndex(float pixelX, float pixelY) {
        if (drawDimensions == null) {
            LogUtil.e("Cannot find piece by position before draw dimensions have been calculated!");
            return -1;
        }
        return piecePositions.indexOf(gridDrawer.calculateGridPos(drawDimensions, pixelX, pixelY));
    }

    private void movePieceBy(int pieceIndex, float deltaX, float deltaY) {
        Pos source = piecePositions.get(pieceIndex);
        PointF origin = gridDrawer.calculateFieldCenter(drawDimensions, source.x, source.y);
        Pos destination = gridDrawer.calculateGridPos(drawDimensions, origin.x + deltaX, origin.y + deltaY);
        piecePositions.moveOrSwap(pieceIndex, destination);
        piecePositionsChanged();
    }

    private void updateDrawDimensions() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth == 0 && viewHeight == 0) {
            LogUtil.i("Cannot update draw dimensions; viewport has not been calculated yet.");
            return;
        }
        int contentWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        int contentHeight = viewHeight - getPaddingTop() - getPaddingBottom();
        if (contentWidth <= 0 || contentHeight <= 0) {
            LogUtil.e("Invalid content area size: %dx%d", contentWidth, contentHeight);
            return;
        }
        float scaleToFit = Math.min(
            contentWidth / canvasBounds.width(),
            contentHeight / canvasBounds.height());
        float scale = scaleToFit * zoomFactor;
        float renderedPixelWidth = scale * canvasBounds.width();
        float renderedPixelHeight = scale * canvasBounds.height();
        float drawOffsetX = getPaddingLeft() + 0.5f * contentWidth
                - renderedPixelWidth * zoomCx - scale * canvasBounds.left;
        float drawOffsetY = getPaddingTop() + 0.5f * contentHeight
                - renderedPixelHeight * zoomCy - scale * canvasBounds.top;
        drawDimensions = new DrawDimensions(scale, drawOffsetX, drawOffsetY);
        float minZoomCx = Math.min(0.5f * contentWidth / renderedPixelWidth, 0.5f);
        float maxZoomCx = 1.0f - minZoomCx;
        float minZoomCy = Math.min(0.5f * contentHeight / renderedPixelHeight, 0.5f);
        float maxZoomCy = 1.0f - minZoomCy;
        zoomCenterBounds = new RectF(minZoomCx, minZoomCy, maxZoomCx, maxZoomCy);
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
                zoomCx - deltaX / (drawDimensions.scale * canvasBounds.width()),
                zoomCy - deltaY / (drawDimensions.scale * canvasBounds.height()));
    }

    private void setZoomCenter(float cx, float cy) {
        zoomCx = clamp(cx, zoomCenterBounds.left, zoomCenterBounds.right);
        zoomCy = clamp(cy, zoomCenterBounds.top, zoomCenterBounds.bottom);
        updateDrawDimensions();
        invalidate();
    }

    private boolean detectDrag(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragState = new DragState(event, findPieceIndex(event.getX(), event.getY()));
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

    private class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
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
    }

    private static float clamp(float x, float min, float max) {
        return x < min ? min : x > max ? max : x;
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

    private static class SavedState extends BaseSavedState {
        final List<Pos> piecePositions;
        final float zoomCx;
        final float zoomCy;
        final float zoomFactor;

        public SavedState(BaseGridView view, Parcelable superState) {
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

        void restore(BaseGridView view) {
            view.setPiecePositions(piecePositions);
            view.zoomFactor = zoomFactor;
            view.zoomCx = zoomCx;
            view.zoomCy = zoomCy;
        }
    }
}
