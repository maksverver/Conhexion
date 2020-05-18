package ch.verver.chilab;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

public class RectGridView extends BaseGridView {
    public RectGridView(Context context) {
        super(context, new RectGridDrawer(context.getResources(), context.getTheme()));
    }

    public RectGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, new RectGridDrawer(context.getResources(), context.getTheme()), attrs);
    }

    public RectGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, new RectGridDrawer(context.getResources(), context.getTheme()), attrs, defStyleAttr);
    }

    public RectGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, new RectGridDrawer(context.getResources(), context.getTheme()), attrs, defStyleAttr, defStyleRes);
    }

    public void setPiecePositionsChangedListener(RectPiecePositionsChangedListener rectPiecePositionsChangedListener) {
        super.setPiecePositionsChangedListener(new ListenerAdapter(rectPiecePositionsChangedListener));
    }

    private static class ListenerAdapter implements BaseGridView.PiecePositionsChangedListener {
        private final RectPiecePositionsChangedListener delegate;

        ListenerAdapter(RectPiecePositionsChangedListener delegate) {
            this.delegate = delegate;
        }

        @Override
        public void piecePositionsChanged(BaseGridView baseGridView) {
            delegate.rectPiecePositionsChanged((RectGridView) baseGridView);
        }
    }
}
