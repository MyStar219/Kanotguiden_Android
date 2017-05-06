package com.teamlocator.main.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.teamlocator.main.model.Mission;
import com.teamlocator.main.model.MissionList;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class MissionService extends AbstractService {

    // server for fetching mission xml
    //    public static final String BASE_FETCH_URL = "http://www.canoego.se/missions/";
    public static final String BASE_FETCH_URL = "http://itinplanner.com/Kanotguiden/";
    // server for sending mission results to
    public static final String BASE_SUBMIT_URL = "http://itinplanner.com/Kanotguiden/";

    public static final String ACTION_MISSION_FOUND = "kanotguiden.mission.found";
    public static final String ACTION_MISSION_SEND_CACHED = "kanotguiden.mission.send";
    public static final String EXTRA_MISSION = "mission";

    private static final String PREF_CACHED_MISSIONS = "cached_missions";
    private static final String PREF_MISSIONS = "missions";
    private static final String PREF_CURRENT_MISSION = "current_mission";
    private static final String PREF_NAME = "name";
    private static final String PREF_EMAIL = "email";

    private final SharedPreferences preferences;

    public MissionService(Context context) {
        super(context);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Gets the missions from the server
     *
     * @return
     */
    public Observable<MissionList> getMissionsFromServer() {
        return createMissionFetchRestService().getMissions()
                .subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .doOnNext(missionList -> setCachedMissions(missionList));
    }

    public Observable<ResponseBody> sendMissionToServer(String name, String email, Mission mission) {
        return createMissionSendRestService().sendMission(
                name,
                email,
                mission.getMission_code(),
                mission.getDate(),
                mission.getLat(),
                mission.getLon())
                .subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Gets the local copy of the missions
     *
     * @return
     */
    public MissionList getCachedMissions() {
        Gson gson = new Gson();
        return gson.fromJson(preferences.getString(PREF_MISSIONS, ""), MissionList.class);
    }

    /**
     * Saves the missions to the local storage
     *
     * @param missionList
     */
    public void setCachedMissions(MissionList missionList) {
        Gson gson = new Gson();
        preferences.edit().putString(PREF_MISSIONS, gson.toJson(missionList)).apply();
    }

    public List<Mission> getSkippedMissions() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Mission>>() {
        }.getType();
        String gsonString = preferences.getString(PREF_CACHED_MISSIONS, "");
        return gsonString.isEmpty() ? new ArrayList<>() : gson.fromJson(gsonString, listType);
    }

    public void setSkippedMissions(List<Mission> missionList) {
        Gson gson = new Gson();
        preferences.edit().putString(PREF_CACHED_MISSIONS, gson.toJson(missionList)).apply();
    }

    public String getUserName() {
        return preferences.getString(PREF_NAME, "");
    }

    public void setUserName(String name) {
        preferences.edit().putString(PREF_NAME, name).apply();
    }

    public String getUserEmail() {
        return preferences.getString(PREF_EMAIL, "");
    }

    public void setUserEmail(String email) {
        preferences.edit().putString(PREF_EMAIL, email).apply();
    }

    public void addMissionToSkipped(Mission mission) {
        List<Mission> list = getSkippedMissions();

        if (list.contains(mission))
            return;

        list.add(mission);
        setSkippedMissions(list);
    }

    public void removeMissionFromSkipped(Mission mission) {
        List<Mission> list = getSkippedMissions();
        list.remove(mission);
        setSkippedMissions(list);
    }

    public Mission getCurrentMission() {
        Gson gson = new Gson();
        return gson.fromJson(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_CURRENT_MISSION, ""), Mission.class);
    }

    public void setCurrentMission(Mission mission) {
        Gson gson = new Gson();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_CURRENT_MISSION, gson.toJson(mission)).apply();
    }

    /**
     * Checks if there is any mission nearby
     *
     * @param context app context
     * @return mission or null
     */
    public Mission checkIfNearMission(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location currentLocation = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (currentLocation == null || l.getAccuracy() < currentLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                currentLocation = l;
            }
        }
        if (currentLocation != null) {
            for (Mission mission : getCachedMissions().getMission()) {
                Location missionLocation = new Location("mission");
                missionLocation.setLatitude(Double.parseDouble(mission.getLat()));
                missionLocation.setLongitude(Double.parseDouble(mission.getLon()));

                if ((currentLocation.distanceTo(missionLocation) / 1000) <= Float.parseFloat(getCachedMissions().getMissionRange())) {
//                if ((currentLocation.distanceTo(missionLocation) / 1000) <= 10000000) {
                    return mission;
                }
            }
        }

//        setCurrentMission(null);
        return null;
    }
}
