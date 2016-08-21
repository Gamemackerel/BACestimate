package com.example.abe.bac_dr1;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;
import java.util.Random;

public class BACIntentService extends IntentService {
    private String TAG = "console";
    public static final String PARAM_IN_MSG = "imsg";
    public static final String PARAM_OUT_MSG = "omsg";
    private static BAC bac;

    public BACIntentService() {
        super("SimpleIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String msg = intent.getStringExtra(PARAM_IN_MSG);
        Log.d(TAG, "BACIntentService: work order recieved, it was: " + msg);

        String[] userData = msg.split(" ");

        //3 possible instructions: start object, take shot, do metabolization calc, stop object
        //if the first char marker is s, then follow instructions for starting the BAC app
        if(msg.charAt(0) == 's') {
            Log.d(TAG, "starting BAC object with these parameters:" + Arrays.toString(userData));

            int bodyType = Integer.parseInt(userData[1]);
            boolean male = Boolean.parseBoolean(userData[2]);
            double KgramsWeight = (Double.parseDouble(userData[3]) / 1000); //change back to Kgs
            //int MetabModifier = Integer.parseInt(userData[4]);

            bac = new BAC(KgramsWeight, male, bodyType);
            Log.d(TAG, "BAC object initialized, now I'm going to start the BACUpdateSchedulerIntentService");


            //start the UpdateSchedulerIntentService that gives a scheduled work order to update bac once a minute
            Intent msgIntent = new Intent(this, BACUpdateSchedulerIntentService.class);
            msgIntent.putExtra(BACUpdateSchedulerIntentService.PARAM_IN_MSG, "");
            startService(msgIntent);
        } else if(msg.charAt(0) == 't') {
            Double volume = Double.parseDouble(userData[1]);
            Double percentEth = Double.parseDouble(userData[2]);
            Log.d(TAG, "message recieved from Main2Activity, it wants me to take a drink");
            bac.takeShot(volume, percentEth);
        } else if(msg.charAt(0) == 'm') {
            //this should be called on a scheduled execution once a minute
            //this should be called on a scheduled execution once a minute
            //if i wanted to update once a second I would have to change these methods
            Log.d(TAG, "BACInentService: message recieved from updateScheduler, it wants me to update the BAC");
            bac.absorbUpdate();
            bac.metabolizeUpdate();
            Log.d(TAG, "I updated the BAC, the new value for BAC is: " + bac.getBAC());
            //Now I should use the updated BAC as a outbound message that can be read by any activity
            //which needs to display the current estimate
        } else if (msg.charAt(0) == 'x') {
            //end sesh
            //not sure if the user should ever manually do this, probably not.
            //maybe could be implemented by deleting the bac object by reassigning it to null
            bac = null;
        }

        double thisBAC = bac.getBAC();
        String resultTxt = "" + thisBAC;
        Log.d(TAG, "passing back to main2activity: BAC is: " + resultTxt);
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(Main2Activity.ResponseReceiver.ACTION_RESP);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(PARAM_OUT_MSG, resultTxt);
        sendBroadcast(broadcastIntent);

    }
}