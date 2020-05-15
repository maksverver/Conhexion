package ch.verver.chilab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.List;

public class RectGridView extends View {
    public interface PiecePositionsChangedListener {
        void piecePositionsChanged(RectGridView view);
    }

    /**
     * Number of padding squares to add around the bounding box of the pieces. This should be
     * nonzero to allow the bounding box to be extended by dragging a piece into the padding.
     */
    private static final int GRID_PADDING = 1;

    private Context context;
    private Drawable[] pieceDrawables;
    private Drawable overlapHorizDrawable;
    private Drawable overlapVertiDrawable;

    private PiecePositionIndex piecePositionIndex = new PiecePositionIndex();

    // (drawOffsetX, drawOffsetY) is the pixel position of grid point (0, 0)
    // drawScale is the size in pixels of a grid square
    private int drawOffsetX;
    private int drawOffsetY;
    private float drawScale;

    @Nullable private DragState dragState = null;

    @Nullable private PiecePositionsChangedListener piecePositionsChangedListener = null;

    public RectGridView(Context context) {
        super(context);
        init(context);
    }

    public RectGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RectGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public RectGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private Drawable getDrawable(@DrawableRes int id) {
        return ResourcesCompat.getDrawable(getResources(), id, context.getTheme());
    }

    private int getColor(@ColorRes int id) {
        return ResourcesCompat.getColor(getResources(), id, context.getTheme());
    }

    private void init(Context context) {
        this.context = context;
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
            pieceDrawables[i] = getDrawable(pieceDrawableIds[i]);
        }
        overlapHorizDrawable = getDrawable(R.drawable.rect_overlap_horiz);
        overlapVertiDrawable = getDrawable(R.drawable.rect_overlap_verti);
    }

    private void updateDrawDimensions() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (viewWidth <= 0 || viewHeight <= 0) {
            // Size is invalid / has not been set yet.
            return;
        }

        Rect gridBounds = piecePositionIndex.getBoundingRect();
        gridBounds.left -= GRID_PADDING;
        gridBounds.top -= GRID_PADDING;
        gridBounds.right += GRID_PADDING;
        gridBounds.bottom += GRID_PADDING;
        int gridWidth = gridBounds.right - gridBounds.left;
        int gridHeight = gridBounds.bottom - gridBounds.top;
        int contentWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        int contentHeight = viewHeight - getPaddingTop() - getPaddingBottom();
        int usedWidth = Math.min(viewWidth, viewHeight * gridWidth / gridHeight);
        int usedHeight = Math.min(viewHeight, viewWidth * gridHeight / gridWidth);

        drawScale = Math.min(contentWidth / gridWidth, contentHeight / gridHeight);
        drawOffsetX = getPaddingLeft() + (viewWidth - usedWidth) / 2 - (int) (drawScale * gridBounds.left);
        drawOffsetY = getPaddingTop() + (viewHeight - usedHeight) / 2 - (int) (drawScale * gridBounds.top);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        cancelDrag();
        updateDrawDimensions();
    }

    public void setPiecePositions(List<Pos> positions) {
        if (positions.size() != RectPuzzle.PIECE_COUNT) {
            throw new IllegalArgumentException();
        }
        cancelDrag();
        piecePositionIndex.assign(positions);
        invalidate();
        piecePositionsChanged();
    }

    public ArrayList<Pos> getPiecePositions() {
        return piecePositionIndex.toList();
    }

    public PiecePositionIndex getPiecePositionIndex() {
        return new PiecePositionIndex(piecePositionIndex);
    }

    private int gridToPixelX(float x) {
        return drawOffsetX + (int) (drawScale * x);
    }

    private int gridToPixelY(float y) {
        return drawOffsetY + (int) (drawScale * y);
    }

    private float pixelToGridX(float x) {
        return (x - drawOffsetX) / drawScale;
    }

    private float pixelToGridY(float y) {
        return (y - drawOffsetY) / drawScale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int viewHeight = getHeight();
        int viewWidth = getWidth();

        // Draw grid lines
        {
            Paint gridStrokePaint = new Paint();
            gridStrokePaint.setColor(getColor(R.color.rectGridGridLines));
            gridStrokePaint.setStyle(Paint.Style.STROKE);
            gridStrokePaint.setStrokeWidth(0.05f * drawScale);
            gridStrokePaint.setStrokeCap(Paint.Cap.ROUND);
            int y = drawOffsetY;
            while (y > 0) {
                y -= drawScale;
            }
            while (y < viewHeight) {
                canvas.drawLine(0, y, viewWidth, y, gridStrokePaint);
                y += drawScale;
            }
            int x = drawOffsetX;
            while (x > 0) {
                x -= drawScale;
            }
            while (x < viewWidth) {
                canvas.drawLine(x, 0, x, viewHeight, gridStrokePaint);
                x += drawScale;
            }
        }

        int draggedPieceIndex = dragState == null ? -1 : dragState.pieceIndex;

        // Draw pieces
        canvas.setMatrix(new Matrix());
        for (int i = 0; i < piecePositionIndex.size(); ++i) {
            if (i != draggedPieceIndex) {
                Pos pos = piecePositionIndex.get(i);
                drawPiece(canvas, i, pos.x, pos.y);
            }
        }

        // Draw overlap errors
        for (int i = 0; i < piecePositionIndex.size(); ++i) {
            Pos pos = piecePositionIndex.get(i);
            if (i != draggedPieceIndex) {
                int j = piecePositionIndex.indexOf(pos.x - 1, pos.y);
                if (j != -1 && j != draggedPieceIndex && (
                        !RectDirection.LEFT.hasPath(i) ||
                        !RectDirection.RIGHT.hasPath(j))) {
                    overlapVertiDrawable.setBounds(
                            gridToPixelX(pos.x - 0.25f), gridToPixelY(pos.y),
                            gridToPixelX(pos.x + 0.25f), gridToPixelY(pos.y + 1.0f));
                    overlapVertiDrawable.draw(canvas);
                }

                j = piecePositionIndex.indexOf(pos.x, pos.y - 1);
                if (j != -1 && j != draggedPieceIndex && (
                        !RectDirection.UP.hasPath(i) ||
                        !RectDirection.DOWN.hasPath(j))) {
                    overlapHorizDrawable.setBounds(
                            gridToPixelX(pos.x), gridToPixelY(pos.y - 0.25f),
                            gridToPixelX(pos.x + 1.0f), gridToPixelY( pos.y + 0.25f));
                    overlapHorizDrawable.draw(canvas);
                }
            }
        }

        // Draw dragged piece last to ensure it is displayed on top!
        if (dragState != null) {
            int i = dragState.pieceIndex;
            Pos p = piecePositionIndex.get(i);
            float x = p.x, y = p.y;
            x += dragState.deltaX;
            y += dragState.deltaY;
            drawPiece(canvas, i, x, y);
        }
    }

    private void drawPiece(Canvas canvas, int pieceIndex, float x, float y) {
        if (pieceIndex >= 0 && pieceIndex < pieceDrawables.length) {
            Drawable pieceDrawable = pieceDrawables[pieceIndex];
            if (pieceDrawable != null) {
                pieceDrawable.setBounds(
                        gridToPixelX(x - 0.5f), gridToPixelY(y - 0.5f),
                        gridToPixelX(x + 1.5f), gridToPixelY(y + 1.5f));
                pieceDrawable.draw(canvas);
                return;
            }
        }
        // Fallback: draw piece as a colored square
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255 * pieceIndex / 14, 0, 255 - 255 * pieceIndex / 14));
        canvas.drawRect(
                gridToPixelX(x), gridToPixelY(y),
                gridToPixelX(x + 1), gridToPixelY(y + 1), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return startDrag(event);
            case MotionEvent.ACTION_MOVE:
                return continueDrag(event);
            case MotionEvent.ACTION_UP:
                return finishDrag(event);
            case MotionEvent.ACTION_CANCEL:
                return cancelDrag();
        }
        return super.onTouchEvent(event);
    }

    private class DragState {
        final int pointerId;
        final int pieceIndex;
        final float startX, startY;
        float deltaX = 0.0f, deltaY = 0.0f;

        private DragState(MotionEvent e) {
            this.startX = pixelToGridX(e.getX());
            this.startY = pixelToGridY(e.getY());
            this.pointerId = e.getPointerId(0);
            this.pieceIndex = piecePositionIndex.indexOf((int) Math.floor(startX), (int) Math.floor(startY));
        }

        private boolean update(MotionEvent e) {
            int pointerIndex = e.findPointerIndex(pointerId);
            if (pointerIndex < 0) {
                return false;
            }
            deltaX = pixelToGridX(e.getX(pointerIndex)) - startX;
            deltaY = pixelToGridY(e.getY(pointerIndex)) - startY;
            return true;
        }
    }

    private boolean cancelDrag() {
        if (dragState != null) {
            dragState = null;
            LogUtil.v("Drag cancelled");
            invalidate();
        }
        return false;
    }

    boolean startDrag(MotionEvent e) {
        DragState newDragState = new DragState(e);
        if (newDragState.pieceIndex == -1) {
            newDragState = null;
        } else {
            LogUtil.v("Drag started");
        }
        if (dragState != newDragState) {
            dragState = newDragState;
            invalidate();
            return true;
        } else {
            return false;
        }
    }

    boolean continueDrag(MotionEvent e)  {
        if (dragState == null || !dragState.update(e)) {
            return false;
        }
        invalidate();
        return true;
    }

    boolean finishDrag(MotionEvent e) {
        if (dragState == null || !dragState.update(e)) {
            return false;
        }
        LogUtil.v("Drag finished");
        int i = dragState.pieceIndex;
        Pos oldPos = piecePositionIndex.get(i);
        Pos newPos = new Pos(
                oldPos.x + Math.round(dragState.deltaX),
                oldPos.y + Math.round(dragState.deltaY));
        piecePositionIndex.moveOrSwap(i, newPos);
        piecePositionsChanged();
        dragState = null;
        invalidate();
        return true;
    }

    public void setPiecePositionsChangedListener(@Nullable PiecePositionsChangedListener piecePositionsChangedListener) {
        this.piecePositionsChangedListener = piecePositionsChangedListener;
    }

    private void piecePositionsChanged() {
        if (piecePositionsChangedListener != null) {
            piecePositionsChangedListener.piecePositionsChanged(this);
        }
        updateDrawDimensions();  // bounding rect may have changed
    }

    private static class SavedState extends BaseSavedState {
        final List<Pos>  piecePositions;

        public SavedState(RectGridView view, Parcelable superState) {
            super(superState);
            piecePositions = view.piecePositionIndex.toList();
        }

        public SavedState(Parcel in) {
            super(in);
            piecePositions = new ArrayList<>();
            in.readList(piecePositions, null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeList(piecePositions);
        }

        void restore(RectGridView view) {
            view.setPiecePositions(piecePositions);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        LogUtil.v("RectGridView saving instance state");
        return new SavedState(this, super.onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcelable) {
        LogUtil.v("RectGridView restoring instance state");
        SavedState savedState = (SavedState) parcelable;
        super.onRestoreInstanceState(savedState.getSuperState());
        savedState.restore(this);
    }
}
