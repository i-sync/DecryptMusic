package com.example.decryptmusic.Models;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Story {
    private static String qreg = "第?(\\d+)期[ -_：:]?";
    private static String jreq = "第?(\\d+)集[ -_：:]?";
    private static String sreq = "第?(\\d+)[ \\.]?";
    private static final Pattern qPattern = Pattern.compile(qreg);
    private static final Pattern jPattern = Pattern.compile(jreq);
    private static final Pattern sPattern = Pattern.compile(sreq);
    private String download_url;
    private String url;
    private String mayday;
    private int type; //trial = 1, plan = 2, pay = 3
    private String name;
    private int order_in_album;
    private String album_name;

    public Story(){}
    public Story(String download_url, String url, String mayday, int type, String name, int order_in_album)
    {
        this.download_url = download_url;
        this.url = url;
        this.mayday = mayday;
        this.type = type;
        this.name = name;
        this.order_in_album = order_in_album;
    }

    public String getDownload_url() {
        return download_url;
    }

    public void setDownload_url(String download_url) {
        this.download_url = download_url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMayday() {
        return mayday;
    }

    public void setMayday(String mayday) {
        this.mayday = mayday;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getName() {
        /*if(true) {
            return String.format("%02d.%s.mp3", this.order_in_album -1, this.name.replaceFirst("^\\d+.", "").trim());
        }*/
        if (this.type == 2) //plan
        {
            return String.format("%02d.%s.mp3", this.order_in_album , this.name.replaceFirst("^\\d*", "").trim());
        }
        String index = "00";
        String tName = this.name;

        Matcher matcher = qPattern.matcher(tName);
        if(matcher.find())
        {
            index = matcher.group(1);
            tName = tName.replaceFirst(qreg, "").trim();
            return String.format("%02d.%s.mp3", Integer.parseInt(index), tName);
        }

        matcher = jPattern.matcher(tName);
        if(matcher.find())
        {
            index = matcher.group(1);
            tName = tName.replaceFirst(jreq, "").trim();
            return String.format("%02d.%s.mp3", Integer.parseInt(index), tName);
        }

        matcher = sPattern.matcher(tName);
        if(matcher.find())
        {
            index = matcher.group(1);
            tName = tName.replaceFirst(sreq, "").trim();
            return String.format("%02d.%s.mp3", Integer.parseInt(index), tName);
        }
        return tName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getOrder_in_album() {
        return order_in_album;
    }

    public void setOrder_in_album(int order_in_album) {
        this.order_in_album = order_in_album;
    }

    public String getAlbum_name() {
        return album_name;
    }

    public void setAlbum_name(String album_name) {
        this.album_name = album_name;
    }

    @NonNull
    @Override
    public String toString() {
        return "type:" + this.type + "name:" + this.getName();
    }
}
