package ch.verver.chilab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
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

    interface PiecePositionsChangedListener {
        void piecePositionsChanged(RectGridView view);
    }

    private Context context;
    private Drawable[] pieceDrawables;
    private Drawable overlapHorizDrawable;
    private Drawable overlapVertiDrawable;

    private PiecePositionIndex piecePositionIndex = new PiecePositionIndex(1, 1);

    private Matrix transformMatrix = new Matrix();
    private Matrix inverseTransformMatrix = new Matrix();

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

        sizeChanged();
    }

    // Called whenever either the View size or the grid size changed.
    private void sizeChanged() {
        cancelDrag();
        recalculateTransformMatrix();
    }

    private void recalculateTransformMatrix() {
        transformMatrix.reset();
        inverseTransformMatrix.reset();

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            // Size is invalid / has not been set yet.
            return;
        }

        // + 1 to add a half-cell padding around the edges
        // TODO: use padding from getPaddingLeft()/getPaddingTop()/getPaddingRight()/getPaddingBottom() instead!
        int gridWidth = piecePositionIndex.getGridWidth();
        int gridHeight = piecePositionIndex.getGridHeight();
        int usedWidth = Math.min(width, height * (gridWidth + 1) / (gridHeight + 1));
        int usedHeight = Math.min(height, width * (gridHeight + 1) / (gridWidth + 1));

        transformMatrix.postTranslate(0.5f, 0.5f);  // padding
        transformMatrix.postScale((float) usedWidth / (gridWidth + 1), (float) usedHeight / (gridHeight + 1));
        transformMatrix.postTranslate(0.5f*(width - usedWidth), 0.5f*(height - usedHeight));

        if (!transformMatrix.invert(inverseTransformMatrix)) {
            LogUtil.e("Transform matrix could not be inverted!");
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        sizeChanged();
    }

    public void setGridSize(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("grid width and height must be positive integers");
        }
        piecePositionIndex = new PiecePositionIndex(width, height, piecePositionIndex);
        sizeChanged();
        invalidate();
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.setMatrix(transformMatrix);

        // Draw grid lines
        Paint gridStrokePaint = new Paint();
        gridStrokePaint.setColor(getColor(R.color.rectGridGridLines));
        gridStrokePaint.setStyle(Paint.Style.STROKE);
        gridStrokePaint.setStrokeWidth(0.05f);
        gridStrokePaint.setStrokeCap(Paint.Cap.ROUND);
        int gridWidth = piecePositionIndex.getGridWidth();
        int gridHeight = piecePositionIndex.getGridHeight();
        for (int y = 0; y <= gridHeight; ++y) {
            canvas.drawLine(0, y, gridWidth, y, gridStrokePaint);
        }
        for (int x = 0; x <= gridWidth; ++x) {
            canvas.drawLine(x, 0, x, gridHeight, gridStrokePaint);
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
                    float[] bounds = {pos.x - 0.25f, pos.y, pos.x + 0.25f, pos.y + 1.0f};
                    transformMatrix.mapPoints(bounds);
                    overlapVertiDrawable.setBounds((int) bounds[0], (int) bounds[1], (int) bounds[2], (int) bounds[3]);
                    overlapVertiDrawable.draw(canvas);
                }

                j = piecePositionIndex.indexOf(pos.x, pos.y - 1);
                if (j != -1 && j != draggedPieceIndex && (
                        !RectDirection.UP.hasPath(i) ||
                        !RectDirection.DOWN.hasPath(j))) {
                    float[] bounds = {pos.x, pos.y - 0.25f, pos.x + 1.0f, pos.y + 0.25f};
                    transformMatrix.mapPoints(bounds);
                    overlapHorizDrawable.setBounds((int) bounds[0], (int) bounds[1], (int) bounds[2], (int) bounds[3]);
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
                float[] bounds = {x - 0.5f, y - 0.5f, x + 1.5f, y + 1.5f};
                transformMatrix.mapPoints(bounds);
                pieceDrawable.setBounds((int) bounds[0], (int) bounds[1], (int) bounds[2], (int) bounds[3]);
                pieceDrawable.draw(canvas);
                return;
            }
        }
        // Fallback: draw piece as a colored square
        float[] bounds = {x, y, x + 1, y + 1};
        transformMatrix.mapPoints(bounds);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(255 * pieceIndex / 14, 0, 255 - 255 * pieceIndex / 14));
        canvas.drawRect(bounds[0], bounds[1], bounds[2], bounds[3], paint);
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
            float[] xy = new float[]{e.getX(), e.getY()};
            inverseTransformMatrix.mapPoints(xy);
            float startX = xy[0];
            float startY = xy[1];
            int pieceIndex = -1;
            int gridX = (int) Math.floor(startX);
            int gridY = (int) Math.floor(startY);
            for (int i = 0; i < piecePositionIndex.size(); ++i) {
                if (piecePositionIndex.get(i).equals(gridX, gridY)) {
                    pieceIndex = i;
                    break;
                }
            }
            this.startX = startX;
            this.startY = startY;
            this.pointerId = e.getPointerId(0);
            this.pieceIndex = pieceIndex;
        }

        private boolean update(MotionEvent e) {
            int pointerIndex = e.findPointerIndex(pointerId);
            if (pointerIndex < 0) {
                return false;
            }
            float[] xy = new float[]{e.getX(pointerIndex), e.getY(pointerIndex)};
            inverseTransformMatrix.mapPoints(xy);
            deltaX = xy[0] - startX;
            deltaY = xy[1] - startY;
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
        if (piecePositionIndex.inRange(newPos)) {
            piecePositionIndex.moveOrSwap(i, newPos);
            piecePositionsChanged();
        }
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
    }

    private static class SavedState extends BaseSavedState {
        final int gridWidth;
        final int gridHeight;
        final List<Pos>  piecePositions;

        public SavedState(RectGridView view, Parcelable superState) {
            super(superState);
            gridWidth = view.piecePositionIndex.getGridWidth();
            gridHeight = view.piecePositionIndex.getGridHeight();
            piecePositions = view.piecePositionIndex.toList();
        }

        public SavedState(Parcel in) {
            super(in);
            gridWidth = in.readInt();
            gridHeight = in.readInt();
            piecePositions = new ArrayList<>();
            in.readList(piecePositions, null);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(gridWidth);
            out.writeInt(gridHeight);
            out.writeList(piecePositions);
        }

        void restore(RectGridView view) {
            // Note: these methods must be called in this order!
            view.setGridSize(gridWidth, gridHeight);
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
