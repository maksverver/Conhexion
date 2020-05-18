package ch.verver.chilab;

import android.view.View;

class ViewPort {
    public final int width;
    public final int height;
    public final int paddingLeft;
    public final int paddingTop;
    public final int paddingRight;
    public final int paddingBottom;
    public final int contentWidth;
    public final int contentHeight;

    ViewPort(View view) {
        width = view.getWidth();
        height = view.getHeight();
        paddingLeft = view.getPaddingLeft();
        paddingTop = view.getPaddingTop();
        paddingRight = view.getPaddingRight();
        paddingBottom = view.getPaddingBottom();
        contentWidth = width - paddingLeft - paddingRight;
        contentHeight = height - paddingTop - paddingBottom;
    }
}
