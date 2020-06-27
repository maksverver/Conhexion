package ch.verver.conhexion;

import android.util.Log;

import java.util.Locale;

class LogUtil {
    public static Locale LOCALE = Locale.US;
    public static final String TAG = "Conhexion";

    public static void d(String format, Object... args) {
        Log.d(TAG, String.format(LOCALE, format, args));
    }

    public static void v(String format, Object... args) {
        Log.v(TAG, String.format(LOCALE, format, args));
    }

    public static void i(String format, Object... args) {
        Log.i(TAG, String.format(LOCALE, format, args));
    }

    public static void w(String format, Object... args) {
        Log.w(TAG, String.format(LOCALE, format, args));
    }

    public static void w(Exception e, String format, Object... args) {
        Log.w(TAG, String.format(LOCALE, format, args), e);
    }

    public static void e(String format, Object... args) {
        Log.e(TAG, String.format(LOCALE, format, args));
    }

    public static void e(Exception e, String format, Object... args) {
        Log.e(TAG, String.format(LOCALE, format, args), e);
    }
}
