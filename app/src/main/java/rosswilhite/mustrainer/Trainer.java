package rosswilhite.mustrainer;

import android.app.Activity;
import java.lang.Math;
import android.app.Fragment;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.media.AudioRecord;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageView;
import rosswilhite.mustrainer.LiveMonitor;



import static android.R.attr.layout_below;

import java.io.IOException;

public class Trainer extends AppCompatActivity {
    private static String pitchlist[] = {"C","C#/Db","D","D#/Eb","E","F","F#/Gb","G","G#/Ab","A","A#/Bb","B"};


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;

    private LiveButton mLiveButton = null;
    private LiveMonitor mLiveMonitor = null;
    private Thread UIUpdater = null;
   // private int MonitorAmp = 0;

    private boolean permissionToRecordAccepted = false;
    private boolean monitoring = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();

    }

    public double roundDouble(double d, int i) {
       return Math.round(d*(10^i)) / 10^i;
    }

    public void updateView(){


        //final boolean isdrawing = true;
        runOnUiThread(new Runnable() {
        @Override
        public void run() {

            if (monitoring){
            TextView monHz = (TextView) findViewById(R.id.monHZ);
                if (monHz == null)
                    Log.d("D","Frequency Monitor is uninitialized.");
            monHz.setText(String.format("{0} Hz",mLiveMonitor.getMonitorHz()));

                final String pitch = pitchlist[(int)mLiveMonitor.getMonitorPitch()];

                TextView monPitch = (TextView) findViewById(R.id.monPitch);
                if (monPitch == null)
                    Log.d("D","Pitch Monitor is uninitialized.");
                monPitch.setText(pitch);

            TextView monOct = (TextView) findViewById(R.id.monOct);
                if (monOct == null)
                    Log.d("D","Octave Monitor is uninitialized.");
            monOct.setText(String.format("{0}",mLiveMonitor.getMonitorOct()));


            //TextView sharp = (TextView) findViewById(R.id.IntonationLabels);
            //TextView flat = (TextView) findViewById(R.id.IntonationLabelf);
            //TextView natural = (TextView) findViewById(R.id.IntonationLabeln);
            TextView centsview = (TextView) findViewById(R.id.cents);
           // ImageView tunerDot = (ImageView) findViewById(R.id.TunerDot);
            double cents = roundDouble(mLiveMonitor.getCentsSharp(),2);
            centsview.setText(String.format("{0} cents",Double.toString(cents)));
            /*if (cents > 20){
                sharp.setVisibility(View.VISIBLE);
                natural.setVisibility(View.INVISIBLE);
                flat.setVisibility(View.INVISIBLE);
                tunerDot.setColorFilter(Color.RED);
                tunerDot.setPadding(0,0,0,(int)cents*4);
            }
            else if (cents < -20){
                sharp.setVisibility(View.INVISIBLE);
                natural.setVisibility(View.INVISIBLE);
                flat.setVisibility(View.VISIBLE);
                tunerDot.setColorFilter(Color.RED);
                tunerDot.setPadding(0,-(int)cents*4,0,0);
            }
            else {
                sharp.setVisibility(View.INVISIBLE);
                natural.setVisibility(View.VISIBLE);
                flat.setVisibility(View.INVISIBLE);
                tunerDot.setColorFilter(Color.GREEN);
                tunerDot.setPadding(0,0,0,0);

            }*/}
            {
                TextView monHz = (TextView) findViewById(R.id.monHZ);
                monHz.setText("0 Hz");

                TextView monOct = (TextView) findViewById(R.id.monOct);
                monOct.setText("0");

                TextView monPitch = (TextView) findViewById(R.id.monPitch);
                monPitch.setText("0");

               // TextView sharp = (TextView) findViewById(R.id.IntonationLabels);
                //TextView flat = (TextView) findViewById(R.id.IntonationLabelf);
                //TextView natural = (TextView) findViewById(R.id.IntonationLabeln);
                TextView centsview = (TextView) findViewById(R.id.cents);
                ImageView tunerDot = (ImageView) findViewById(R.id.TunerDot);
                centsview.setText("0 cents");
                //    sharp.setVisibility(View.INVISIBLE);
                //    natural.setVisibility(View.INVISIBLE);
                //    flat.setVisibility(View.INVISIBLE);
                    tunerDot.setColorFilter(Color.RED);
                    tunerDot.setPadding(0,0,0,0);
            }

            mLiveMonitor.validateUpdate();

        }
    });
        //TextView mvuMeter = (TextView) findViewById(R.id.vuMeter);
      //  mvuMeter.setText(Double.toString(mLiveMonitor.getLatestMonitorLevel()));
    }


    class LiveButton extends Button {

        TextView mvuMeter = null;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                if (!monitoring) {
                    setText("Stop tuner");
                    //mvuMeter.setText

                } else {
                    setText("Start tuner");
                }
                monitoring = !monitoring;
                UIUpdater.start();
                mLiveMonitor.onMonitor(!monitoring);
            }
        };

        public LiveButton(Context ctx) {
            super(ctx);
            setText("Start tuner");
            setOnClickListener(clicker);
        }
    }


   /* public interface UIupdateListener {
    public void onObjectReady(String title);
    }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainer);

        // R.layout.activity_trainer.var.
        // Record to the external cache directory for visibility
        mFileName = getExternalCacheDir().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
                        //requestPermissions(Manifest.permission.RECORD_AUDIO);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        //LinearLayout ll = new LinearLayout(this);
       // LiveButton LiveB = (LiveButton) findViewById(R.id.tunerButton);
        RelativeLayout R1 = (RelativeLayout) findViewById(R.id.activity_trainer);
        mLiveButton = new LiveButton(this);
        mLiveMonitor = new LiveMonitor();

        String samplerateString = null, buffersizeString = null;
        if (Build.VERSION.SDK_INT >= 17) {
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString == null) samplerateString = "44100";
        if (buffersizeString == null) buffersizeString = "512";
        mLiveMonitor.setUp(Integer.parseInt(samplerateString),Integer.parseInt(buffersizeString));

        RelativeLayout.LayoutParams MonBParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        MonBParams.addRule(RelativeLayout.BELOW,R.id.pagename);
        MonBParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        //MonBParams.addRule(android:layout_below,android"@id/vuMeter");
        R1.addView(mLiveButton,MonBParams);
        setContentView(R1);
        updateView();

        UIUpdater = new Thread(new Runnable() {
            public void run() {
                while (monitoring) {
                    updateView();
                }
            }
        }, "UI Updater Thread");
        UIUpdater.start();
    }

}
