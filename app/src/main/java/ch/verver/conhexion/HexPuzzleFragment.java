package ch.verver.conhexion;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HexPuzzleFragment extends Fragment {

    // Required constructor -- called by the framework.
    public HexPuzzleFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_hex_puzzle, container, false);
        HexGridView hexGridView = rootView.findViewById(R.id.hex_grid_view);
        AppState appState = ((App) getActivity().getApplication()).getAppState();
        hexGridView.setPiecePositionsLiveData(this, appState.getHexPuzzlePiecePositions());
        hexGridView.setErrorVisibilityLiveData(this, appState.getErrorVisibility());
        return rootView;
    }

    HexGridView getHexGridView() {
        View view = getView();
        if (view == null) {
            return null;
        }
        return view.findViewById(R.id.hex_grid_view);
    }
}
