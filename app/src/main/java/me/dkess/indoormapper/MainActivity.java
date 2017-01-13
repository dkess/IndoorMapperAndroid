package me.dkess.indoormapper;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mCompass;

    private boolean isRecording = false;

    private static final Direction[] dir_const = {
            Direction.left,
            Direction.forward,
            Direction.right};

    private static final int[] turnButtonIds = {R.id.b_left, R.id.b_forward, R.id.b_right};
    private static final int[] forceButtonIds = {R.id.b_forceleft, R.id.b_forceforward, R.id.b_forceright};

    private boolean isUndoLongPressed = false;
    IndoorMapper indoorMapper = null;

    AccelerometerDBHelper accelerometerDBHelper;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.textView)).setMovementMethod(new ScrollingMovementMethod());

        displayMsg("App started");

        Button undoButton = (Button) findViewById(R.id.b_undo);
        undoButton.setOnLongClickListener(undoLongClickListener);
        undoButton.setOnTouchListener(undoTouchListener);

        ((ToggleButton) findViewById(R.id.walking_toggle)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                recordingTogglePressed(isChecked);
            }
        });

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        accelerometerDBHelper = new AccelerometerDBHelper(getApplicationContext());
        db = accelerometerDBHelper.getWritableDatabase();
        statement = db.compileStatement(SQL_STMT);
    }

    protected void onResume() {
        super.onResume();

        if (isRecording) {
            startRecording();
        }
    }

    protected void onPause() {
        super.onPause();

        stopRecording();
    }

    protected void onStop() {
        super.onStop();

        stopRecording();
    }

    protected void onDestroy() {
        super.onDestroy();

        stopRecording();
    }

    private View.OnLongClickListener undoLongClickListener = new View.OnLongClickListener(){
        public boolean onLongClick(View v) {
            isUndoLongPressed = true;
            return false;
        }
    };


    private View.OnTouchListener undoTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View pView, MotionEvent pEvent) {
            if (pEvent.getAction() == MotionEvent.ACTION_DOWN) {
                isUndoLongPressed = false;
            }
            return false;
        }
    };

    private void resetButtons() {
        int[] ids = {R.id.b_left, R.id.b_forward, R.id.b_right,
                R.id.b_forceleft, R.id.b_forceforward, R.id.b_forceright};
        for (int i : ids) {
            ToggleButton tb = (ToggleButton) findViewById(i);
             tb.setChecked(false);
        }
    }

    private EnumSet<Direction> getSelectedDirs() {
        EnumSet<Direction> dirs = EnumSet.noneOf(Direction.class);
        for (int i = 0; i < 3; i++) {
            ToggleButton tb = (ToggleButton) findViewById(turnButtonIds[i]);
            if (tb.isChecked()) {
                dirs.add(dir_const[i]);
            }
        }
        return dirs;
    }

    private Direction getSelectedForceTurn() {
        for (int i = 0; i < 3; i++) {
            ToggleButton tb = (ToggleButton) findViewById(forceButtonIds[i]);
            if (tb.isChecked()) {
                return dir_const[i];
            }
        }
        return null;
    }

    private static DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private void displayMsg(String msg) {
        msg = dateFormat.format(new Date()) + " " + msg + "\n";
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(msg + textView.getText());
    }

    public void onGoPress(View view) {
        File file = new File(getExternalFilesDir(null), "nodes.json");
        IndoorMapper.IndoorMap map = IndoorMapper.IndoorMap.readFromFile(file);

        indoorMapper = new IndoorMapper(map);
        IndoorMapper.ForkResult res = indoorMapper.fork_part1(getSelectedDirs());
        if (res == null) {
            displayMsg("Unexpected directions");
        } else if (res.on != null) {
            interpret_part2(
                    indoorMapper.fork_part2(res.on_id, null, getSelectedForceTurn()));
        } else {
            Intent intent = new Intent(this, ChooseNode.class);
            intent.putExtra("me.dkess.indoormapper.DATA", res.choices);

            // we definitely wont be walking while typing, so stop recording movement on this screen
            ((ToggleButton) findViewById(R.id.walking_toggle)).setChecked(false);

            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // start recording again
        ((ToggleButton) findViewById(R.id.walking_toggle)).setChecked(true);

        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                Bundle res = data.getExtras();
                int choice = res.getInt("me.dkess.indoormapper.CHOICE_ID", -2);
                if (choice == -1) {
                    String newNode = res.getString("me.dkess.indoormapper.NEW_NODE");
                    interpret_part2(
                            indoorMapper.fork_part2(choice, newNode, getSelectedForceTurn()));
                } else {
                    interpret_part2(
                            indoorMapper.fork_part2(choice, null, getSelectedForceTurn()));
                }
            }
        }
    }

    private void interpret_part2(Direction nextDir) {
        IntString res = indoorMapper.currentNode();
        displayMsg("On node "+res.n+": "+res.s);
        displayMsg("You should turn "+nextDir);
        File file = new File(getExternalFilesDir(null), "nodes.json");
        boolean writeRes = indoorMapper.map.writeToFile(file);
        if (!writeRes) {
            displayMsg("File error! :(");
        }
        updateExpectedDir();
        resetButtons();

        // Automatically fill in directions, if we already know what node we're on
        Set<Direction> directionsWeSee = indoorMapper.whatWeSee();
        if (directionsWeSee != null) {
            for (Direction d : directionsWeSee) {
                // Convert the direction to an index for the turnButtonIds array
                int turnButtonIndex = (d.ordinal() + 1) % 4;
                ((ToggleButton) findViewById(turnButtonIds[turnButtonIndex % 4])).setChecked(true);
            }
        }

    }

    public void onUndoPress(View view) {
        if (isUndoLongPressed) {
            File file = new File(getExternalFilesDir(null), "nodes.json");
            IndoorMapper.IndoorMap map = IndoorMapper.IndoorMap.readFromFile(file);
            indoorMapper = new IndoorMapper(map);
            indoorMapper.undo();

            IntString res = indoorMapper.currentNode();

            displayMsg("Undo. Now at " + res.n + ": "+res.s);
            boolean writeRes = indoorMapper.map.writeToFile(file);
            if (!writeRes) {
                displayMsg("File error! :(");
            }
            updateExpectedDir();
            resetButtons();

        }
        isUndoLongPressed = false;
    }

    private void recordingTogglePressed(boolean isPressed) {
        isRecording = isPressed;

        if (isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopRecording() {
        mSensorManager.unregisterListener(this);
        flushAccelEntries();
    }

    private void updateExpectedDir() {
        Direction facing = getCompassDir();
        Direction expected = null;

        String msg = "Facing: " + facing;
        if (indoorMapper != null) {
            expected = indoorMapper.absolute_dir();
            msg += " Exp: " + expected;
        }
        TextView tv = ((TextView) findViewById(R.id.current_orientation));
        tv.setText(msg);

        if (facing != null && expected != null && facing != expected) {
            tv.setTextColor(Color.rgb(204,42,42));
        } else {
            tv.setTextColor(Color.rgb(22,107,19));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    float[] mGravity;
    float[] mGeomagnetic;
    float azimut;
    float north;

    public void calibrateForward(View view) {
        north = azimut;
    }

    float TWOPI = 2 * (float) Math.PI;

    /**
     * Returns the direction the user is facing, relative to whichever direction they were facing
     * when calibrateForward() was called.
     */
    private Direction getCompassDir() {
        int shifted = (int) ((azimut - north + TWOPI + (float) Math.PI / 4) / (Math.PI / 2)) % 4;
        return Direction.values()[shifted];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }

        if ((event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                && mGravity != null && mGeomagnetic != null) {
            float Ro[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(Ro, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(Ro, orientation);
                azimut = (orientation[0] + 3 * (float) Math.PI) % TWOPI;
                updateExpectedDir();
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // This measurement tends to be ~150ms off from the better measurement, used below
            //long timeInMillis = System.currentTimeMillis();

            // convert sensor timestamp to unix time
            // https://code.google.com/p/android/issues/detail?id=7981#c17
            long timeInMillis = System.currentTimeMillis() + ((event.timestamp-SystemClock.elapsedRealtimeNanos())/1000000L);

            addAccelEntry(timeInMillis, event.values[2]);
        }
    }

    // items for accumulating accelerometer data and flushing to db
    private static final int MAX_ACCEL_ENTRIES = 2048;
    private float[] accelEntries = new float[MAX_ACCEL_ENTRIES];
    private int numAccelEntries = 0;

    private final String SQL_STMT =
            "insert into " + AccelerometerDBHelper.TABLE_NAME + " values (?,?)";
    private SQLiteStatement statement;

    private void addAccelEntry(long timestamp, double e) {
        if (numAccelEntries >= MAX_ACCEL_ENTRIES) {
            flushAccelEntries();
        }
        if (numAccelEntries == 0) {
            db.beginTransaction();
        }
        statement.clearBindings();
        statement.bindLong(1, timestamp);
        statement.bindDouble(2, e);
        statement.execute();
        numAccelEntries++;
    }

    private void flushAccelEntries() {
        if (numAccelEntries > 0) {
            db.setTransactionSuccessful();
            db.endTransaction();
            numAccelEntries = 0;
        }
    }
}
