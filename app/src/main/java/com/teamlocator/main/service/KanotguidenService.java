package com.teamlocator.main.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.teamlocator.main.R;
import com.teamlocator.main.model.Mission;
import com.teamlocator.main.ui.SplashActivity;
import com.teamlocator.main.utils.ConnectionUtils;
import com.teamlocator.main.utils.MiscUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class KanotguidenService extends Service {

    private static final int INTERVAL_SCAN_MISSIONS_MINUTES = 2;
    private static final int INTERVAL_CHECK_INTERNET_SECONDS = 30;

    private MissionService missionService;
    private NotificationManager notificationManager;

    public KanotguidenService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        missionService = new MissionService(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, Service.START_FLAG_REDELIVERY | Service.START_FLAG_RETRY, startId);
        Observable.interval(0, INTERVAL_SCAN_MISSIONS_MINUTES, TimeUnit.MINUTES, Schedulers.newThread()).subscribe(aLong -> {
            // download missions
            missionService.getMissionsFromServer().subscribe(
                    missionList -> Log.d("", ""),
                    throwable -> Log.d("", ""));

            // check if any mission in mission range
            startMission(missionService.checkIfNearMission(getApplicationContext()));
        });

        Observable.interval(0, INTERVAL_CHECK_INTERNET_SECONDS, TimeUnit.SECONDS, Schedulers.newThread()).subscribe(aLong -> {
            // check internet and resend data cached missions to server
            if (!missionService.getSkippedMissions().isEmpty() && ConnectionUtils.isConnected(getApplicationContext())) {
                sendMissions();
            }
        });

        return Service.START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        PendingIntent service = PendingIntent.getService(
                getApplicationContext(),
                1001,
                new Intent(getApplicationContext(), KanotguidenService.class),
                PendingIntent.FLAG_ONE_SHOT);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 5000, service);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void sendMissions() {
        if (!MiscUtils.isAppInBackground(getApplicationContext())) {
            sendBroadcast(new Intent(MissionService.ACTION_MISSION_SEND_CACHED));
        }
    }

    private void startMission(Mission mission) {
        if (mission == null) {
            missionService.setCurrentMission(mission);
            return;
        }

        // don`t show current mission
        Mission currentMission = missionService.getCurrentMission();
        if (currentMission != null && mission.getMission_code().equals(currentMission.getMission_code()))
            return;

        mission.setDate(System.currentTimeMillis());
        missionService.setCurrentMission(mission);

        if (MiscUtils.isAppInBackground(getApplicationContext())) {
            // show notification
            Intent openAppIntent = new Intent(this, SplashActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new NotificationCompat.Builder(getApplicationContext())
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(mission.getTitle() + ":" + mission.getMission_code())
                    .setPriority(Notification.PRIORITY_MAX)
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                    .build();
            notificationManager.notify(1, notification);
            missionService.addMissionToSkipped(mission);
        } else {
            // show popup
            Intent missionIntent = new Intent(MissionService.ACTION_MISSION_FOUND);
            missionIntent.putExtra(MissionService.EXTRA_MISSION, mission);
            sendBroadcast(missionIntent);
        }
    }
}
