package com.example.harrower_craig_s2366969.model;

//
// Name                 Craig Harrower
// Student ID           S2366969
// Programme of Study   BSc Software Development
//

// one situation from the feed (an incident or a roadwork) - like the Thing class in lab 3
public class Situation {
    private String recordType;      // type: RoadOrCarriagewayOrLaneManagement
    private String subType;         // type: roadClosed, queuingTraffic, roadworks
    private String comment;         // generalPublicComment text
    private String startTime;       // overallStartTime
    private String endTime;         // overallEndTime (roadworks only)
    private int operationalLanes;   // numberOfOperationalLanes
    private int lanesRestricted;    // numberOfLanesRestricted
    private String delaysType;       // longDelays / delays (roadworks)
    private double fromLat;
    private double fromLon;
    private double toLat;
    private double toLon;
    private String road = "";       // pulled out of the comment text, e.g. "M8"
    private String direction = "";  // pulled out of the comment text, e.g. "Westbound"

    public Situation(){

    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(int operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    public int getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(int lanesRestricted) {
        this.lanesRestricted = lanesRestricted;
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType;
    }

    public double getFromLat() {
        return fromLat;
    }

    public void setFromLat(double fromLat) {
        this.fromLat = fromLat;
    }

    public double getFromLon() {
        return fromLon;
    }

    public void setFromLon(double fromLon) {
        this.fromLon = fromLon;
    }

    public double getToLat() {
        return toLat;
    }

    public void setToLat(double toLat) {
        this.toLat = toLat;
    }

    public double getToLon() {
        return toLon;
    }

    public void setToLon(double toLon) {
        this.toLon = toLon;
    }

    public String getRoad() {
        return road;
    }

    public void setRoad(String road) {
        this.road = road;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @Override
    public String toString() {
        return "Situation{" +
                "recordType='" + recordType + '\'' +
                ", subType='" + subType + '\'' +
                ", comment='" + comment + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", operationalLanes=" + operationalLanes +
                ", lanesRestricted=" + lanesRestricted +
                ", delaysType='" + delaysType + '\'' +
                ", fromLat=" + fromLat +
                ", fromLon=" + fromLon +
                ", toLat=" + toLat +
                ", toLon=" + toLon +
                ", road='" + road + '\'' +
                ", direction='" + direction + '\'' +
                '}';
    }
}
