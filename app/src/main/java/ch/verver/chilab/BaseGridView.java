package ch.verver.chilab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
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

    // Current client zoom state.
    // (zoomCx, zoomCy) is the point that should be rendered at the center of the view.
    private float zoomCx = 0.5f;  // between 0 and 1
    private float zoomCy = 0.5f;  // between 0 and 1
    private float zoomFactor = MIN_ZOOM_FACTOR;

    // Current viewport. Recalculated in onSizeChanged().
    private ViewPort viewPort = null;

    // Current draw dimensions. Depends on view dimensions (width, height, padding) and zoom state,
    // and should be recalculated on change.
    private DrawDimensions drawDimensions = null;

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
    }

    public BaseGridView(Context context, GridDrawer gridDrawer, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
    }

    public BaseGridView(Context context, GridDrawer gridDrawer, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
    }

    public BaseGridView(Context context, GridDrawer gridDrawer, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.gridDrawer = gridDrawer;
        this.scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
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
        viewPort = new ViewPort(this);
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
        updateDrawDimensions();
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
        if (viewPort == null) {
            LogUtil.i("Cannot update draw dimensions; viewport has not been calculated yet.");
            return;
        }
        if (viewPort.contentWidth <= 0 || viewPort.contentHeight <= 0) {
            LogUtil.e("Invalid viewport size: %s", viewPort);
            return;
        }

        // Note: paddedGridBounds is recreated each time, because Rect is a mutable type, so we
        // cannot reuse the instance after passing it to calculateDrawDimensions() below.
        Rect paddedGridBounds = new Rect(gridBounds);
        paddedGridBounds.left -= GRID_PADDING;
        paddedGridBounds.top -= GRID_PADDING;
        paddedGridBounds.right += GRID_PADDING;
        paddedGridBounds.bottom += GRID_PADDING;
        drawDimensions = gridDrawer.calculateDrawDimensions(
                viewPort, paddedGridBounds, zoomFactor, zoomCx, zoomCy);
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
