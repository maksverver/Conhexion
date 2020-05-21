package ch.verver.chilab;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class RectPuzzleFragment extends Fragment {

    private final List<Pos> initialPiecePositions;

    public RectPuzzleFragment(List<Pos> initialPiecePositions) {
        this.initialPiecePositions = initialPiecePositions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rect_puzzle, container, false);
        RectGridView rectGridView = view.findViewById(R.id.rect_grid_view);
        rectGridView.setPiecePositions(initialPiecePositions);
        FragmentActivity activity = getActivity();
        if (activity instanceof RectPiecePositionsChangedListener) {
            rectGridView.setPiecePositionsChangedListener((RectPiecePositionsChangedListener) activity);
        }
        return view;
    }
}
