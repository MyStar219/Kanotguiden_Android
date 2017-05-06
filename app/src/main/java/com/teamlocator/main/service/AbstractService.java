package com.teamlocator.main.service;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class AbstractService {
    private final OkHttpClient okHttpClient;
    Context context;

    public AbstractService(Context context) {

        this.context = context;
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    FusionApiService createFusionRestService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FusionService.BASE_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(FusionApiService.class);
    }

    MissionApiService createMissionFetchRestService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MissionService.BASE_FETCH_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MissionApiService.class);
    }

    MissionApiService createMissionSendRestService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MissionService.BASE_SUBMIT_URL)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .client(okHttpClient)
                .build();
        return retrofit.create(MissionApiService.class);
    }
}
