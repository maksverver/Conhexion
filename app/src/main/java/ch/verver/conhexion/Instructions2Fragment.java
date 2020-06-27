package ch.verver.conhexion;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Instructions2Fragment extends Fragment {

    public Instructions2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_instructions2, container, false);
        Activity activity = getActivity();
        if (activity instanceof View.OnClickListener) {
            View.OnClickListener onClickListener = (View.OnClickListener) activity;
            rootView.findViewById(R.id.instructions2_previous_button)
                    .setOnClickListener(onClickListener);
            rootView.findViewById(R.id.instructions2_next_button)
                    .setOnClickListener(onClickListener);
        }
        return rootView;
    }

}
