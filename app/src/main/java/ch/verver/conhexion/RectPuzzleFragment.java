package ch.verver.conhexion;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class RectPuzzleFragment extends Fragment {

    // Required constructor -- called by the framework.
    public RectPuzzleFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_rect_puzzle, container, false);
        RectGridView rectGridView = rootView.findViewById(R.id.rect_grid_view);
        rectGridView.setPiecePositionsLiveData(this,
                ((App) getActivity().getApplication()).getAppState().getRectPuzzlePiecePositions());
        return rootView;
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
