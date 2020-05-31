package ch.verver.chilab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;

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

    // You can long-press on a piece to select the whole connected group for dragging (instead of
    // moving just a single piece). However, we ignore the long-press if the first piece has been
    // moved by more than `scale * LONG_PRESS_MAX_MOVEMENT` pixels, i.e., 35% of a grid canvas unit.
    // This allows users to move a single piece out of a group by quickly dragging it away.
    private static final float LONG_PRESS_MAX_MOVEMENT = 0.35f;

    private final Handler handler = new Handler();
    private final GridDrawer gridDrawer;
    private final ScaleGestureDetector scaleGestureDetector;

    private MutableLiveData<ImmutableList<Pos>> piecePositionsLiveData = null;
    private Observer<ImmutableList<Pos>> piecePositionsLiveDataObserver = new Observer<ImmutableList<Pos>>() {
        @Override
        public void onChanged(ImmutableList<Pos> positions) {
            cancelDrag();
            piecePositions.assign(positions);
            updateOverlapErrors(0);
            Rect newGridBounds = piecePositions.getBoundingRect();
            if (!gridBounds.equals(newGridBounds)) {
                // Grid bounding box has changed!
                gridBounds = newGridBounds;
                updateCanvasBounds();
                updateDrawDimensions();
            }
            invalidate();
        }
    };

    // Current piece positions
    private final PiecePositionIndex piecePositions = new PiecePositionIndex();
    private final ReadonlyPiecePositionIndex readonlyPiecePositions = piecePositions.readonlyWrapper();

    // List of overlap errors. Recalculated whenever piece positions OR dragged pieces change.
    private ImmutableList<Pair<Pos, Direction>> overlapErrors = ImmutableList.empty();

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
    private @Nullable DragState dragState = null;

    // Determines whether the view allows panning, zooming, and moving pieces.
    private boolean editable = true;

    // When non-null, a victory animation is in progress. See startVictoryAnimation()
    private @Nullable VictoryAnimator victoryAnimator = null;

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

    public void setPiecePositionsLiveData(LifecycleOwner lifecycleOwner, MutableLiveData<ImmutableList<Pos>> newData) {
        if (piecePositionsLiveData != null) {
            piecePositionsLiveData.removeObserver(piecePositionsLiveDataObserver);
        }
        piecePositionsLiveData = newData;
        newData.observe(lifecycleOwner, piecePositionsLiveDataObserver);
    }

    public void setEditable(boolean newEditable) {
        if (editable == newEditable) {
            return;
        }
        editable = newEditable;
        cancelDrag();
        invalidate();
    }

    public void startVictoryAnimation() {
        if (victoryAnimator != null) {
            LogUtil.w("BaseGridView: cannot start victory animation while animation is in progress");
            return;
        }
        victoryAnimator = new VictoryAnimator();
    }

    @Override
    final public boolean onTouchEvent(MotionEvent event) {
        boolean handled = super.onTouchEvent(event);
        if (editable) {
            // Handle pinch-to-zoom gesture.
            handled |= scaleGestureDetector.onTouchEvent(event);
            if (scaleGestureDetector.isInProgress()) {
                cancelDrag();
            } else {
                handled |= detectDrag(event);
            }
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

        long draggedPieces = 0;
        float dragDeltaX = 0.0f;
        float dragDeltaY = 0.0f;
        if (dragState != null && dragState.pieces != 0) {
            draggedPieces = dragState.pieces;
            dragDeltaX = dragState.deltaX;
            dragDeltaY = dragState.deltaY;
        }
        if (victoryAnimator == null) {
            gridDrawer.draw(canvas, drawDimensions, readonlyPiecePositions, overlapErrors,
                    draggedPieces, dragDeltaX, dragDeltaY);
        } else {
            // Zoom out during victory animation.
            // Maximum duration: log(10) / log(0.75) =~ 8 seconds to zoom out from 10 to 1.
            if (zoomFactor > MIN_ZOOM_FACTOR && victoryAnimator.frameDelay > 0) {
                // No need to use Math.min() since setZoomFactor() will clamp the zoom factor.
                setZoomFactor((float) (zoomFactor * Math.pow(0.75f, victoryAnimator.frameDelay)));
            }
            gridDrawer.animateVictory(canvas, drawDimensions, readonlyPiecePositions, victoryAnimator.frameTime);
        }
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

    private void draggedPiecesChanged(long draggedPieces) {
        updateOverlapErrors(draggedPieces);
    }

    private void updateOverlapErrors(long draggedPieces) {
        overlapErrors = ImmutableList.copyOf(
                calculateOverlapErrors(
                        gridDrawer.getErrorDirections(), piecePositions, draggedPieces));
    }

    private static ArrayList<Pair<Pos, Direction>> calculateOverlapErrors(
            Direction[] errorDirections, PiecePositionIndex piecePositions, long draggedPieces) {
        ArrayList<Pair<Pos, Direction>> overlapErrors = new ArrayList<>();
        for (int i = 0, n = piecePositions.size(); i < n; ++i) {
            if (!Util.isDragged(draggedPieces, i)) {
                Pos pos = piecePositions.get(i);
                for (Direction direction : errorDirections) {
                    int j = piecePositions.indexOf(direction.step(pos));
                    if (j != -1 && !Util.isDragged(draggedPieces, j) &&
                            (!direction.hasPath(i) || !direction.opposite().hasPath(j))) {
                        overlapErrors.add(Pair.create(pos, direction));
                    }
                }
            }
        }
        return overlapErrors;
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

    private void movePiecesBy(long pieces, @Nullable GroupFinder.Step[] steps, float deltaX, float deltaY) {
        if (pieces == 0) {
            return;
        }
        // Precondition: steps != null iff. `pieces` has more than one bit set.
        if (Util.isMultiDrag(pieces) != (steps != null)) {
            throw new AssertionError();
        }
        int firstPieceIndex = steps == null ? Util.getDraggedIndex(pieces) : steps[0].pieceIndex;
        Pos source = piecePositions.get(firstPieceIndex);
        PointF origin = gridDrawer.calculateFieldCenter(drawDimensions, source.x, source.y);
        Pos destination = gridDrawer.calculateGridPos(drawDimensions, origin.x + deltaX, origin.y + deltaY);
        if (source.equals(destination)) {
            return;
        }
        PiecePositionIndex newPiecePositions = new PiecePositionIndex(piecePositions);
        if (steps == null) {
            // Move a single piece.
            newPiecePositions.moveOrSwap(firstPieceIndex, destination);
        } else {
            // Move multiple pieces. Note that the set of source and destination positions may
            // partially overlap, but that's okay since we use the source piece index (not its
            // position) to identify the piece to be moved, and since all destinations are distinct,
            // each piece will end up in the right place.
            //
            // This code also behaves well if some destination positions are already occupied by
            // pieces, as long as the sets of source and destination positions are disjoint. In that
            // case, the existing pieces will be swapped to the source while maintaining their
            // original configuration. For example:
            //
            //      .......     .......
            //      .aa.b..     .b..aa.   Moving the a's three spaces to the right moves b and c to
            //      .aa..c.  => ..c.aa.   the left, keeping their relative positions intact.
            //      .......     .......
            //
            // The only case that isn't handled very nicely is when some of the destination fields
            // are occupied, and there is overlap between source and destination fields. In that
            // case, the occupied destination fields are moved to the vacated source fields in a way
            // that may break up an existing configuration. For example:
            //
            //      .......     .......
            //      ..aa.b.     ...baa.   Moving the a's two spaces to the right moves the b's to
            //      .aaa.b.  => .b.aaa.   the left, but by different amounts, breaking the group up.
            //      .......     .......
            //
            // I think this is acceptable; I don't think there is a nicer way to handle this without
            // involving other fields than the source and destination fields, and I don't think
            // users expect would expect anything better to happen in this case.
            int[] pieceIndices = GroupFinder.getPieces(steps);
            Pos[] pieceDestinations  = GroupFinder.reconstructPositions(steps, destination);
            for (int i = 0; i < steps.length; ++i) {
                newPiecePositions.moveOrSwap(pieceIndices[i], pieceDestinations[i]);
            }
        }
        // We haven't applied any changes yet, but setting the new value in the MutableLiveData will
        // cause the new value to be passed to piecePositionsLiveDataObserver#onChanged().
        piecePositionsLiveData.setValue(newPiecePositions.toImmutableList());
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
                LogUtil.v("Drag started");
                dragState = new DragState(event, findPieceIndex(event.getX(), event.getY()));
                if (dragState.pieces != 0) {
                    draggedPiecesChanged(dragState.pieces);
                }
                startLongPressDetection(dragState);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragState == null || !dragState.update(event)) {
                    return false;
                }
                if (dragState.pieces == 0) {
                    dragViewBy(dragState.deltaDeltaX, dragState.deltaDeltaY);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                if (dragState == null || !dragState.update(event)) {
                    return false;
                }
                LogUtil.v("Drag finished");
                DragState oldDragState = endDrag();
                movePiecesBy(oldDragState.pieces, oldDragState.pieceSteps, oldDragState.deltaX, oldDragState.deltaY);
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                return cancelDrag();
        }
        return false;
    }

    private boolean cancelDrag() {
        if (dragState != null) {
            LogUtil.v("Drag cancelled");
            endDrag();
            invalidate();
            return true;
        }
        return false;
    }

    // Assumes dragState != null. Don't call this method directly! Use cancelDrag() instead.
    private DragState endDrag() {
        DragState oldDragState = dragState;
        dragState = null;
        if (oldDragState.pieces != 0) {
            draggedPiecesChanged(0);
        }
        return oldDragState;
    }

    private void startLongPressDetection(final DragState originalDragState) {
        if (originalDragState.pieces != 0) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (dragState == originalDragState && dragState.pieces != 0 &&
                            sqr(dragState.deltaX) + sqr(dragState.deltaY) <
                                sqr(LONG_PRESS_MAX_MOVEMENT * drawDimensions.scale)) {
                        GroupFinder.Step[] pieceSteps = GroupFinder.calculateSteps(
                                gridDrawer.getConnectionDirections(),
                                readonlyPiecePositions,
                                Util.getDraggedIndex(dragState.pieces));
                        if (pieceSteps.length > 1) {
                            dragState.pieces = GroupFinder.getPieceMask(pieceSteps);
                            dragState.pieceSteps = pieceSteps;
                            draggedPiecesChanged(dragState.pieces);
                            invalidate();
                        }
                    }
                }
            }, ViewConfiguration.getLongPressTimeout());
        }
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

    private static float sqr(float x) {
        return x * x;
    }

    private static class DragState {
        final int pointerId;
        final float startX, startY;
        float lastX, lastY;
        float deltaX = 0.0f, deltaY = 0.0f;
        float deltaDeltaX = 0.0f, deltaDeltaY = 0.0f;
        // Bitmask of pieces being dragged. pieceSteps != null iff. more than 1 bit is set in pieces.
        long pieces;
        // Steps used to select multiple pieces. Used to reconstruct their positions when dropped.
        @Nullable GroupFinder.Step[] pieceSteps;

        private DragState(MotionEvent e, int firstPieceIndex) {
            this.startX = this.lastX = e.getX();
            this.startY = this.lastY = e.getY();
            this.pointerId = e.getPointerId(0);
            this.pieces = firstPieceIndex >= 0 ? (long) 1 << firstPieceIndex : 0;
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
        final float zoomCx;
        final float zoomCy;
        final float zoomFactor;

        public SavedState(BaseGridView view, Parcelable superState) {
            super(superState);
            zoomCx = view.zoomCx;
            zoomCy = view.zoomCy;
            zoomFactor = view.zoomFactor;
        }

        public SavedState(Parcel in) {
            super(in);
            zoomCx = in.readFloat();
            zoomCy = in.readFloat();
            zoomFactor = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(zoomCx);
            out.writeFloat(zoomCy);
            out.writeFloat(zoomFactor);
        }

        void restore(BaseGridView view) {
            view.zoomFactor = zoomFactor;
            view.zoomCx = zoomCx;
            view.zoomCy = zoomCy;
        }
    }

    private class VictoryAnimator implements Runnable {
        private static final int TARGET_FPS = 30;
        private static final float DURATION = 12.0f;  // seconds

        final long startTimeMillis;
        final boolean wasEditable;

        float frameTime = 0.0f;
        float frameDelay = 0.0f;

        public VictoryAnimator() {
            startTimeMillis = SystemClock.elapsedRealtime();
            wasEditable = editable;
            setEditable(false);
            handler.post(this);
        }

        @Override
        public void run() {
            float newFrameTime = (SystemClock.elapsedRealtime() - startTimeMillis) * 1e-3f;
            frameDelay = newFrameTime - frameTime;
            frameTime = newFrameTime;

            if (frameTime < DURATION) {
                handler.postDelayed(this, 1000 / TARGET_FPS);
            } else {
                // End animation
                victoryAnimator = null;
                setEditable(wasEditable);
            }
            invalidate();
        }
    }
}
