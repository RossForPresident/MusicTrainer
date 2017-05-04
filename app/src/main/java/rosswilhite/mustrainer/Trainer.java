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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ImageView;
import rosswilhite.mustrainer.LiveMonitor;



import static android.R.attr.layout_below;

import java.io.IOException;

public class Trainer extends AppCompatActivity {


    static{

        System.loadLibrary("Superpoweredlib");
    }

    private static String pitchlist[] = {"C","C#/Db","D","D#/Eb","E","F","F#/Gb","G","G#/Ab","A","A#/Bb","B"};


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;

    private int SampleRateInHz = 44100;
    private int minBufferSize = 512;
    private double maxHZ = 1500;

    private LiveButton mLiveButton = null;
    //private LiveMonitor mLiveMonitor = null;
    private Thread TunerThread = null;
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

    private void updater(double HZ,float th){
        final double hz = HZ;
        final float thr = th;
        Thread UIThread = new Thread(new Runnable() {
            public void run() {
                updateView(hz,thr);
            }
        }, "UI Thread");
        UIThread.start();
    }
    private void updateView(double HZ, float th){


        //final double monitorHZ = getHzField();
        final double monitorHZ = HZ;
        final float thr = th;

        //final boolean isdrawing = true;
        runOnUiThread(new Runnable() {
        @Override
        public void run() {

            final TextView thresh = (TextView)findViewById(R.id.ThreshMon);
            if (thresh == null)
                Log.d("D", "Threshold Monitor is uninitialized.");
            else
            thresh.setText(String.format("%1$.3f", thr));
            if (monitoring && monitorHZ > 0) {
                double monitorPitch = Math.round(12 * (Math.log(monitorHZ / 261.626)) / Math.log(2));
                monitorPitch += 48;
                int monitorOct = 0;
                while (monitorPitch >= 12) {
                    monitorPitch -= 12;
                    monitorOct++;
                }
                while (monitorPitch < 0) {
                    monitorPitch += 12;
                    monitorOct--;
                }
                String pitch = pitchlist[(int) monitorPitch];
                double nearestHZ = (Math.pow(2, (((monitorPitch + (12 * monitorOct)) - 48) / 12))) * 261.626;
                double centsSharp = 1200 * Math.log(monitorHZ / nearestHZ) / Math.log(2.0);

                TextView monHz = (TextView) findViewById(R.id.monHZ);
                if (monHz == null)
                    Log.d("D", "Frequency Monitor is uninitialized.");
                else
                monHz.setText(String.format("%1$.2f Hz", monitorHZ));

                TextView monPitch = (TextView) findViewById(R.id.monPitch);
                if (monPitch == null)
                    Log.d("D", "Pitch Monitor is uninitialized.");
                monPitch.setTextColor(Color.argb(255, 100 + (int) ((Math.abs(centsSharp) / 40)) * 100, 100, 100));
                if (centsSharp < 7 && centsSharp > -7) {
                    monPitch.setTextColor(Color.GREEN);
                }
                monPitch.setText(pitch);

                TextView monOct = (TextView) findViewById(R.id.monOct);
                if (monOct == null)
                    Log.d("D", "Octave Monitor is uninitialized.");
                monOct.setText(String.format("%1$d", monitorOct));

                TextView centsview = (TextView) findViewById(R.id.cents);
                double cents = roundDouble(centsSharp, 2);
                centsview.setText(String.format("%s cents", Double.toString(cents)));
            }
            else {
                TextView monHz = (TextView) findViewById(R.id.monHZ);
                if (monHz == null)
                    Log.d("D","Frequency Monitor is uninitialized.");
                else
                    monHz.setText("0 Hz");

                TextView monOct = (TextView) findViewById(R.id.monOct);
                if (monHz == null)
                    Log.d("D","Octave Monitor is uninitialized.");
                else
                monOct.setText("0");

                TextView monPitch = (TextView) findViewById(R.id.monPitch);
                if (monHz == null)
                    Log.d("D","Pitch Monitor is uninitialized.");
                else
                monPitch.setText("0");
            }

        }
    });
    }


    class LiveButton extends Button {

        TextView mvuMeter = null;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                monitoring = !monitoring;
                if (monitoring) {
                    setText("Stop tuner");
                    //mvuMeter.setText
                    TunerThread.start();
                    //StartRec(SampleRateInHz, minBufferSize, maxHZ);

                } else {
                    setText("Start tuner");
                    StopRec();
                }
             //   UIUpdater.start();
            }
        };

        public LiveButton(Context ctx) {
            super(ctx);
            setText("Start tuner");
            setOnClickListener(clicker);
        }
    }


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
     //   mLiveMonitor = new LiveMonitor();

        String samplerateString = null, buffersizeString = null;
        if (Build.VERSION.SDK_INT >= 17) {
            AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString != null) SampleRateInHz = Integer.parseInt(samplerateString);
        if (buffersizeString != null) minBufferSize = Integer.parseInt(buffersizeString);
      //  mLiveMonitor.setUp(Integer.parseInt(samplerateString),Integer.parseInt(buffersizeString));

        RelativeLayout.LayoutParams MonBParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        MonBParams.addRule(RelativeLayout.BELOW,R.id.pagename);
        MonBParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        //MonBParams.addRule(android:layout_below,android"@id/vuMeter");
        R1.addView(mLiveButton,MonBParams);
        final SeekBar crossfader = (SeekBar)findViewById(R.id.Sensbar);
        if (crossfader != null) crossfader.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                onThresh(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        setContentView(R1);
        updateView(0,0);

        TunerThread = new Thread(new Runnable() {
            public void run() {
                StartRec(SampleRateInHz, 2400, maxHZ);
            }
        }, "Tuner Thread");
       // UIUpdater.start();
    }

    public native void StartRec(int samplerate,int buffersize, double HzHi);
    public native void StopRec();
    public native void update(int samplerate,int buffersize, double HzHi);
    public native void GetStatus();
    public native double getHzField();
    private native void onThresh(int value);
}
