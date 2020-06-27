package ch.verver.conhexion;

import android.app.Activity;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class Instructions1Fragment extends Fragment {

    public Instructions1Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_instructions1, container, false);
        Activity activity = getActivity();
        if (activity instanceof View.OnClickListener) {
            View.OnClickListener onClickListener = (View.OnClickListener) activity;
            rootView.findViewById(R.id.instructions1_next_button)
                    .setOnClickListener(onClickListener);
        }
        return rootView;
    }
}
