package com.teamlocator.main.service;

import com.teamlocator.main.model.MissionList;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Created by kiril on 28.03.2017.
 */

public interface MissionApiService {

    /**
     * Get mission xml from sever
     * @return
     */
    @GET("missions.xml")
    Observable<MissionList> getMissions();

    /**
     * Send mission to the server
     * @param name
     * @param email
     * @param missionCode
     * @param date
     * @param lat
     * @param lng
     * @return
     */
    @FormUrlEncoded
    @POST("mission_submit.php")
    Observable<ResponseBody> sendMission(@Field("Name") String name,
                                         @Field("Email") String email,
                                         @Field("MissionCode") String missionCode,
                                         @Field("Date") long date,
                                         @Field("Lat") String lat,
                                         @Field("Lng") String lng);
}
