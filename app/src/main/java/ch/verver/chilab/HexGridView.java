package ch.verver.chilab;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

public class HexGridView extends BaseGridView {
    public HexGridView(Context context) {
        super(context, new HexGridDrawer(context.getResources(), context.getTheme()));
    }

    public HexGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, new HexGridDrawer(context.getResources(), context.getTheme()), attrs);
    }

    public HexGridView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, new HexGridDrawer(context.getResources(), context.getTheme()), attrs, defStyle);
    }

    public HexGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, new HexGridDrawer(context.getResources(), context.getTheme()), attrs, defStyleAttr, defStyleRes);
    }
}
