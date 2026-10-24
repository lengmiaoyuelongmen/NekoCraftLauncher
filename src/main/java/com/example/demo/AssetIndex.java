package com.example.demo;

import java.util.Map;

public class AssetIndex {
    private Map<String, AssetObject> objects;
    public Map<String, AssetObject> getObjects() { return objects; }

    public static class AssetObject {
        private String hash;
        private long size;
        public String getHash() { return hash; }
        public long getSize() { return size; }
    }
}