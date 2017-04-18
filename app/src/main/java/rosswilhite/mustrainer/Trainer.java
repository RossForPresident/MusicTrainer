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


/*class LiveMonitor2 extends Fragment {


    //TextView mvuMeter = null;
    private AudioRecord livetrainer = null;
    private Thread recordingThread = null;
    private UIupdateListener UIupdater = null;
    private FFT fftest = null;


    private int audioSource;    //Recording Source
    private int SampleRateInHz = 44100; // the sample rate expressed in Hertz. Usually 44.1k
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;  //describes the configuration of the audio channels
    private int audioFormat;    //the format in which the audio data is to be returned
    private int minBufferSize; //total size in bytes of buffer

    private int BufferElements = 1024;
    private int ElementSize = 2; //in bytes (16-bit) DCM

    private short monitorPeak = 0;
    private short nmonitorPeak = 0;
    private double monitorPitch = 0;
    private int monitorOct = 0;
    private double monitorHZ = 0;
    private double centsSharp = 0;
    private double[] freqTable = new double[BufferElements];
    private double[] pitchTable = new double[BufferElements];
    private String[] noteNameTable = new String[BufferElements];
    private boolean isRecording = false;
    private boolean updating = false;

    private static String pitchlist[] = {"C","C#/Db","D","D#/Eb","E","F","F#/Gb","G","G#/Ab","A","A#/Bb","B"};

    static{

        System.loadLibrary("Superpoweredlib");
    }

    //CONSTANTS
    // --- ERRORS
    private static final int ERROR = -1;
    private static final int ERROR_TASK_NOT_READY = -2;


    public LiveMonitor2(){
        audioSource = MediaRecorder.AudioSource.MIC;
        SampleRateInHz = 44100;
        channelConfig = AudioFormat.CHANNEL_IN_MONO;
        audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        minBufferSize = AudioRecord.getMinBufferSize(SampleRateInHz,channelConfig,audioFormat);


        UIupdater = null;
        //setContentView(R.layout.activity_trainer);
    }

    public void setUIListener(UIupdateListener UIupdate){
        UIupdater = UIupdate;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_trainer);

    }


    public double getLatestMonitorLevel(){
        short d = monitorPeak;
        monitorPeak = 0;
        return d;
    }
    public double getLatestNMonitorLevel(){
        short d = nmonitorPeak;
        nmonitorPeak = 0;
        return d;
    }
    public double getMonitorHz(){
        double d = monitorHZ;
        monitorHZ = 0;
        return d;
    }
    public double getMonitorPitch() {
        double d = monitorPitch;
        monitorPitch = 0;
        return d;
    }
    public int getMonitorOct() {
        int d = monitorOct;
        monitorOct = 0;
        return d;
    }

    public double getCentsSharp() {
        double d = centsSharp;
        centsSharp = 0;
        return d;
    }

    public void validateUpdate() {
        updating = false;
    }

    void onMonitor(boolean start) {
        if (start) {
            startMonitoring();
        } else {
            stopMonitoring();
        }
    }

    private void startMonitoring() {

        // mvuMeter = (TextView) findViewById(R.id.vuMeter);
        monitorPeak = 0;
        nmonitorPeak = 0;
        livetrainer = new AudioRecord(audioSource, SampleRateInHz, channelConfig,
                audioFormat, BufferElements * ElementSize);
        livetrainer.setRecordPositionUpdateListener(monitoring);
        livetrainer.setPositionNotificationPeriod(4410);

        if (livetrainer.getState() == AudioRecord.STATE_UNINITIALIZED) {
            //  Log.e(LOG_TAG, "startMonitoring() failed");
        }
        //IF USING API 23:
        /*new AudioRecord.Builder()
                .setAudioSource(audioSource)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SampleRateInHz)
                        .setChannelMask(channelConfig)
                        .build())
                .setBufferSize(bufferSizeInBytes)
                .build();*/
/*
        for  (int i = 0; i < BufferElements; i++){
            noteNameTable[i] = null;
            pitchTable[i] = -1;
            freqTable[i] = (SampleRateInHz * i ) / ( BufferElements);
        }
        for (int i = 0; i<127; i++) {
            double pitch =  (440.0 / 32.0) *  Math.pow(2, (i - 9.0) / 12.0);
            if (pitch > SampleRateInHz / 2.0)
                break;

            double min = 10000000000.0;
            int index = -1;
            for (int j = 0; j < BufferElements; j++) {
                if (Math.abs(freqTable[j] - pitch) < min) {
                    min = Math.abs(freqTable[j] - pitch);
                    index = j;
                }
            }
            noteNameTable[index] = pitchlist[i%12];
            pitchTable[index] = pitch;
        }

        livetrainer.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                monitorAudio();
            }
        }, "LiveTuner Thread");
        recordingThread.start();
    }

    public static double[] short2double(short[] pcms) {
        double[] doublers = new double[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            doublers[i] = (double)pcms[i];
        }
        return doublers;
    }

    public void monitorAudio(){
        short sData[] = new short[BufferElements];
        double[] dData = new double[BufferElements];
        boolean isPositive = true;

        while (isRecording){
            int crosses = 0;
            int octave = 0;
            //monitorPitch = 0;
            //monitorOct = 0;

            while(updating){}
            livetrainer.read(sData, 0, BufferElements);
            dData = short2double(sData);

            Complex[] fftTempArray = new Complex[BufferElements];
            for (int i=0; i<BufferElements; i++)
            {
                fftTempArray[i] = new Complex(dData[i], 0);
            }
            Complex[] fftArray = FFT.fft(fftTempArray);
            double[] Abs = new double[BufferElements/2];

            double mMaxFFTSample = 0.0;
            int mPeakPos = 0;
            for(int i = 0; i < (BufferElements/2); i++)
            {
                Abs[i] = Math.sqrt(Math.pow(fftArray[i].re(), 2) + Math.pow(fftArray[i].im(), 2));
                if(Abs[i] > mMaxFFTSample)
                {
                    mMaxFFTSample = Abs[i];
                    mPeakPos = i;
                }
            }

            /*for (int i = 0; i < sData.length; i++){
                if (isPositive && sData[i] < 0){
                    crosses++;
                    isPositive = false;
                }
                if (sData[i] > 0){
                    isPositive = true;
                }
                if (sData[i] > monitorPeak)
                    monitorPeak = sData[i];
                if (sData[i] < nmonitorPeak)
                    nmonitorPeak = sData[i];
            }*/
            //monitorHZ = (double)((double)crosses * (double)(SampleRateInHz/sData.length));
   /*         monitorHZ = freqTable[mPeakPos];
            monitorPitch = Math.round(12*(Math.log(monitorHZ/261.626))/Math.log(2));
            monitorPitch += 48;
            while (monitorPitch >= 12){
                monitorPitch -= 12;
                monitorOct++;
            }
            while (monitorPitch < 0){
                monitorPitch += 12;
                monitorOct--;
            }

            int nearestNoteDelta=0;

            while( true ) {
                if( nearestNoteDelta < mPeakPos && noteNameTable[mPeakPos-nearestNoteDelta] != null ) {
                    nearestNoteDelta = -nearestNoteDelta;
                    break;
                } else if( nearestNoteDelta + mPeakPos < BufferElements && noteNameTable[mPeakPos+nearestNoteDelta] != null ) {
                    break;
                }
                ++nearestNoteDelta;
            }
            String nearestPitch = noteNameTable[mPeakPos+nearestNoteDelta];
            double nearestHZ = pitchTable[mPeakPos+nearestNoteDelta];
            centsSharp = 1200 * Math.log( monitorHZ / nearestHZ ) / Math.log( 2.0 );
            //updateView();
            updating = true;
            UIupdater.updateUI();

        }
    }

    AudioRecord.OnRecordPositionUpdateListener monitoring = new AudioRecord.OnRecordPositionUpdateListener() {
        public void onMarkerReached(AudioRecord recorder) {

        }
        public void onPeriodicNotification(AudioRecord recorder) {
            // Trainer.
        }
    };

    private void stopMonitoring() {
        isRecording = false;
        livetrainer.stop();
        if (livetrainer != null) {
            livetrainer.release();
            livetrainer = null;
        }
        recordingThread = null;
    }
}
*/


public class Trainer extends AppCompatActivity {
    private static String pitchlist[] = {"C","C#/Db","D","D#/Eb","E","F","F#/Gb","G","G#/Ab","A","A#/Bb","B"};


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;

    private LiveButton mLiveButton = null;
    private LiveMonitor mLiveMonitor = null;
    private UIupdateListener UIupdater = null;
   // private int MonitorAmp = 0;

    private boolean permissionToRecordAccepted = false;
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

        final String pitch = pitchlist[(int)mLiveMonitor.getMonitorPitch()];

        final boolean isdrawing = true;
        runOnUiThread(new Runnable() {
        @Override
        public void run() {

            if (isdrawing){
            TextView monHz = (TextView) findViewById(R.id.monHZ);
                if (monHz == null)
                    Log.d("D","Frequency Monitor is uninitialized.");
            monHz.setText(String.format("{0} Hz",mLiveMonitor.getMonitorHz()));

            TextView monOct = (TextView) findViewById(R.id.monOct);
                if (monOct == null)
                    Log.d("D","Octave Monitor is uninitialized.");
            monOct.setText(String.format("{0}",mLiveMonitor.getMonitorOct()));

            TextView monPitch = (TextView) findViewById(R.id.monPitch);
                if (monPitch == null)
                    Log.d("D","Frequency Monitor is uninitialized.");
            monPitch.setText(pitch);

            //TextView sharp = (TextView) findViewById(R.id.IntonationLabels);
            //TextView flat = (TextView) findViewById(R.id.IntonationLabelf);
            //TextView natural = (TextView) findViewById(R.id.IntonationLabeln);
            TextView centsview = (TextView) findViewById(R.id.cents);
            ImageView tunerDot = (ImageView) findViewById(R.id.TunerDot);
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
            else{
                TextView monHz = (TextView) findViewById(R.id.monHZ);
                monHz.setText("0 Hz");

                TextView monOct = (TextView) findViewById(R.id.monOct);
                monOct.setText("");

                TextView monPitch = (TextView) findViewById(R.id.monPitch);
                monPitch.setText("0");

               // TextView sharp = (TextView) findViewById(R.id.IntonationLabels);
                //TextView flat = (TextView) findViewById(R.id.IntonationLabelf);
                //TextView natural = (TextView) findViewById(R.id.IntonationLabeln);
                TextView centsview = (TextView) findViewById(R.id.cents);
                ImageView tunerDot = (ImageView) findViewById(R.id.TunerDot);
                centsview.setText("cents");
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
        boolean mStartRecording = true;

        TextView mvuMeter = null;

        OnClickListener clicker = new OnClickListener() {
            public void onClick(View v) {
                mLiveMonitor.onMonitor(mStartRecording);
                if (mStartRecording) {
                    setText("Stop tuner");
                    //mvuMeter.setText

                } else {
                    setText("Start tuner");
                }
                mStartRecording = !mStartRecording;
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


        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

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


        mLiveMonitor.setUIListener(new UIupdateListener() {
            @Override
            public void updateUI() {
            updateView();
            }
        });
        RelativeLayout.LayoutParams MonBParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        MonBParams.addRule(RelativeLayout.BELOW,R.id.pagename);
        MonBParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        //MonBParams.addRule(android:layout_below,android"@id/vuMeter");
        R1.addView(mLiveButton,MonBParams);
        setContentView(R1);
        updateView();
    }

}
