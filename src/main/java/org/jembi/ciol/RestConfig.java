package org.jembi.ciol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RestConfig(@JsonProperty("AppID") String appID,
                         @JsonProperty("DHIS2_IP") String dhis2IP,
                         @JsonProperty("DHIS2_port") Long dhis2Port,
                         @JsonProperty("DHIS2_Username") String dhis2Username,
                         @JsonProperty("DHIS2_Password") String dhis2Password) {
}