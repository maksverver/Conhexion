package ch.verver.chilab;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class RectPuzzleFragment extends Fragment {

    private @Nullable List<Pos> initialPiecePositions;

    // Required constructor -- called by the framework.
    public RectPuzzleFragment() {}

    public RectPuzzleFragment(List<Pos> initialPiecePositions) {
        this.initialPiecePositions = initialPiecePositions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rect_puzzle, container, false);
        RectGridView rectGridView = view.findViewById(R.id.rect_grid_view);
        if (initialPiecePositions != null) {
            rectGridView.setPiecePositions(initialPiecePositions);
            initialPiecePositions = null;
        }
        FragmentActivity activity = getActivity();
        if (activity instanceof RectPiecePositionsChangedListener) {
            rectGridView.setPiecePositionsChangedListener((RectPiecePositionsChangedListener) activity);
        }
        return view;
    }

    @Nullable
    public RectGridView getRectGridView() {
        View view = getView();
        if (view == null) {
            return null;
        }
        return view.findViewById(R.id.rect_grid_view);
    }
}
