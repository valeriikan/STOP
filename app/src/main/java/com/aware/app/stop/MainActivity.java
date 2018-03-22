package com.aware.app.stop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView mMainNav;
    private FrameLayout mMainFrame;
    private GameFragment gameFragment;
    private MedicationFragment medicationFragment;

    private static NotificationManager manager;


    public static final String STOP_TAG = "STOP_TAG";
    public static final String ACTION_STOP_FINGERPRINT = "ACTION_STOP_FINGERPRINT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.pref_ball_game, true);
        Aware.isBatteryOptimizationIgnored(getApplicationContext(), "com.aware.app.stop");
        Aware.setSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_ACCELEROMETER, 20000);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_DB_SLOW, true);

        // Get an instance of the NotificationManager service
        manager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(5);

        scheduleNotification();

        mMainFrame = findViewById(R.id.main_frame);
        mMainNav = findViewById(R.id.main_nav);
        //mMainNav.setSelectedItemId(R.id.nav_game);

        medicationFragment = new MedicationFragment();
        gameFragment = new GameFragment();

        setFragment(gameFragment);

        mMainNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.nav_game:
                        setFragment(gameFragment);
                        return true;
                    case R.id.nav_medication:
                        setFragment(medicationFragment);
                        return true;
                    default:
                        return false;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setFragment(Fragment fragment) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.main_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Notification scheduler: 4 times per day
    private void scheduleNotification() {

        try {

            Scheduler.Schedule morning = Scheduler.getSchedule(this, "morning");
            if (morning == null) {
                morning = new Scheduler.Schedule("morning");
                morning.addHour(8).addHour(11)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra("trigger-time", "Morning");

                Scheduler.saveSchedule(getApplicationContext(), morning);
                Aware.startScheduler(getApplicationContext());
            }

            Scheduler.Schedule noon = Scheduler.getSchedule(this, "noon");
            if (noon == null) {
                noon = new Scheduler.Schedule("noon");
                noon.addHour(12).addHour(14)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra("trigger-time", "Noon");

                Scheduler.saveSchedule(getApplicationContext(), noon);
                Aware.startScheduler(getApplicationContext());
            }

            Scheduler.Schedule afternoon = Scheduler.getSchedule(this, "afternoon");
            if (afternoon == null) {
                afternoon = new Scheduler.Schedule("afternoon");
                afternoon.addHour(15).addHour(18)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra("trigger-time", "Afternoon");

                Scheduler.saveSchedule(getApplicationContext(), afternoon);
                Aware.startScheduler(getApplicationContext());
            }

            Scheduler.Schedule evening = Scheduler.getSchedule(this, "evening");
            if (evening == null) {
                evening = new Scheduler.Schedule("evening");
                evening.addHour(19).addHour(21)
                        .random(1,0)
                        .setActionType(Scheduler.ACTION_TYPE_BROADCAST)
                        .setActionIntentAction(MainActivity.ACTION_STOP_FINGERPRINT)
                        .addActionExtra("trigger-time", "Evening");

                Scheduler.saveSchedule(getApplicationContext(), evening);
                Aware.startScheduler(getApplicationContext());
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Notification set up
    private static void notifyShow(Context c, String notifyText) {

        // Attach activity opened in onClick
        PendingIntent contentIntent = PendingIntent.getActivity(c, 0,
                new Intent(c, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        // Build Notification
        long vibrate[] = {0, 1000};
        Notification.Builder builder = new Notification.Builder(c)
                .setSmallIcon(R.drawable.ic_action_aware_studies)
                .setOngoing(true)
                .setVibrate(vibrate)
                .setContentTitle("STOP")
                .setContentText(notifyText)
                .setContentIntent(contentIntent);

        // Build the notification and show it.
        manager.notify(5, builder.build());

        // Vibrate at notification
        Vibrator vibrator = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(500);
    }

    // BroadcastReceiver to trigger notifications
    public static class ParkinsonSnapshot extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(MainActivity.ACTION_STOP_FINGERPRINT)) {
                Log.d(MainActivity.STOP_TAG, "Broadcast received");

                Random r = new Random();
                int sensitivity = r.nextInt(6 - 1) + 1;

                SharedPreferences sPref = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
                SharedPreferences.Editor editor = sPref.edit();
                editor.putString("key_sensitivity", String.valueOf(sensitivity));
                editor.commit();

                Aware.debug(context, "STOP-notification triggered: " + intent.getStringExtra("trigger-time"));
                notifyShow(context,intent.getStringExtra("trigger-time") + ": play a game and record medication");
            }
        }
    }
}
