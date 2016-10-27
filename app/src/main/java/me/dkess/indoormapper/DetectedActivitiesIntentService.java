package me.dkess.indoormapper;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

/**
 * Created by daniel on 10/23/16.
 */

public class DetectedActivitiesIntentService extends IntentService {
    protected static final String TAG = "DetectedActivitiesIS";

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
        System.out.println("constructor called");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("on create called");
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        Intent localIntent = new Intent(Constants.BROADCAST_ACTION);

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        //ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        // Log each activity.
        /*
        System.out.println("activities detected");
        for (DetectedActivity da: detectedActivities) {
            System.out.println(da.getType() + " " + da.getConfidence() + "%");
        }
        */

        // Broadcast the list of detected activities.
        localIntent.putExtra(Constants.BROADCAST_ACTION, result.getMostProbableActivity());
        localIntent.putExtra(Constants.DETECT_TIME, result.getTime());
        //localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}
