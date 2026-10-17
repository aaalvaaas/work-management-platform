package ru.itmo.wmp.platform.infrastructure.nextcloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcsGroupsResponse(Ocs ocs) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ocs(OcsResponse.Meta meta, Data data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(List<String> groups) {
    }
}
