package ch.verver.conhexion;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

public class RectGridView extends BaseGridView<RectDirection> {
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
}
