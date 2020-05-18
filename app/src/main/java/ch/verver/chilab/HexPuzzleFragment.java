package ch.verver.chilab;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HexPuzzleFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hex_puzzle, container, false);
        HexGridView hexGridView = view.findViewById(R.id.hex_grid_view);
        hexGridView.setPiecePositions(App.getAppState().getHexPuzzlePiecePositions());
        FragmentActivity activity = getActivity();
        if (activity instanceof HexPiecePositionsChangedListener) {
            hexGridView.setPiecePositionsChangedListener((HexPiecePositionsChangedListener) activity);
        }
        return view;
    }
}
