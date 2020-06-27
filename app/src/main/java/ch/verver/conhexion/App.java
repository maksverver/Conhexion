package ch.verver.conhexion;

import android.app.Application;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

public class App extends Application {
    private ViewModelProvider viewModelProvider = new ViewModelProvider(
            new ViewModelStore(),
            new ViewModelProvider.AndroidViewModelFactory(this));

    AppState getAppState() {
        return viewModelProvider.get(AppState.class);
    }
}
