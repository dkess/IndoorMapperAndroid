package me.dkess.indoormapper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Daniel on 12/30/2016.
 */

public class AccelerometerDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "accel_z";
    private static final String TABLE_CREATE =
            "create table if not exists "
                    + TABLE_NAME
                    + " (timestamp integer, z real)";

    AccelerometerDBHelper(Context context) {
        super(context, "indoormapper", null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 2 && newVersion == 3) {
            db.execSQL(TABLE_CREATE);
        }

    }
}
