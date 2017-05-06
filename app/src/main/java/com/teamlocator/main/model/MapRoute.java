package com.teamlocator.main.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Created by kiril on 28.03.2017.
 */

public class MapRoute {
    private long Id;
    private long width;
    private String color;
    private String name;
    private String link;
    private List<LatLng> route;

    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }

    public long getWidth() {
        return width;
    }

    public void setWidth(long width) {
        this.width = width;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = "#" + color;
    }

    public List<LatLng> getRoute() {
        return route;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setRoute(List<LatLng> route) {
        this.route = route;
    }
}
