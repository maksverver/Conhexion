package ch.verver.chilab;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HexPuzzleSolvedFragment extends Fragment {

    public HexPuzzleSolvedFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hex_puzzle_solved, container, false);
        Activity activity = getActivity();
        if (activity instanceof View.OnClickListener) {
            View.OnClickListener onClickListener = (View.OnClickListener) activity;
            view.findViewById(R.id.replay_rect_puzzle).setOnClickListener(onClickListener);
            view.findViewById(R.id.replay_hex_puzzle).setOnClickListener(onClickListener);
        }
        return view;
    }
}
