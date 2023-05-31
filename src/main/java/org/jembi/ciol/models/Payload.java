package org.jembi.ciol.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Payload(
        @JsonProperty("dataset") String dataset,
        @JsonProperty("completeDate") String completeDate,
        @JsonProperty("period") String period,
        @JsonProperty("orgUnit") String orgUnit,
        @JsonProperty("attributeOptionCombo") String attributeOptionCombo,
        @JsonProperty("dataValues") List <DataValues> dataValues) {

    public record DataValues(
            @JsonProperty("dataElement") String dataElement,
            @JsonProperty("categoryOptionCombo") String categoryOptionCombo,
            @JsonProperty("value") String value){}

}
