package com.ayyappasamaaj.tattvamasi.model;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class Event {
    private long date;
    private String desc;
    private long latitude;
    private long longitude;
    private String name;
    private String registrationLink;
    private String venue;
    // extra
    private String month;
    private String day;
    private String time;

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
        Log.d("Event", "Event date = "+date);
        // from date set month, day
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date * 1000);
        calendar.setTimeZone(TimeZone.getTimeZone("America/Santiago"));

        // set month
        int month = calendar.get(Calendar.MONTH);
        setMonth(theMonth(month).toUpperCase());

        // set day
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        setDay(String.valueOf(day));

        // set time
        String amPm = new SimpleDateFormat("hh:mm aa").format(calendar.getTimeInMillis());
        setTime(amPm);
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public long getLatitude() {
        return latitude;
    }

    public void setLatitude(long latitude) {
        this.latitude = latitude;
    }

    public long getLongitude() {
        return longitude;
    }

    public void setLongitude(long longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistrationLink() {
        return registrationLink;
    }

    public void setRegistrationLink(String registrationLink) {
        this.registrationLink = registrationLink;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public static String theMonth(int month){
        String[] monthNames = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        return monthNames[month];
    }
}
