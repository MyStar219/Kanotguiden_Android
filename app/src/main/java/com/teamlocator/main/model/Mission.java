package com.teamlocator.main.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

import java.io.Serializable;

/**
 * Created by kiril on 28.03.2017.
 */

@Root(name = "mission", strict = false)
public class Mission implements Serializable {
    @Element(name = "URL_Link")
    private String URL_Link;

    @Element(name = "title")
    private String title;

    @Element(name = "description")
    private String description;

    @Element(name = "mission_code")
    private String mission_code;

    @Element(name = "lat")
    private String lat;

    @Element(name = "lon")
    private String lon;

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    private long date;

    public String getURL_Link() {
        return URL_Link;
    }

    public void setURL_Link(String URL_Link) {
        this.URL_Link = URL_Link;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMission_code() {
        return mission_code;
    }

    public void setMission_code(String mission_code) {
        this.mission_code = mission_code;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "ClassPojo [URL_Link = " + URL_Link + ", title = " + title + ", lon = " + lon + ", description = " + description + ", mission_code = " + mission_code + ", lat = " + lat + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return ((Mission) obj).getMission_code().equals(this.getMission_code());
    }
}
