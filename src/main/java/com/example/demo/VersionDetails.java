package com.example.demo;

import java.util.List;

public class VersionDetails {
    private String id;
    private String mainClass;
    private List<Library> libraries;
    private Downloads downloads;
    private AssetIndexInfo assetIndex;

    public String getId() { return id; }
    public String getMainClass() { return mainClass; }
    public List<Library> getLibraries() { return libraries; }
    public Downloads getDownloads() { return downloads; }
    public AssetIndexInfo getAssetIndex() { return assetIndex; }

    public static class Downloads {
        private Client client;
        public Client getClient() { return client; }
    }
    public static class Client {
        private String url;
        public String getUrl() { return url; }
    }
    public static class AssetIndexInfo {
        private String id;
        private String url;
        public String getId() { return id; }
        public String getUrl() { return url; }
    }
}