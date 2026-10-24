package com.example.demo;

public class Modpack {
    private String iconText;
    private String iconColor;
    private String title;
    private String desc;
    private String tags;
    private String downloads;
    private String updated;
    private String source;

    public Modpack(String iconText, String iconColor, String title, String desc, String tags, String downloads, String updated, String source) {
        this.iconText = iconText;
        this.iconColor = iconColor;
        this.title = title;
        this.desc = desc;
        this.tags = tags;
        this.downloads = downloads;
        this.updated = updated;
        this.source = source;
    }

    public String getIconText() { return iconText; }
    public String getIconColor() { return iconColor; }
    public String getTitle() { return title; }
    public String getDesc() { return desc; }
    public String getTags() { return tags; }
    public String getDownloads() { return downloads; }
    public String getUpdated() { return updated; }
    public String getSource() { return source; }
}