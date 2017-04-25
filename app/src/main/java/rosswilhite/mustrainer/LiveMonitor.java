package rosswilhite.mustrainer;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;

import rosswilhite.mustrainer.Complex;
import rosswilhite.mustrainer.FFT;
import rosswilhite.mustrainer.UIupdateListener;

public class LiveMonitor{

    private int SampleRateInHz = 48000; // the sample rate expressed in Hertz. Usually 44.1k
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;  //describes the configuration of the audio channels
//    private int audioFormat;    //the format in which the audio data is to be returned
    private int minBufferSize; //total size in bytes of buffer

    private int BufferElements = 1024;
    private int ElementSize = 2; //in bytes (16-bit) DCM

    private short monitorPeak = 0;
    private short nmonitorPeak = 0;
    private double monitorPitch = 0;
    private int monitorOct = 0;
    private double monitorHZ = 0;
    private double centsSharp = 0;
    private double maxHZ = 1500;
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


    public LiveMonitor(){

        channelConfig = AudioFormat.CHANNEL_IN_MONO;
        //audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        //minBufferSize = AudioRecord.getMinBufferSize(SampleRateInHz,channelConfig,audioFormat);
        minBufferSize = 512;
        SampleRateInHz = 44100;
    }


    public void setUp(int sampleRateHz, int bufferSize) {
      //  super.onCreate(savedInstanceState);
        SampleRateInHz = sampleRateHz;
        minBufferSize = bufferSize;

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
        monitorHZ = getHzField();
        return monitorHZ;
    }
    public double getMonitorPitch() {
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

        return monitorPitch;
    }
    public int getMonitorOct() {
        return monitorOct;
    }

    public double getCentsSharp() {
        double nearestHZ = (Math.pow(2,((monitorPitch-(12*monitorOct))/12)))*261.626;
        centsSharp = 1200 * Math.log( monitorHZ / nearestHZ ) / Math.log( 2.0 );

        return centsSharp;
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
        monitorPeak = 0;
        nmonitorPeak = 0;
        /*
        livetrainer = new AudioRecord(audioSource, SampleRateInHz, channelConfig,
                audioFormat, BufferElements * ElementSize);
        livetrainer.setRecordPositionUpdateListener(monitoring);
        livetrainer.setPositionNotificationPeriod(4410);

        if (livetrainer.getState() == AudioRecord.STATE_UNINITIALIZED) {
            //  Log.e(LOG_TAG, "startMonitoring() failed");
        }
        */

       /* for  (int i = 0; i < BufferElements; i++){
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
        }*/

        StartRec(SampleRateInHz, minBufferSize, maxHZ);
    }

    public static double[] short2double(short[] pcms) {
        double[] doublers = new double[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            doublers[i] = (double)pcms[i];
        }
        return doublers;
    }


    private void stopMonitoring() {
        isRecording = false;
        StopRec();
    }

    public native void StartRec(int samplerate,int buffersize, double HzHi);
    public native void StopRec();
    public native void update(int samplerate,int buffersize, double HzHi);
    public native void GetStatus();
    public native double getHzField();
}
