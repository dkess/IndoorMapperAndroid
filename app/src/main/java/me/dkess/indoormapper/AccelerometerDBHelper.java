package me.dkess.indoormapper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Daniel on 12/30/2016.
 */

public class AccelerometerDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "accel_data";
    private static final String TABLE_CREATE =
            "create table if not exists "
                    + TABLE_NAME
                    + " (sensor integer, timestamp integer, x real, y real, z real)";

    AccelerometerDBHelper(Context context) {
        super(context, "indoormapper", null, 2);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
