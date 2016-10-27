package me.dkess.indoormapper;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {
    private static final IndoorMapper.Direction[] dir_const = {
            IndoorMapper.Direction.left,
            IndoorMapper.Direction.forward,
            IndoorMapper.Direction.right};

    private static final int[] turnButtonIds = {R.id.b_left, R.id.b_forward, R.id.b_right};
    private static final int[] forceButtonIds = {R.id.b_forceleft, R.id.b_forceforward, R.id.b_forceright};


    private boolean isUndoLongPressed = false;
    IndoorMapper indoorMapper = null;

    protected GoogleApiClient mGoogleApiClient;
    private ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.textView)).setMovementMethod(new ScrollingMovementMethod());

        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        displayMsg("App started");

        Button undoButton = (Button) findViewById(R.id.b_undo);
        undoButton.setOnLongClickListener(undoLongClickListener);
        undoButton.setOnTouchListener(undoTouchListener);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdate`s() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));

    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver that was registered during onResume().
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        TextView statusIndicator = (TextView) findViewById(R.id.indicator);
        if (status.isSuccess()) {
            statusIndicator.setText("it's working");
        } else {
            statusIndicator.setText("Error: " + status.getStatusMessage());
        }
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

    private EnumSet<IndoorMapper.Direction> getSelectedDirs() {
        EnumSet<IndoorMapper.Direction> dirs = EnumSet.noneOf(IndoorMapper.Direction.class);
        for (int i = 0; i < 3; i++) {
            ToggleButton tb = (ToggleButton) findViewById(turnButtonIds[i]);
            if (tb.isChecked()) {
                dirs.add(dir_const[i]);
            }
        }
        return dirs;
    }

    private IndoorMapper.Direction getSelectedForceTurn() {
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
            displayMsg("On node "+res.on_id+": "+res.on);
            interpret_part2(
                    indoorMapper.fork_part2(res.on_id, null, getSelectedForceTurn()));
        } else {
            Intent intent = new Intent(this, ChooseNode.class);
            intent.putExtra("me.dkess.indoormapper.DATA", res.choices);
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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

    private void interpret_part2(IndoorMapper.Direction nextDir) {
        displayMsg("You should turn "+nextDir);
        File file = new File(getExternalFilesDir(null), "nodes.json");
        boolean writeRes = indoorMapper.map.writeToFile(file);
        if (!writeRes) {
            displayMsg("File error! :(");
        }
        resetButtons();
    }

    public void onUndoPress(View view) {
        if (isUndoLongPressed) {
            File file = new File(getExternalFilesDir(null), "nodes.json");
            IndoorMapper.IndoorMap map = IndoorMapper.IndoorMap.readFromFile(file);
            indoorMapper = new IndoorMapper(map);
            IntString res = indoorMapper.undo();

            displayMsg("Undo. Now at " + res.n + ": "+res.s);
            boolean writeRes = indoorMapper.map.writeToFile(file);
            if (!writeRes) {
                displayMsg("File error! :(");
            }
            resetButtons();

        }
        isUndoLongPressed = false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayMsg("connected to gp");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                0,
                getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        displayMsg("gp connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        displayMsg("gp connection failed");
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "activity-detection-response-receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            /*
            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
            System.out.println("got something");
            System.out.println(updatedActivities);
            int highestType = -1;
            int highestConfidence = 0;
            for (DetectedActivity da : updatedActivities) {
                if (da.getConfidence())
            }
            */
            DetectedActivity da = intent.getParcelableExtra(Constants.BROADCAST_ACTION);
            long detectTime = intent.getLongExtra(Constants.DETECT_TIME, 0);
            boolean notMoving = da.getType() == DetectedActivity.STILL;

            TextView tv = (TextView) findViewById(R.id.walking_indicator);
            if (notMoving) {
                tv.setText("Not moving");
            } else {
                tv.setText("Moving");
            }

            File file = new File(getExternalFilesDir(null), "walking_log.txt");
            try {
                FileWriter fileWriter = new FileWriter(file, true);
                String status = notMoving ? "n " : "y ";
                fileWriter.write(status + detectTime);
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {}
        }
    }
}
