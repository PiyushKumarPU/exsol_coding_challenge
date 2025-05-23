package com.exasol.integration.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@JsonPropertyOrder({"Organization", "Repository", "Latest_Release_Date", "Latest_Release_Version"})
@Builder(toBuilder = true)
public class RepositoryInfo {

    @JsonProperty("Organization")
    private String organization;

    @JsonProperty("Repository")
    private String repository;

    private String latestReleaseDate;
    private String latestReleaseVersion;

    @JsonProperty("Latest_Release_Date")
    public String getLatestReleaseDate() {
        return latestReleaseDate != null ? latestReleaseDate : "";
    }

    @JsonProperty("Latest_Release_Version")
    public String getLatestReleaseVersion() {
        return latestReleaseVersion != null ? latestReleaseVersion : "";
    }
}
