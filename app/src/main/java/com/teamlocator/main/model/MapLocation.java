package com.teamlocator.main.model;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by kiril on 28.03.2017.
 */

public class MapLocation {
    private long id;
    private String name;
    private String description;
    private String link;
    private LatLng latLng;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
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
}
