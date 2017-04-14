package com.iheartlives.monitor.tasks;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.iheartlives.monitor.Patient;
import com.iheartlives.monitor.R;

import java.util.HashMap;
import java.util.Random;

public class MonitorActivity extends BaseActivity {
    public final static String EXTRA_MESSAGE = "com.iheartlives.monitor.START";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        HashMap<String, Patient> patients = createPatients();

        mHasOptionsMenu = false;

        // BARCODE SCAN STUFF
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        TextView patientNameView = (TextView) this.findViewById(R.id.patient_name);
        TextView sexTextView = (TextView) this.findViewById(R.id.sex);
        TextView dobTextView  = (TextView) this.findViewById(R.id.dob);

        try {
            patientNameView.setText(patients.get(message).getFirstName() + " " + patients.get(message).getLastName());
            sexTextView.setText(patients.get(message).getSex());
            dobTextView.setText(patients.get(message).getDob());
        } catch (Exception e){
            System.out.println("Invalid patient");
        }

        // HEART RATE STUFF
        TextView versionView = (TextView) this.findViewById(R.id.hello_label);
        PackageInfo info = null;
        try {
            info = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (info != null) {
            String versionName = info.versionName;
            int versionCode = info.versionCode;
            versionView.setText(String.format("%s: Version %d (%s)", info.applicationInfo.loadLabel(getPackageManager()), versionCode, versionName));
        }

//        Button appUpdateBtn = (Button) findViewById(R.id.app_update_button);
//        appUpdateBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                fireUpdateIntent();
//            }
//        });
//
//        Button fotaBtn = (Button) findViewById(R.id.fota_button);
//        fotaBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startFota();
//            }
//        });

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

        bpmIndicator.setText(String.valueOf(heartRate));
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

    }

    // START FOTA
    // adb shell am start com.bymason.platform.fota/com.bymason.platform.fota.activities.FotaCheckActivity
//    private void startFota() {
//        Toast.makeText(this, "Opening Mason Airship App...", Toast.LENGTH_SHORT).show();
//        Intent intent = new Intent()
//                .setAction("android.settings.SYSTEM_UPDATE_SETTINGS");
//        startActivity(intent);
//    }
//
//    private void fireUpdateIntent() {
//        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
//        Intent intent = new Intent()
//                .setAction("bymason.platform.action.CHECK_FOR_UPDATE")
//                .putExtra("bymason.platform.extra.TARGET_MODULE", getPackageName());
//        sendBroadcast(intent);
//    }


}
