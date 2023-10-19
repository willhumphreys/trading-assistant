package uk.co.threebugs.mochiwhattotrade3;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetaTraderDir {
    @JsonProperty("name")
    private String name;

    @JsonProperty("dir-path")
    private String dirPath;

    // Getters and setters
    public String getName() {
        return name;
    }

    public String getDirPath() {
        return dirPath;
    }
}
