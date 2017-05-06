package com.teamlocator.main.service;

import android.content.Context;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.teamlocator.main.model.MapLocation;
import com.teamlocator.main.model.MapRoute;

import org.apache.commons.lang3.math.NumberUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class FusionService extends AbstractService {

    public static final String BASE_URL = "https://www.googleapis.com/fusiontables/v2/";
    private static final String TABLE_LOCATION_ID = "1f_drsbDdOJF36VtQOzd--xLvbtNaFqrsaJHL8ubt";
    private static final String TABLE_ROUTE_ID = "1fduGMK-nzU4xchPz23ul7_m6-yAIIp6yDM2w2ouR";

    private static final String QUERY_KEY = "AIzaSyAjoA2zJsLweZqrCi2_JMZhqKygEFQtmIM";
    private static final String QUERY_LOCATION = "SELECT * FROM " + TABLE_LOCATION_ID;
    private static final String QUERY_ROUTE = "SELECT * FROM " + TABLE_ROUTE_ID;

    private static final String ROUTES = "routes";
    private static final String MARKERS = "markers";

    public FusionService(Context context) {
        super(context);
    }

    public Observable<List<MapLocation>> getLocationsFromServer() {
        return createFusionRestService().query(QUERY_LOCATION, QUERY_KEY)
                .subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .map(fusionResponse -> {
                    List<MapLocation> locations = new ArrayList<>();

                    for (List<Object> row : fusionResponse.getRows()) {
                        // skip if contains wrong data
                        if (!NumberUtils.isNumber((String) row.get(0)))
                            continue;

                        String[] location = ((String) row.get(2)).split(" ");
                        if (location.length != 2)
                            continue;
                        if (!NumberUtils.isNumber(location[0]) || !NumberUtils.isNumber(location[1]))
                            continue;

                        MapLocation mapLocation = new MapLocation();
                        mapLocation.setId(Long.parseLong((String) row.get(0)));
                        mapLocation.setDescription((String) row.get(1));
                        mapLocation.setLatLng(new LatLng(NumberUtils.createDouble(location[0]), NumberUtils.createDouble(location[1])));
                        mapLocation.setName((String) row.get(3));
                        mapLocation.setLink((String) row.get(4));
                        locations.add(mapLocation);
                    }
                    return locations;
                })
                .doOnNext(mapLocations -> setMarkersToCache(mapLocations));
    }

    public Observable<List<MapRoute>> getRoutesFromServer() {
        return createFusionRestService().query(QUERY_ROUTE, QUERY_KEY)
                .subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .map(fusionResponse -> {
                    List<MapRoute> routes = new ArrayList<>();

                    for (List<Object> row : fusionResponse.getRows()) {
                        // skip if contains wrong data
                        if (!NumberUtils.isNumber((String) row.get(0)) || !NumberUtils.isNumber((String) row.get(1)))
                            continue;
                        if (!(row.get(3) instanceof LinkedTreeMap))
                            continue;

                        MapRoute mapRoute = new MapRoute();
                        mapRoute.setId(Long.parseLong((String) row.get(0)));
                        mapRoute.setWidth(Long.parseLong((String) row.get(1)));
                        mapRoute.setColor((String) row.get(2));
                        mapRoute.setName((String) row.get(4));
                        mapRoute.setLink((String) row.get(5));

                        List<LatLng> route = new ArrayList<>();
                        List<List<Double>> coordinates = (List<List<Double>>) ((LinkedTreeMap) ((LinkedTreeMap) row.get(3)).get("geometry")).get("coordinates");
                        for (List<Double> point : coordinates) {
                            route.add(new LatLng(point.get(1), point.get(0)));
                        }
                        mapRoute.setRoute(route);

                        routes.add(mapRoute);
                    }
                    return routes;
                })
                .doOnNext(mapRoutes -> setRoutesToCache(mapRoutes));
    }

    public List<MapRoute> getRoutesFromCache() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<MapRoute>>() {
        }.getType();
        String gsonString = PreferenceManager.getDefaultSharedPreferences(context).getString(ROUTES, "");
        return gsonString.isEmpty() ? new ArrayList<>() : gson.fromJson(gsonString, listType);
    }

    public void setRoutesToCache(List<MapRoute> routes) {
        Gson gson = new Gson();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(ROUTES, gson.toJson(routes)).apply();
    }

    public List<MapLocation> getMarkersFromCache() {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<MapLocation>>() {
        }.getType();
        String gsonString = PreferenceManager.getDefaultSharedPreferences(context).getString(MARKERS, "");
        return gsonString.isEmpty() ? new ArrayList<>() : gson.fromJson(gsonString, listType);
    }

    public void setMarkersToCache(List<MapLocation> locations) {
        Gson gson = new Gson();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(MARKERS, gson.toJson(locations)).apply();
    }

    public MapLocation getMarkerByIdFromCache(long id) {
        for (MapLocation mapLocation : getMarkersFromCache())
            if (mapLocation.getId() == id)
                return mapLocation;

        return null;
    }
}
