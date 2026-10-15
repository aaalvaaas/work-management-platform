package ru.itmo.wmp.platform.infrastructure.nextcloud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OcsResponse(Ocs ocs) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ocs(Meta meta) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(String status, int statuscode, String message) {
    }
}
