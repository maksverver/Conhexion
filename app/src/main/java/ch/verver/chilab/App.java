package ch.verver.chilab;

import android.app.Application;

public class App extends Application {
    private static AppState appState;

    static AppState getAppState() {
        if (appState == null) {
            throw new IllegalStateException("Application has not been created yet");
        }
        return appState;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        appState = new AppState();
        appState.restoreFromSharedPreferences(this);
        appState.fillInMissingFields();

        LogUtil.i("Application created");
    }
}
