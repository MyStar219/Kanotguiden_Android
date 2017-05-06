package com.teamlocator.main.model;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "missions", strict = false)
public class MissionList {
    @Attribute(name = "missionRange")
    private String missionRange;

    @Attribute(name = "externalLink")
    private String externalLink;

    @ElementList(name = "mission", inline = true)
    private List<Mission> mission;

    public String getMissionRange() {
        return missionRange;
    }

    public void setMissionRange(String missionRange) {
        this.missionRange = missionRange;
    }

    public List<Mission> getMission() {
        return mission;
    }

    public void setMission(List<Mission> mission) {
        this.mission = mission;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    @Override
    public String toString() {
        return "ClassPojo [missionRange = " + missionRange + ", mission = " + mission + ", externalLink = " + externalLink + "]";
    }
}
