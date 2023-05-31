package org.jembi.ciol.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InputReportData(
        @JsonProperty("metadataVersion") String metadataVersion,
        @JsonProperty("period") String period,
        @JsonProperty("orgUnitId") String orgUnitId,
        @JsonProperty("reportName") String reportName,
        @JsonProperty("dataElements") DataElements[] dataElements){

        public record DataElements(
                @JsonProperty("dataElementName") String dataElementName,
                @JsonProperty("values") Values[] values){

                public record Values(
                        @JsonProperty("disaggregations") Disaggregations[] disaggregations,
                        @JsonProperty("value") Integer value){

                        public record Disaggregations(
                                @JsonProperty("key") String key,
                                @JsonProperty("index") Integer index
                        ){}
                }
        }
}
