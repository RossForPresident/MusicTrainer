package rosswilhite.mustrainer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Trainer_Backup extends AppCompatActivity {

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String mFileName = null;

    private LiveButton mLiveButton = null;
    private LiveMonitor mLiveMonitor = null;
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

    public void updateView(){
        TextView mvuMeter = (TextView) findViewById(R.id.pagename);
        mvuMeter.setText(Double.toString(mLiveMonitor.getLatestMonitorLevel()));
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
