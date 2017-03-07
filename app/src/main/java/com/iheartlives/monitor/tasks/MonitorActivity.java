package com.iheartlives.monitor.tasks;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.iheartlives.monitor.Patient;
import com.iheartlives.monitor.R;
import com.iheartlives.monitor.comms.Message;
import com.iheartlives.monitor.comms.MonitorBackend;
import com.iheartlives.monitor.comms.WSMonitorBackendImpl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

public class MonitorActivity extends BaseActivity {
    public final static String EXTRA_MESSAGE = "com.iheartlives.monitor.START";
    public static final String TAG = "MonitorActivity";

    private TextView mStatusLabel;

    private MonitorBackend mBackend;
    private MonitorBackend.ClientListener mServerCommsListener = new MonitorBackend
            .ClientListener() {
        @Override
        public void onReady() {
            setStatus("Connection succeeded");
        }

        @Override
        public void onPause() {
            setStatus("Connection paused");
        }

        @Override
        public void onResume() {
            setStatus("Connection resumed");
        }

        @Override
        public void onMessage(Message message) {
            setStatus(String.format(Locale.US, "Got '%s' (%d bytes)",
                    message.type, message.message.length()));
        }

        @Override
        public void onError() {
            setStatus("Connection failed");
            mBackend = null;
        }

        @Override
        public void onComplete() {
            setStatus("Connection closed");
            mBackend = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        HashMap<String, Patient> patients = createPatients();

        mHasOptionsMenu = false;
        mBackend = null;

        // BARCODE SCAN STUFF
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        TextView patientNameView = (TextView) this.findViewById(R.id.patient_name);
        TextView sexTextView = (TextView) this.findViewById(R.id.sex);
        TextView dobTextView = (TextView) this.findViewById(R.id.dob);
        mStatusLabel = (TextView) findViewById(R.id.status_label);

        try {
            patientNameView.setText(patients.get(message).getFirstName() + " " +
                    patients.get(message).getLastName());
            sexTextView.setText(patients.get(message).getSex());
            dobTextView.setText(patients.get(message).getDob());
        } catch (Exception e) {
            System.out.println("Invalid patient");
        }

        // HEART RATE STUFF
        TextView versionView = (TextView) this.findViewById(R.id.version_label);
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (info != null) {
            String versionName = info.versionName;
            int versionCode = info.versionCode;
            versionView.setText(String.format(Locale.US, "%s: Version %d (%s)",
                    info.applicationInfo.loadLabel(getPackageManager()), versionCode, versionName));
        }

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateBPM();
                handler.postDelayed(this, 3000);
            }
        }, 3000);

    }

    private void updateBPM() {
        TextView bpmIndicator = (TextView) this.findViewById(R.id.bmp_indicator);

        Random r = new Random();
        int heartRate = r.nextInt(72 - 68) + 68;

        String rateStr = String.valueOf(heartRate);
        bpmIndicator.setText(rateStr);
        sendMessageToServer(rateStr);
    }

    private void sendMessageToServer(String rateStr) {
        if (mBackend != null && mBackend.ready()) {
            Log.d(TAG, "Sending message to server");
            Message msg = new Message("data", rateStr);
            mBackend.sendMessage(msg);
        }
    }

    private HashMap<String, Patient> createPatients() {
        HashMap<String, Patient> patients = new HashMap<String, Patient>();
        patients.put("4537691221", new Patient("George", "Washington", "M", "02/22/1732"));
        patients.put("653767141", new Patient("Abraham", "Lincoln", "M", "02/12/1809"));
        patients.put("8339671550", new Patient("Franklin", "Roosevelt", "M", "01/30/1882"));
        patients.put("6507601325", new Patient("Amelia", "Earhart", "F", "07/24/1897"));
        patients.put("5438671481", new Patient("Marie", "Curie", "F", "11/12/1867"));
        patients.put("335672544", new Patient("Harriet", "Tubman", "F", "01/01/1822"));
        patients.put("1", new Patient("Jim", "Xiao", "M", "11/01/1988"));

        return patients;
    }

    public void clear(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MESSAGE, "RESET");
        startActivity(intent);
    }

    public void toggleUpload(View view) {
        if (mBackend == null) {
            ((Button) view).setText(getString(R.string.stop_upload));
            mBackend = WSMonitorBackendImpl.get();
            mBackend.init(this, mServerCommsListener);
            setStatus("Connecting...");
        } else {
            ((Button) view).setText(getString(R.string.start_upload));
            mBackend.close();
            mBackend = null;
            setStatus(getString(R.string.idle));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        mBackend.close();
        mBackend = null;
        setStatus("Stopping");
    }

    private void setStatus(String s) {
        mStatusLabel.setText(s);
    }
}
