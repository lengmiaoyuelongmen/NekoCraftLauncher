package com.example.demo;

public class Library {
    private String name;
    private Downloads downloads;

    public String getName() { return name; }
    public Downloads getDownloads() { return downloads; }

    public String getPath() {
        if (downloads != null && downloads.getArtifact() != null && downloads.getArtifact().getPath() != null) {
            return downloads.getArtifact().getPath();
        }
        if (name != null) {
            String[] parts = name.split(":");
            if (parts.length == 3) {
                String group = parts[0].replace(".", "/");
                String artifact = parts[1];
                String version = parts[2];
                return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
            }
        }
        return null;
    }

    public String getUrl() {
        if (downloads != null && downloads.getArtifact() != null && downloads.getArtifact().getUrl() != null) {
            return downloads.getArtifact().getUrl();
        }
        String path = getPath();
        if (path != null) return "https://libraries.minecraft.net/" + path;
        return null;
    }

    public Classifiers getClassifiers() {
        if (downloads != null) return downloads.getClassifiers();
        return null;
    }

    public static class Downloads {
        private Artifact artifact;
        private Classifiers classifiers;
        public Artifact getArtifact() { return artifact; }
        public Classifiers getClassifiers() { return classifiers; }
    }

    public static class Artifact {
        private String path;
        private String url;
        public String getPath() { return path; }
        public String getUrl() { return url; }
    }

    public static class Classifiers {
        @com.google.gson.annotations.SerializedName("natives-windows")
        private Artifact nativesWindows;
        public Artifact getNativesWindows() { return nativesWindows; }
    }
}