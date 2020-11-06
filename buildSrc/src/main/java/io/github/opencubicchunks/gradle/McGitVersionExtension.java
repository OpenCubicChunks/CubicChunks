package io.github.opencubicchunks.gradle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class McGitVersionExtension {

    boolean configured;
    private Boolean snapshot;
    private String versionSuffix = "";
    private String forceVersionString;
    private String mcVersion;
    private final Map<String, String> commitVersions = new HashMap<>();

    public void setCommitVersion(String commit, String version) {
        this.commitVersions.put(commit, version);
    }

    public Map<String, String> getCommitVersions() {
        return Collections.unmodifiableMap(commitVersions);
    }

    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    public String getMcVersion() {
        return this.mcVersion;
    }

    public boolean isSnapshot() {
        return snapshot;
    }

    public void setSnapshot(boolean snapshot) {
        this.configured = true;
        this.snapshot = snapshot;
    }

    public String getVersionSuffix() {
        return versionSuffix;
    }

    public void setVersionSuffix(String versionSuffix) {
        this.configured = true;
        this.versionSuffix = versionSuffix;
    }

    public String getForceVersionString() {
        return forceVersionString;
    }

    public void setForceVersionString(String forceVersionString) {
        this.configured = true;
        this.forceVersionString = forceVersionString;
    }
}