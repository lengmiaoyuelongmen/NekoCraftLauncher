package com.example.demo;

public class ModpackProject {
    private String id;
    private String title;
    private String description;
    private String author;
    private String iconUrl;
    private long downloads;
    private String updated;
    private String source;
    private String projectType;

    public ModpackProject(String id, String title, String description, String author,
                          String iconUrl, long downloads, String updated, String source, String projectType) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.iconUrl = iconUrl;
        this.downloads = downloads;
        this.updated = updated;
        this.source = source;
        this.projectType = projectType;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public String getIconUrl() { return iconUrl; }
    public long getDownloads() { return downloads; }
    public String getUpdated() { return updated; }
    public String getSource() { return source; }
    public String getProjectType() { return projectType; }

    public String getFormattedDownloads() {
        if (downloads >= 100000000) {
            return String.format("%.2f 亿", downloads / 100000000.0);
        } else if (downloads >= 10000) {
            return String.format("%.2f 万", downloads / 10000.0);
        } else {
            return String.valueOf(downloads);
        }
    }
}