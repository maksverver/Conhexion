package ch.verver.conhexion;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * A 2D position, consisting of a pair of immutable integers.
 *
 * <p>Similar to {@link android.graphics.Point}, except the fields of that class are mutable.
 */
class Pos implements Comparable<Pos>, Parcelable {
    public final int x;
    public final int y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Pos pos) {
        return equals(pos.x, pos.y);
    }

    public boolean equals(int x, int y) {
        return this.x == x && this.y == y;
    }

    // Implementation of Object

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof Pos && equals((Pos) obj);
    }

    @Override
    public int hashCode() {
        return 31337 * x + y;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format(Locale.US, "Pos(%d, %d)", x, y);
    }

    // Implementation of Comparable<Pos>

    @Override
    public int compareTo(Pos o) {
        if (x != o.x) {
            return x < o.x ? -1 : +1;
        }
        if (y != o.y) {
            return y < o.y ? -1 : +1;
        }
        return 0;
    }

    // Implementation of Parcelable

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(x);
        out.writeInt(y);
    }

    public static final Parcelable.Creator<Pos> CREATOR = new Parcelable.Creator<Pos>() {
        public Pos createFromParcel(Parcel in) {
            int x = in.readInt();
            int y = in.readInt();
            return new Pos(x, y);
        }

        public Pos[] newArray(int size) {
            return new Pos[size];
        }
    };

    // End of Parcelable implementation.
}
