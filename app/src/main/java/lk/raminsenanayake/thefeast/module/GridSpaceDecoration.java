package lk.raminsenanayake.thefeast.module;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class GridSpaceDecoration extends RecyclerView.ItemDecoration {
    private int space;

    public GridSpaceDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
                               RecyclerView parent, RecyclerView.State state) {
        outRect.left = space/2;
        outRect.right = space/2;
        outRect.bottom = space;

        // Add top margin only for the first item to avoid double space between items
//        if (parent.getChildLayoutPosition(view) == 0) {
//            outRect.top = space;
//        } else {
//            outRect.top = 0;
//        }
    }
}
