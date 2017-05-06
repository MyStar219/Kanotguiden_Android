package com.teamlocator.main.service;

import com.teamlocator.main.model.FusionResponse;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by kiril on 28.03.2017.
 */

public interface FusionApiService {
    @GET("query")
    Observable<FusionResponse> query(@Query("sql") String sql, @Query("key") String key);
}
